package com.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.data.remote.UserAppCustomizationDto
import com.keeftalk.chat.domain.model.AppCustomization
import com.keeftalk.chat.domain.repository.AppCustomizationRepository
import com.keeftalk.chat.di.AppModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
            delay(3.seconds)
            val supabase = getSupabase()
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
                val default = AppCustomization.default(userId)
                supabase.postgrest["user_app_customization"].upsert(UserAppCustomizationDto.fromDomain(default))
                _customization.value = default
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
            val userId = customization.userId
            if (userId.isNotEmpty() && customization.syncEnabled) {
                val dto = UserAppCustomizationDto.fromDomain(customization)
                getSupabase().postgrest["user_app_customization"].upsert(dto)
            }
            _customization.value = customization
            prefs.updateAppCustomization(customization)
            Result.success(Unit)
        } catch (e: Exception) {
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
