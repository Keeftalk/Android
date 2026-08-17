package com.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.data.remote.*
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.domain.repository.SettingsRepository
import com.keeftalk.chat.di.AppModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

class SettingsRepositoryImpl(
    private val context: Context,
    private val prefs: UserPreferencesRepository
) : SettingsRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _settings = MutableStateFlow(UserSettings(userId = ""))
    override val settings: Flow<UserSettings> = _settings.asStateFlow()
    
    private var syncJob: Job? = null
    private var realtimeChannel: RealtimeChannel? = null
    private val syncMutex = Mutex()

    private suspend fun getSupabase(): SupabaseClient = AppModule.provideSupabaseClientAsync(context)

    init {
        repositoryScope.launch {
            prefs.fullSettingsFlow.collect { localSettings ->
                _settings.update { localSettings }
            }
        }
    }

    override fun startSettingsSync() {
        syncJob?.cancel()
        syncJob = repositoryScope.launch {
            val supabase = getSupabase()
            supabase.auth.sessionStatus
                .map { (it as? io.github.jan.supabase.auth.status.SessionStatus.Authenticated)?.session?.user?.id }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId != null) {
                        fetchAndObserve(userId)
                    } else {
                        stopSettingsSync()
                    }
                }
        }
    }

    private suspend fun fetchAndObserve(userId: String) = syncMutex.withLock {
        val supabase = getSupabase()
        try {
            val remote = supabase.postgrest["user_settings"]
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<UserSettingsDto>()
            
            if (remote != null) {
                val domain = remote.toDomain()
                prefs.updateFullSettings(domain)
            } else {
                // Initialize remote settings if they don't exist
                val initial = _settings.value.copy(userId = userId)
                supabase.postgrest["user_settings"].upsert(UserSettingsDto.fromDomain(initial))
            }
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to fetch settings from Supabase", e)
        }

        // Realtime
        realtimeChannel?.let { supabase.realtime.removeChannel(it) }
        val channel = supabase.realtime.channel("user_settings_sync_$userId")
        realtimeChannel = channel
        
        channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "user_settings"
        }.onEach { action ->
            val dto = action.decodeRecord<UserSettingsDto>()
            if (dto.userId == userId) {
                prefs.updateFullSettings(dto.toDomain())
            }
        }.launchIn(repositoryScope)
        
        channel.subscribe()
    }

    override suspend fun updateSettings(settings: UserSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.updateFullSettings(settings)
            val supabase = getSupabase()
            if (supabase.auth.currentUserOrNull() != null) {
                supabase.postgrest["user_settings"].upsert(UserSettingsDto.fromDomain(settings))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to update settings", e)
            Result.failure(e)
        }
    }

    private suspend fun updateSection(update: (UserSettings) -> UserSettings): Result<Unit> {
        val newSettings = update(_settings.value)
        return updateSettings(newSettings)
    }

    override suspend fun updateChatSettings(update: (UserChatSettings) -> UserChatSettings) = 
        updateSection { it.copy(chatSettings = update(it.chatSettings)) }

    override suspend fun updateCallSettings(update: (UserCallSettings) -> UserCallSettings) =
        updateSection { it.copy(callSettings = update(it.callSettings)) }

    override suspend fun updateNoteSettings(update: (UserNoteSettings) -> UserNoteSettings) =
        updateSection { it.copy(noteSettings = update(it.noteSettings)) }

    override suspend fun updateVaultSettings(update: (UserVaultSettings) -> UserVaultSettings) =
        updateSection { it.copy(vaultSettings = update(it.vaultSettings)) }

    override suspend fun updateCalendarSettings(update: (UserCalendarSettings) -> UserCalendarSettings) =
        updateSection { it.copy(calendarSettings = update(it.calendarSettings)) }

    override suspend fun updateEmailSettings(update: (UserEmailSettings) -> UserEmailSettings) =
        updateSection { it.copy(emailSettings = update(it.emailSettings)) }

    override suspend fun updateNotificationSettings(update: (UserNotificationSettings) -> UserNotificationSettings) =
        updateSection { it.copy(notificationSettings = update(it.notificationSettings)) }

    override suspend fun updatePrivacySettings(update: (UserPrivacySettings) -> UserPrivacySettings) =
        updateSection { it.copy(privacySettings = update(it.privacySettings)) }

    override suspend fun updateParentalControls(update: (UserParentalControls) -> UserParentalControls) =
        updateSection { it.copy(parentalControls = update(it.parentalControls)) }

    override suspend fun getCacheSize(): Map<String, Long> = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        fun getFolderSize(file: java.io.File): Long {
            if (!file.exists()) return 0
            if (file.isFile) return file.length()
            var size = 0L
            file.listFiles()?.forEach { size += getFolderSize(it) }
            return size
        }

        val images = getFolderSize(java.io.File(cacheDir, "media/image"))
        val videos = getFolderSize(java.io.File(cacheDir, "media/video"))
        val files = getFolderSize(java.io.File(cacheDir, "media/file"))
        val total = getFolderSize(cacheDir)
        
        mapOf(
            "images" to images,
            "videos" to videos,
            "files" to files,
            "others" to (total - (images + videos + files)).coerceAtLeast(0),
            "total" to total
        )
    }

    override suspend fun clearCache(types: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            fun deleteFolder(file: java.io.File) {
                if (!file.exists()) return
                if (file.isDirectory) file.listFiles()?.forEach { deleteFolder(it) }
                file.delete()
            }

            types.forEach { type ->
                when (type) {
                    "images" -> deleteFolder(java.io.File(cacheDir, "media/image"))
                    "videos" -> deleteFolder(java.io.File(cacheDir, "media/video"))
                    "files" -> deleteFolder(java.io.File(cacheDir, "media/file"))
                    "all" -> {
                        deleteFolder(java.io.File(cacheDir, "media"))
                        deleteFolder(java.io.File(context.filesDir, "media"))
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearSearchHistory(): Result<Unit> = Result.success(Unit)
    override suspend fun clearRecentEmojis(): Result<Unit> = Result.success(Unit)

    override suspend fun resetToDefault(): Result<Unit> {
        val userId = _settings.value.userId
        return updateSettings(UserSettings(userId = userId))
    }

    override fun stopSettingsSync() {
        syncJob?.cancel()
        repositoryScope.launch {
            realtimeChannel?.let { 
                try {
                    getSupabase().realtime.removeChannel(it)
                } catch (_: Exception) {}
            }
            realtimeChannel = null
        }
    }

    override fun shutdown() {
        stopSettingsSync()
        repositoryScope.cancel()
    }
}
