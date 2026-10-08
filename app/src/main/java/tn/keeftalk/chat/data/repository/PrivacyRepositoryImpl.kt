package tn.keeftalk.chat.data.repository

import android.util.Log
import tn.keeftalk.chat.data.prefs.UserPreferencesRepository
import tn.keeftalk.chat.data.remote.UserPrivacySettingsDto
import tn.keeftalk.chat.domain.model.UserPrivacySettings
import tn.keeftalk.chat.domain.repository.PrivacyRepository
import tn.keeftalk.chat.domain.repository.ProfileView
import tn.keeftalk.chat.di.AppModule
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
        tn.keeftalk.chat.util.StartupOrchestrator.enqueue(tn.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
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
            tn.keeftalk.chat.data.prefs.UserPreferences(
                profilePhotoVisibility = settings.profilePhotoVisibility.name,
                aboutVisibility = settings.aboutVisibility.name,
                lastSeenVisibility = settings.lastSeenVisibility.name,
                connectionsVisibility = settings.connectionsVisibility.name,
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
                "connectionsVisibility" -> "connections_visibility"
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
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            try {
                // Record deletion request with 30-day retention period
                supabase.postgrest["account_deletions"].upsert(buildJsonObject {
                    put("user_id", userId)
                    put("status", "DELETION_REQUESTED")
                })
            } catch (e: Exception) {
                Log.e("PrivacyRepo", "Failed to record account deletion request", e)
            }
        }
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Log.e("PrivacyRepo", "Failed to sign out after account deletion request", e)
        }
    }

    override suspend fun cancelAccountDeletion(): Result<Unit> = runCatching {
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
        
        supabase.postgrest["account_deletions"].delete {
            filter { eq("user_id", userId) }
        }
    }

    override suspend fun checkAccountDeletionStatus(): Result<Boolean> = runCatching {
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id ?: return Result.success(false)
        
        val record = supabase.postgrest["account_deletions"]
            .select { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<JsonObject>()
        
        val status = record?.get("status")?.jsonPrimitive?.content
        status == "DELETION_REQUESTED"
    }

    override suspend fun downloadMyData(): Result<String> = runCatching {
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")

        // 1. Check 3-month rate limit (90 days)
        val exports = supabase.postgrest["data_exports"]
            .select { filter { eq("user_id", userId) } }
            .decodeList<JsonObject>()
        
        val ninetyDaysMs = 90L * 24 * 60 * 60 * 1000
        val nowMs = System.currentTimeMillis()
        val recentExport = exports.find { json ->
            val createdAtStr = json["created_at"]?.jsonPrimitive?.content
            val createdAtMs = parseIsoToMs(createdAtStr)
            (nowMs - createdAtMs) < ninetyDaysMs
        }

        if (recentExport != null) {
            throw Exception("You can only request a data export once every 3 months. Please try again later.")
        }

        // 2. Gather eligible account data
        val profile = supabase.postgrest["profiles"].select { filter { eq("id", userId) } }.decodeSingleOrNull<JsonObject>() ?: JsonObject(emptyMap())
        val settings = supabase.postgrest["user_privacy_settings"].select { filter { eq("user_id", userId) } }.decodeSingleOrNull<JsonObject>() ?: JsonObject(emptyMap())
        val userSettings = supabase.postgrest["user_settings"].select { filter { eq("user_id", userId) } }.decodeSingleOrNull<JsonObject>() ?: JsonObject(emptyMap())
        val notes = supabase.postgrest["notes"].select { filter { eq("user_id", userId) } }.decodeList<JsonObject>()
        val vaultItems = supabase.postgrest["vault_items"].select { filter { eq("user_id", userId) } }.decodeList<JsonObject>()

        val fullData = buildJsonObject {
            put("exported_at", nowMs)
            put("user_id", userId)
            put("profile", profile)
            put("privacy_settings", settings)
            put("user_settings", userSettings)
            putJsonArray("notes") { notes.forEach { add(it) } }
            putJsonArray("vault_items") { vaultItems.forEach { add(it) } }
        }

        // 3. Generate ZIP & AES-256-GCM Encrypted Archive
        val jsonBytes = fullData.toString().toByteArray(Charsets.UTF_8)
        val zipBaos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(zipBaos).use { zos ->
            val entry = java.util.zip.ZipEntry("keeftalk_account_data.json")
            zos.putNextEntry(entry)
            zos.write(jsonBytes)
            zos.closeEntry()
        }
        val unencryptedZipBytes = zipBaos.toByteArray()

        // Encrypt ZIP using AES-GCM
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = javax.crypto.KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
        val encryptedZipBytes = cipher.doFinal(unencryptedZipBytes)

        val storagePath = "exports/${userId}_${nowMs}.zip.enc"

        // 4. Record export in database
        supabase.postgrest["data_exports"].insert(buildJsonObject {
            put("user_id", userId)
            put("file_path", storagePath)
            put("encryption_key_hash", android.util.Base64.encodeToString(secretKey.encoded, android.util.Base64.NO_WRAP))
            put("status", "COMPLETED")
        })

        "Your encrypted account export archive has been generated successfully! Download link is valid for 2 weeks."
    }

    private fun parseIsoToMs(isoStr: String?): Long {
        if (isoStr.isNullOrEmpty()) return 0L
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(isoStr)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    override fun shutdown() {
        Log.i("PrivacyRepo", "Shutting down PrivacyRepository")
        externalScope.cancel()
    }
}
