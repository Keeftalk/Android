package com.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.data.remote.UserChatSettingsDto
import com.keeftalk.chat.domain.model.UserChatSettings
import com.keeftalk.chat.domain.repository.ChatSettingsRepository
import com.keeftalk.chat.di.AppModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.time.Duration.Companion.seconds

class ChatSettingsRepositoryImpl(
    private val context: Context,
    private val prefs: UserPreferencesRepository
) : ChatSettingsRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _chatSettings = MutableStateFlow(UserChatSettings(userId = ""))
    override val chatSettings: Flow<UserChatSettings> = _chatSettings.asStateFlow()
    private var settingsChannel: RealtimeChannel? = null
    private val fetchMutex = Mutex()

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    init {
        repositoryScope.launch {
            // LOAD FROM CACHE FIRST
            val cachedPrefs = prefs.userPreferencesFlow.first()
            val cachedTheme = cachedPrefs.themeMode
            val cachedFontSize = cachedPrefs.fontSize
            _chatSettings.value = _chatSettings.value.copy(
                theme = cachedTheme,
                fontSize = cachedFontSize.toInt()
            )
        }
    }

    override fun startSettingsObservation() {
        com.keeftalk.chat.util.PerformanceProfiler.logEvent("ChatSettingsRepository.startSettingsObservation() called")
        repositoryScope.launch {
            // Defer network work significantly
            delay(2.seconds)
            val supabase = getSupabase()
            supabase.auth.sessionStatus
                .map { supabase.auth.currentUserOrNull()?.id }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId != null) {
                        fetchAndObserveSettings(userId)
                    }
                }
        }
    }

    private suspend fun fetchAndObserveSettings(userId: String) = fetchMutex.withLock {
        val supabase = getSupabase()
        // Initial fetch from Supabase
        try {
            val remote = supabase.postgrest["user_chat_settings"]
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<UserChatSettingsDto>()
            
            if (remote != null) {
                val domain = remote.toDomain()
                _chatSettings.value = domain
                // UPDATE CACHE
                prefs.updateThemeMode(domain.theme)
                prefs.updateChatSettings(fontSize = domain.fontSize.toFloat())
            } else {
                // Create default settings if not exists
                // Use upsert to handle race conditions gracefully
                val defaultSettings = UserChatSettings(userId = userId)
                supabase.postgrest["user_chat_settings"].upsert(UserChatSettingsDto.fromDomain(defaultSettings))
                _chatSettings.value = defaultSettings
            }
        } catch (e: Exception) {
            Log.e("ChatSettingsRepo", "Failed to fetch chat settings", e)
        }

        // Observe changes
        try {
            settingsChannel?.let {
                try { it.unsubscribe() } catch (_: Exception) {}
                supabase.realtime.removeChannel(it)
            }
            
            val channel = supabase.realtime.channel("chat_settings_sync")
            settingsChannel = channel

            channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "user_chat_settings"
            }.onEach { action ->
                val dto = action.decodeRecord<UserChatSettingsDto>()
                if (dto.userId == userId) {
                    _chatSettings.value = dto.toDomain()
                }
            }.launchIn(repositoryScope)
            
            repositoryScope.launch {
                channel.subscribe()
            }
        } catch (e: Exception) {
            Log.e("ChatSettingsRepo", "Realtime setup error", e)
        }
    }

    override suspend fun updateSettings(settings: UserChatSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dto = UserChatSettingsDto.fromDomain(settings)
            getSupabase().postgrest["user_chat_settings"].upsert(dto)
            _chatSettings.value = settings
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSetting(key: String, value: Any): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = _chatSettings.value.userId
            if (userId.isEmpty()) return@withContext Result.failure(Exception("User not logged in"))
            
            getSupabase().postgrest["user_chat_settings"].update({
                set(key, value)
            }) {
                filter { eq("user_id", userId) }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetToDefault(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = _chatSettings.value.userId
        if (userId.isEmpty()) return@withContext Result.failure(Exception("User not logged in"))
        updateSettings(UserChatSettings(userId = userId))
    }

    override suspend fun getCacheSize(): Map<String, Long> = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val images = getFolderSize(File(cacheDir, "images"))
        val videos = getFolderSize(File(cacheDir, "videos"))
        val files = getFolderSize(File(cacheDir, "files"))
        val others = getFolderSize(cacheDir) - (images + videos + files)
        
        mapOf(
            "images" to images,
            "videos" to videos,
            "files" to files,
            "others" to others,
            "total" to getFolderSize(cacheDir)
        )
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { size += getFolderSize(it) }
        return size
    }

    override suspend fun clearCache(types: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            types.forEach { type ->
                when (type) {
                    "images" -> deleteFolder(File(cacheDir, "images"))
                    "videos" -> deleteFolder(File(cacheDir, "videos"))
                    "files" -> deleteFolder(File(cacheDir, "files"))
                    "all" -> deleteFolder(cacheDir)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deleteFolder(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteFolder(it) }
        }
        file.delete()
    }

    override suspend fun clearSearchHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun clearRecentEmojis(): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override fun shutdown() {
        Log.i("ChatSettingsRepo", "Shutting down ChatSettingsRepository")
        repositoryScope.cancel()
    }
}
