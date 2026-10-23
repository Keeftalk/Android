package tn.keeftalk.chat.extension.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.extension.data.RemoteReview
import tn.keeftalk.chat.extension.data.RemoteStoreExtension
import tn.keeftalk.chat.extension.data.StoreRepository
import tn.keeftalk.chat.extension.lifecycle.ExtensionHost
import tn.keeftalk.chat.extension.model.ExtensionManifest
import tn.keeftalk.chat.extension.model.InstalledExtension
import tn.keeftalk.chat.extension.model.SecurityReviewReport
import tn.keeftalk.chat.extension.parser.ExtractedPackageSession

sealed class ReviewState {
    data object Idle : ReviewState()
    data object Analyzing : ReviewState()
    data class Ready(
        val session: ExtractedPackageSession,
        val manifest: ExtensionManifest,
        val report: SecurityReviewReport
    ) : ReviewState()
    data class Error(val message: String) : ReviewState()
}

sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data class Downloading(val progressPct: Int) : DownloadStatus()
    data object Verifying : DownloadStatus()
    data object Installing : DownloadStatus()
}

class ExtensionViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ExtensionViewModel"
    private val host: ExtensionHost = AppModule.provideExtensionHost(application)
    private val repository = StoreRepository(application)

    val installedExtensions: StateFlow<List<InstalledExtension>> = host.getAllInstalledExtensionsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _catalog = MutableStateFlow<List<RemoteStoreExtension>>(emptyList())
    val catalog: StateFlow<List<RemoteStoreExtension>> = _catalog.asStateFlow()

    private val _selectedDetailExtension = MutableStateFlow<RemoteStoreExtension?>(null)
    val selectedDetailExtension: StateFlow<RemoteStoreExtension?> = _selectedDetailExtension.asStateFlow()

    private val _selectedPublisherId = MutableStateFlow<String?>(null)
    val selectedPublisherId: StateFlow<String?> = _selectedPublisherId.asStateFlow()

    private val _selectedReviews = MutableStateFlow<List<RemoteReview>>(emptyList())
    val selectedReviews: StateFlow<List<RemoteReview>> = _selectedReviews.asStateFlow()

    private val _rankingMode = MutableStateFlow("global") // "global" or "country"
    val rankingMode: StateFlow<String> = _rankingMode.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _reviewState = MutableStateFlow<ReviewState>(ReviewState.Idle)
    val reviewState: StateFlow<ReviewState> = _reviewState.asStateFlow()

    private val _downloadStatuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatuses: StateFlow<Map<String, DownloadStatus>> = _downloadStatuses.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            val mode = _rankingMode.value
            val rankResult = repository.fetchRankings(mode = mode, countryCode = "TN")
            rankResult.onSuccess { list ->
                if (list.isNotEmpty()) {
                    _catalog.value = list
                } else {
                    fetchDefaultCatalog()
                }
            }.onFailure {
                fetchDefaultCatalog()
            }
        }
    }

    private suspend fun fetchDefaultCatalog() {
        val catResult = repository.fetchCatalog(category = _selectedCategory.value, query = _searchQuery.value)
        catResult.onSuccess { list ->
            _catalog.value = list
        }.onFailure {
            _catalog.value = emptyList()
        }
    }

    fun setRankingMode(mode: String) {
        _rankingMode.value = mode
        loadCatalog()
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
        loadCatalog()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadCatalog()
    }

    fun openDetails(ext: RemoteStoreExtension) {
        _selectedDetailExtension.value = ext
        loadReviewsForExtension(ext.id)
    }

    fun closeDetails() {
        _selectedDetailExtension.value = null
        _selectedReviews.value = emptyList()
    }

    fun openPublisher(publisherId: String) {
        _selectedPublisherId.value = publisherId
    }

    fun closePublisher() {
        _selectedPublisherId.value = null
    }

    fun loadReviewsForExtension(extensionId: String) {
        viewModelScope.launch {
            val res = repository.fetchReviews(extensionId)
            res.onSuccess { list ->
                _selectedReviews.value = list
            }.onFailure {
                _selectedReviews.value = emptyList()
            }
        }
    }

    fun downloadAndInstallRemote(extId: String) {
        viewModelScope.launch {
            _downloadStatuses.value = _downloadStatuses.value + (extId to DownloadStatus.Downloading(15))
            _userMessage.value = "Downloading package..."
            val dlResult = repository.downloadPackageToCache(extId)
            dlResult.onSuccess { pkgFile ->
                _downloadStatuses.value = _downloadStatuses.value + (extId to DownloadStatus.Downloading(65))
                val uri = Uri.fromFile(pkgFile)
                _downloadStatuses.value = _downloadStatuses.value + (extId to DownloadStatus.Verifying)
                try {
                    val (session, manifest, report) = host.stageAndReviewPackage(uri)
                    if (manifest == null || !report.isEligibleForInstallation) {
                        _userMessage.value = "Package review failed: ${report.formatDetails}"
                        _downloadStatuses.value = _downloadStatuses.value - extId
                    } else {
                        _downloadStatuses.value = _downloadStatuses.value + (extId to DownloadStatus.Installing)
                        val installResult = host.confirmAndInstallStagedPackage(session, manifest, report)
                        installResult.onSuccess { ext ->
                            repository.recordInstallationEvent(ext.id, ext.version)
                            _userMessage.value = "Successfully installed '${ext.name}'!"
                            _downloadStatuses.value = _downloadStatuses.value - extId
                        }.onFailure { err ->
                            _userMessage.value = "Installation failed: ${err.message}"
                            _downloadStatuses.value = _downloadStatuses.value - extId
                        }
                    }
                } catch (e: Exception) {
                    _userMessage.value = "Error: ${e.message}"
                    _downloadStatuses.value = _downloadStatuses.value - extId
                }
            }.onFailure { err ->
                _userMessage.value = "Download failed: ${err.message}"
                _downloadStatuses.value = _downloadStatuses.value - extId
            }
        }
    }

    fun stageAndReviewPackage(packageUri: Uri) {
        viewModelScope.launch {
            _reviewState.value = ReviewState.Analyzing
            try {
                val (session, manifest, report) = host.stageAndReviewPackage(packageUri)
                if (manifest == null) {
                    _reviewState.value = ReviewState.Error(report.formatDetails.ifBlank { "Invalid or corrupt extension manifest." })
                } else {
                    _reviewState.value = ReviewState.Ready(session, manifest, report)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reviewing extension package: ${e.message}", e)
                _reviewState.value = ReviewState.Error(e.message ?: "Failed to stage and analyze extension package.")
            }
        }
    }

    fun confirmInstallation(onSuccess: () -> Unit) {
        val currentState = _reviewState.value
        if (currentState !is ReviewState.Ready) return

        viewModelScope.launch {
            val result = host.confirmAndInstallStagedPackage(
                session = currentState.session,
                manifest = currentState.manifest,
                report = currentState.report
            )

            result.onSuccess { ext ->
                repository.recordInstallationEvent(ext.id, ext.version)
                _userMessage.value = "Successfully installed '${ext.name}'!"
                _reviewState.value = ReviewState.Idle
                onSuccess()
            }.onFailure { err ->
                _userMessage.value = "Installation failed: ${err.message}"
            }
        }
    }

    fun submitUserReview(extensionId: String, rating: Int, title: String, reviewText: String) {
        viewModelScope.launch {
            val res = repository.submitReview(extensionId, rating, title, reviewText)
            res.onSuccess {
                _userMessage.value = "Review submitted successfully!"
                loadReviewsForExtension(extensionId)
                loadCatalog()
            }.onFailure { err ->
                _userMessage.value = "Failed to submit review: ${err.message}"
            }
        }
    }

    fun cancelReview() {
        val currentState = _reviewState.value
        if (currentState is ReviewState.Ready) {
            currentState.session.stagingDir.deleteRecursively()
        }
        _reviewState.value = ReviewState.Idle
    }

    fun toggleExtensionEnabled(extensionId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            host.toggleExtensionEnabled(extensionId, isEnabled)
            _userMessage.value = if (isEnabled) "Extension enabled." else "Extension disabled."
        }
    }

    fun uninstallExtension(extensionId: String) {
        viewModelScope.launch {
            host.uninstallExtension(extensionId)
            _userMessage.value = "Extension uninstalled."
            if (_selectedDetailExtension.value?.id == extensionId) {
                closeDetails()
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
