package com.keeftalk.chat.data.repository

import android.util.Log
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.data.remote.UserPrivacySettingsDto
import com.keeftalk.chat.domain.model.UserPrivacySettings
import com.keeftalk.chat.domain.repository.PrivacyRepository
import com.keeftalk.chat.domain.repository.ProfileView
import com.keeftalk.chat.di.AppModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.*

class PrivacyRepositoryImpl(
    private val context: android.content.Context,
    private val prefs: UserPreferencesRepository,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : PrivacyRepository {

    private val _privacySettings = MutableStateFlow<UserPrivacySettings?>(null)
    override val privacySettings: Flow<UserPrivacySettings> = _privacySettings.filterNotNull()
    private var settingsChannel: RealtimeChannel? = null

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    init {
        // Light-weight constructor: Defer work to avoid blocking main thread or early Supabase init
        com.keeftalk.chat.util.StartupOrchestrator.enqueue(com.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
            externalScope.launch {
                val supabase = getSupabase()
                supabase.auth.sessionStatus.collect { status ->
                    if (status is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
                        observePrivacySettings(status.session.user?.id ?: return@collect)
                    }
                }
            }
        }
    }

    private suspend fun observePrivacySettings(userId: String) {
        val supabase = getSupabase()
        // Initial fetch
        try {
            val dto = supabase.postgrest["user_privacy_settings"]
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<UserPrivacySettingsDto>()
            
            dto?.let {
                val domain = it.toDomain()
                _privacySettings.value = domain
                syncToLocalPrefs(domain)
            }
        } catch (e: Exception) {
            Log.e("PrivacyRepo", "Failed to fetch privacy settings", e)
        }

        // Realtime updates
        try {
            settingsChannel?.let {
                try { it.unsubscribe() } catch (_: Exception) {}
                supabase.realtime.removeChannel(it)
            }
            
            val channel = supabase.realtime.channel("privacy_settings_sync")
            settingsChannel = channel

            channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "user_privacy_settings"
            }.onEach { action ->
                val dto = action.decodeRecord<UserPrivacySettingsDto>()
                if (dto.userId == userId) {
                    val domain = dto.toDomain()
                    _privacySettings.value = domain
                    syncToLocalPrefs(domain)
                }
            }.launchIn(externalScope)
            
            channel.subscribe()
        } catch (e: Exception) {
            Log.e("PrivacyRepo", "Realtime privacy sync error", e)
        }
    }

    private suspend fun syncToLocalPrefs(settings: UserPrivacySettings) {
        prefs.updatePrivacySettings(
            com.keeftalk.chat.data.prefs.UserPreferences(
                profilePhotoVisibility = settings.profilePhotoVisibility.name,
                aboutVisibility = settings.aboutVisibility.name,
                lastSeenVisibility = settings.lastSeenVisibility.name,
                readReceiptsEnabled = settings.readReceiptsEnabled,
                typingIndicatorsEnabled = settings.typingIndicatorsEnabled,
                profileViewHistoryEnabled = settings.profileViewHistoryEnabled,
                whoCanCallMe = settings.callPermission.name,
                whoCanAddToGroups = settings.groupPermission.name,
                screenshotProtectionEnabled = settings.screenshotProtectionEnabled,
                biometricLockEnabled = settings.biometricLockEnabled,
                biometricTimeout = settings.biometricTimeoutMinutes
            )
        )
    }

    override suspend fun updateSettings(settings: UserPrivacySettings) {
        try {
            syncToLocalPrefs(settings)
            _privacySettings.value = settings
            val dto = UserPrivacySettingsDto.fromDomain(settings)
            getSupabase().postgrest["user_privacy_settings"].upsert(dto)
        } catch (e: Exception) {
            Log.e("PrivacyRepo", "Failed to update privacy settings", e)
            throw e
        }
    }

    override suspend fun updateSetting(key: String, value: Any) {
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        try {
            prefs.updatePrivacySetting(key, value)
            val column = when (key) {
                "profilePhotoVisibility" -> "profile_photo_visibility"
                "aboutVisibility" -> "about_visibility"
                "lastSeenVisibility" -> "last_seen_visibility"
                "readReceiptsEnabled" -> "read_receipts_enabled"
                "typingIndicatorsEnabled" -> "typing_indicators_enabled"
                "profileViewHistoryEnabled" -> "profile_view_history_enabled"
                "whoCanCallMe" -> "call_permission"
                "whoCanAddToGroups" -> "group_permission"
                "screenshotProtectionEnabled" -> "screenshot_protection_enabled"
                "biometricLockEnabled" -> "biometric_lock_enabled"
                "biometricTimeout" -> "biometric_timeout_minutes"
                else -> null
            }

            column?.let { col ->
                supabase.postgrest["user_privacy_settings"].update({
                    set(col, value)
                }) {
                    filter { eq("user_id", userId) }
                }
            }
        } catch (e: Exception) {
            Log.e("PrivacyRepo", "Failed to update privacy setting: $key", e)
        }
    }

    override suspend fun getBlockedUsers(): Flow<List<String>> = flow {
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@flow
        try {
            val response = supabase.postgrest["blocked_users"]
                .select { filter { eq("blocker_id", userId) } }
                .decodeList<JsonObject>()
            emit(response.mapNotNull { it["blocked_id"]?.jsonPrimitive?.content })
        } catch (e: Exception) {
            Log.e("PrivacyRepo", "Failed to get blocked users", e)
            emit(emptyList())
        }
    }

    override suspend fun blockUser(userId: String) {
        val supabase = getSupabase()
        val currentId = supabase.auth.currentUserOrNull()?.id ?: return
        try {
            supabase.postgrest["blocked_users"].insert(buildJsonObject {
                put("blocker_id", currentId)
                put("blocked_id", userId)
            })
        } catch (e: Exception) {
            Log.e("PrivacyRepo", "Failed to block user", e)
        }
    }

    override suspend fun unblockUser(userId: String) {
        val supabase = getSupabase()
        val currentId = supabase.auth.currentUserOrNull()?.id ?: return
        try {
            supabase.postgrest["blocked_users"].delete {
                filter {
                    eq("blocker_id", currentId)
                    eq("blocked_id", userId)
                }
            }
        } catch (e: Exception) {
            Log.e("PrivacyRepo", "Failed to unblock user", e)
        }
    }

    override suspend fun recordProfileView(viewedUserId: String) {
        val supabase = getSupabase()
        val currentId = supabase.auth.currentUserOrNull()?.id ?: return
        if (currentId == viewedUserId) return

        try {
            // Fetch both settings to check privacy
            val mySettings = _privacySettings.value ?: return

            val otherSettingsDto = supabase.postgrest["user_privacy_settings"]
                .select { filter { eq("user_id", viewedUserId) } }
                .decodeSingleOrNull<UserPrivacySettingsDto>()

            val otherSettings = otherSettingsDto?.toDomain()

            // Record view with privacy snapshots
            supabase.postgrest["profile_views"].insert(buildJsonObject {
                put("viewer_id", currentId)
                put("viewed_id", viewedUserId)
                put("viewer_privacy_level", if (mySettings.profileViewHistoryEnabled) "EVERYONE" else "NOBODY")
                put("viewed_privacy_level", if (otherSettings?.profileViewHistoryEnabled == true) "EVERYONE" else "NOBODY")
            })
        } catch (e: Exception) {
            Log.e("PrivacyRepo", "Failed to record profile view", e)
        }
    }

    override suspend fun getProfileViews(limit: Int, offset: Int): Flow<List<ProfileView>> = flow {
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@flow
        try {
            val response = supabase.postgrest["profile_views"]
                .select(columns = Columns.raw("*, viewer:profiles!viewer_id(username, avatar_url)")) {
                    filter { eq("viewed_id", userId) }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    range(offset.toLong(), (offset + limit).toLong())
                }
                .decodeList<JsonObject>()

            val views = response.map { json ->
                val viewer = json["viewer"]?.jsonObject
                val viewerPrivacy = json["viewer_privacy_level"]?.jsonPrimitive?.content ?: "EVERYONE"
                val viewedPrivacy = json["viewed_privacy_level"]?.jsonPrimitive?.content ?: "EVERYONE"

                val isHidden = viewerPrivacy == "NOBODY" || viewedPrivacy == "NOBODY"

                ProfileView(
                    viewerId = if (isHidden) "" else json["viewer_id"]?.jsonPrimitive?.content ?: "",
                    viewedUserId = userId,
                    viewedAt = 0, // Should parse from created_at
                    viewerName = if (isHidden) "Someone" else viewer?.get("username")?.jsonPrimitive?.content,
                    viewerAvatarUrl = if (isHidden) null else viewer?.get("avatar_url")?.jsonPrimitive?.content
                )
            }
            emit(views)
        } catch (e: Exception) {
            Log.e("PrivacyRepo", "Failed to get profile views", e)
            emit(emptyList())
        }
    }

    override suspend fun deleteAccount() {
        try {
            getSupabase().auth.signOut()
        } catch (e: Exception) {
            Log.e("PrivacyRepo", "Failed to delete account", e)
        }
    }

    override suspend fun downloadMyData(): Result<String> = runCatching {
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
        
        val profile = supabase.postgrest["profiles"].select { filter { eq("id", userId) } }.decodeSingle<JsonObject>()
        val settings = supabase.postgrest["user_privacy_settings"].select { filter { eq("user_id", userId) } }.decodeSingleOrNull<JsonObject>() ?: JsonObject(emptyMap())
        
        val data = buildJsonObject {
            put("profile", profile)
            put("settings", settings)
        }
        data.toString()
    }

    override fun shutdown() {
        Log.i("PrivacyRepo", "Shutting down PrivacyRepository")
        externalScope.cancel()
    }
}
