package tn.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import tn.keeftalk.chat.data.prefs.UserPreferencesRepository
import tn.keeftalk.chat.data.remote.UserAppCustomizationDto
import tn.keeftalk.chat.domain.model.AppCustomization
import tn.keeftalk.chat.domain.repository.AppCustomizationRepository
import tn.keeftalk.chat.di.AppModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

class AppCustomizationRepositoryImpl(
    private val context: Context,
    private val prefs: UserPreferencesRepository
) : AppCustomizationRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _customization = MutableStateFlow(AppCustomization())
    override val customization: Flow<AppCustomization> = _customization.asStateFlow()
    private var syncChannel: RealtimeChannel? = null
    private val fetchMutex = Mutex()

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    init {
        repositoryScope.launch {
            val cachedPrefs = prefs.userPreferencesFlow.first()
            _customization.value = cachedPrefs.appCustomization
        }
    }

    override fun startCustomizationObservation() {
        repositoryScope.launch {
            val supabase = getSupabase()
            val currentUserId = supabase.auth.currentUserOrNull()?.id
            if (currentUserId != null) {
                fetchAndObserveCustomization(currentUserId)
            }

            supabase.auth.sessionStatus
                .map { supabase.auth.currentUserOrNull()?.id }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId != null) {
                        fetchAndObserveCustomization(userId)
                    }
                }
        }
    }

    private suspend fun fetchAndObserveCustomization(userId: String) = fetchMutex.withLock {
        val supabase = getSupabase()
        try {
            val remote = supabase.postgrest["user_app_customization"]
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<UserAppCustomizationDto>()
            
            if (remote != null) {
                val domain = remote.toDomain()
                _customization.value = domain
                prefs.updateAppCustomization(domain)
            } else {
                val localWithUserId = _customization.value.copy(userId = userId)
                supabase.postgrest["user_app_customization"].upsert(UserAppCustomizationDto.fromDomain(localWithUserId))
                _customization.value = localWithUserId
                prefs.updateAppCustomization(localWithUserId)
            }
        } catch (e: Exception) {
            Log.e("AppCustomizationRepo", "Failed to fetch customization", e)
        }

        try {
            syncChannel?.let {
                supabase.realtime.removeChannel(it)
            }
            
            val channel = supabase.realtime.channel("app_customization_sync")
            syncChannel = channel

            channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "user_app_customization"
            }.onEach { action ->
                val dto = action.decodeRecord<UserAppCustomizationDto>()
                if (dto.userId == userId) {
                    val domain = dto.toDomain()
                    // Only apply remote changes if sync is enabled locally
                    if (_customization.value.syncEnabled) {
                        _customization.value = domain
                        prefs.updateAppCustomization(domain)
                    }
                }
            }.launchIn(repositoryScope)
            
            repositoryScope.launch {
                channel.subscribe()
            }
        } catch (e: Exception) {
            Log.e("AppCustomizationRepo", "Realtime setup error", e)
        }
    }

    override suspend fun updateCustomization(customization: AppCustomization): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val resolvedUserId = customization.userId.ifEmpty {
                supabase.auth.currentUserOrNull()?.id ?: ""
            }
            val targetCustomization = if (resolvedUserId.isNotEmpty() && customization.userId != resolvedUserId) {
                customization.copy(userId = resolvedUserId)
            } else {
                customization
            }

            if (resolvedUserId.isNotEmpty() && targetCustomization.syncEnabled) {
                val dto = UserAppCustomizationDto.fromDomain(targetCustomization)
                supabase.postgrest["user_app_customization"].upsert(dto)
            }

            _customization.value = targetCustomization
            prefs.updateAppCustomization(targetCustomization)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AppCustomizationRepo", "Failed to update customization remotely", e)
            _customization.value = customization
            prefs.updateAppCustomization(customization)
            Result.failure(e)
        }
    }

    override suspend fun syncCustomization(): Result<Unit> = withContext(Dispatchers.IO) {
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: _customization.value.userId.takeIf { it.isNotEmpty() }
            ?: return@withContext Result.failure(Exception("User not authenticated"))

        try {
            val remote = supabase.postgrest["user_app_customization"]
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<UserAppCustomizationDto>()

            if (remote != null) {
                val domain = remote.toDomain()
                _customization.value = domain
                prefs.updateAppCustomization(domain)
            } else {
                val currentWithUserId = _customization.value.copy(userId = userId)
                supabase.postgrest["user_app_customization"].upsert(UserAppCustomizationDto.fromDomain(currentWithUserId))
                _customization.value = currentWithUserId
                prefs.updateAppCustomization(currentWithUserId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AppCustomizationRepo", "Failed manual sync", e)
            Result.failure(e)
        }
    }

    override suspend fun resetToDefault(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = _customization.value.userId
        updateCustomization(AppCustomization.default(userId))
    }

    override fun shutdown() {
        Log.i("AppCustomizationRepo", "Shutting down AppCustomizationRepository")
        repositoryScope.cancel()
    }
}
