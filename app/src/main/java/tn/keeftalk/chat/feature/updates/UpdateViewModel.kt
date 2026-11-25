package tn.keeftalk.chat.feature.updates

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

enum class ReleaseChannel {
    STABLE, PRERELEASE
}

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class UpToDate(val installedVersion: String, val channel: ReleaseChannel) : UpdateUiState()
    data class UpdateAvailable(
        val installedVersion: String,
        val availableVersion: String,
        val releaseDate: String?,
        val changelog: String?,
        val downloadSize: Long,
        val assetName: String,
        val channel: ReleaseChannel
    ) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

sealed class DownloadUiState {
    object Idle : DownloadUiState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadUiState()
    data class Completed(val apkFile: File) : DownloadUiState()
    object Cancelled : DownloadUiState()
    data class Error(val message: String) : DownloadUiState()
}

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val updateRepository = UpdateRepository(httpClient)

    private val _channel = MutableStateFlow(ReleaseChannel.STABLE)
    val channel: StateFlow<ReleaseChannel> = _channel.asStateFlow()

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val downloadState: StateFlow<DownloadUiState> = _downloadState.asStateFlow()

    private var selectedAsset: GitHubAsset? = null
    private var downloadJob: Job? = null

    val installedVersion: String
        get() = try {
            val pInfo: PackageInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }

    fun setChannel(newChannel: ReleaseChannel) {
        _channel.value = newChannel
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.Checking
            val result = updateRepository.fetchReleases()
            result.onSuccess { releases ->
                val channelReleases = when (_channel.value) {
                    ReleaseChannel.STABLE -> releases.filter { !it.prerelease && !it.draft }
                    ReleaseChannel.PRERELEASE -> releases.filter { !it.draft }
                }

                val latestRelease = channelReleases.maxByOrNull { it.tagName } ?: releases.firstOrNull()
                if (latestRelease == null) {
                    _uiState.value = UpdateUiState.Error("No releases found on GitHub.")
                    return@onSuccess
                }

                val isNewer = UpdateUtils.isNewerVersion(installedVersion, latestRelease.tagName)
                val bestAsset = UpdateUtils.selectBestAsset(latestRelease.assets)
                selectedAsset = bestAsset

                if (isNewer && bestAsset != null) {
                    _uiState.value = UpdateUiState.UpdateAvailable(
                        installedVersion = installedVersion,
                        availableVersion = latestRelease.tagName,
                        releaseDate = latestRelease.publishedAt,
                        changelog = latestRelease.body,
                        downloadSize = bestAsset.size,
                        assetName = bestAsset.name,
                        channel = _channel.value
                    )
                } else {
                    _uiState.value = UpdateUiState.UpToDate(installedVersion, _channel.value)
                }
            }.onFailure { e ->
                _uiState.value = UpdateUiState.Error(e.localizedMessage ?: "Failed to check for updates.")
            }
        }
    }

    fun downloadUpdate() {
        val asset = selectedAsset ?: return
        downloadJob?.cancel()

        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _downloadState.value = DownloadUiState.Downloading(0f, 0L, asset.size)
                val client = OkHttpClient()
                val request = Request.Builder().url(asset.browserDownloadUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    _downloadState.value = DownloadUiState.Error("Download failed: ${response.code}")
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    _downloadState.value = DownloadUiState.Error("Empty response body.")
                    return@launch
                }

                val context = getApplication<Application>()
                val apkFile = File(context.cacheDir, "updates/${asset.name}")
                apkFile.parentFile?.mkdirs()

                val totalBytes = body.contentLength().takeIf { it > 0 } ?: asset.size.takeIf { it > 0 } ?: 1L
                var downloadedBytes = 0L

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (!coroutineContext[Job]!!.isActive) {
                                _downloadState.value = DownloadUiState.Cancelled
                                return@launch
                            }
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                            _downloadState.value = DownloadUiState.Downloading(progress.coerceIn(0f, 1f), downloadedBytes, totalBytes)
                        }
                    }
                }

                _downloadState.value = DownloadUiState.Completed(apkFile)
            } catch (e: CancellationException) {
                _downloadState.value = DownloadUiState.Cancelled
            } catch (e: Exception) {
                _downloadState.value = DownloadUiState.Error(e.localizedMessage ?: "Download failed.")
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _downloadState.value = DownloadUiState.Cancelled
    }

    fun installUpdate(context: Context, apkFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _downloadState.value = DownloadUiState.Error("Failed to start installation: ${e.localizedMessage}")
        }
    }
}
