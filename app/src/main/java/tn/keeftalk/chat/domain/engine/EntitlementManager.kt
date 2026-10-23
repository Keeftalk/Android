package tn.keeftalk.chat.domain.engine

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.entitlement.*
import tn.keeftalk.chat.util.KeeftalkStore
import java.util.Locale

sealed class RedeemResult {
    data class Success(
        val title: String,
        val description: String,
        val isProUnlocked: Boolean = false,
        val unlockedFeatures: List<String> = emptyList()
    ) : RedeemResult()

    data class Error(val message: String) : RedeemResult()
}

@Serializable
private data class RedeemResponseDto(
    val success: Boolean = false,
    val type: String? = null,
    val title: String? = null,
    val message: String? = null,
    val error: String? = null,
    @SerialName("plan_type") val planType: String? = null
)

class EntitlementManager(
    private val context: Context,
    private val store: KeeftalkStore = KeeftalkStore.getInstance()
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeEntitlements = MutableStateFlow<Set<String>>(loadCachedEntitlements())
    val activeEntitlements: StateFlow<Set<String>> = _activeEntitlements.asStateFlow()

    private val _isUniversalPremium = MutableStateFlow(loadCachedPremiumStatus())
    val isUniversalPremium: StateFlow<Boolean> = _isUniversalPremium.asStateFlow()

    init {
        scope.launch {
            refreshEntitlementsFromCloud()
        }
    }

    private fun loadCachedEntitlements(): Set<String> {
        val cached = store.getString("cached_user_entitlements", null)
        return if (!cached.isNullOrEmpty()) {
            try {
                json.decodeFromString<Set<String>>(cached)
            } catch (_: Exception) {
                emptySet()
            }
        } else {
            emptySet()
        }
    }

    private fun loadCachedPremiumStatus(): Boolean {
        return store.getBoolean("cached_premium_status", false)
    }

    private fun saveCachedEntitlements(entitlements: Set<String>, isPremium: Boolean) {
        try {
            store.putString("cached_user_entitlements", json.encodeToString(entitlements))
            store.putBoolean("cached_premium_status", isPremium)
            _activeEntitlements.value = entitlements
            _isUniversalPremium.value = isPremium
        } catch (e: Exception) {
            android.util.Log.e("EntitlementManager", "Failed to cache entitlements locally", e)
        }
    }

    suspend fun refreshEntitlementsFromCloud(): Result<Unit> {
        return runCatching {
            val supabase: SupabaseClient = AppModule.provideSupabaseClientAsync(context)
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@runCatching Unit

            val entitlements = supabase.postgrest["user_feature_entitlements"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("status", "ACTIVE")
                    }
                }.decodeList<UserFeatureEntitlement>()

            val activeKeys = entitlements.filter { it.enabled }.map { it.featureId }.toSet()

            val profile = supabase.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<tn.keeftalk.chat.domain.model.Profile>()

            val isPremium = profile?.planType == tn.keeftalk.chat.domain.model.SubscriptionPlan.PRO_MONTHLY ||
                    profile?.planType == tn.keeftalk.chat.domain.model.SubscriptionPlan.FAMILY_MONTHLY

            saveCachedEntitlements(activeKeys, isPremium)
            Unit
        }
    }

    fun hasFeatureSync(featureId: FeatureId): Flow<Boolean> {
        if (featureId.isAlwaysFree) {
            return flowOf(true)
        }
        return combine(_activeEntitlements, _isUniversalPremium) { entitlements, isPremium ->
            isPremium || entitlements.contains(featureId.key)
        }
    }

    fun hasUniversalPremium(): Flow<Boolean> = _isUniversalPremium.asStateFlow()

    suspend fun redeemCode(
        code: String,
        authRepository: tn.keeftalk.chat.domain.repository.AuthRepository? = null
    ): RedeemResult {
        val cleanCode = code.trim().uppercase(Locale.ROOT)
        if (cleanCode.isBlank()) {
            return RedeemResult.Error("Please enter a valid redemption code.")
        }

        return try {
            val supabase = AppModule.provideSupabaseClientAsync(context)
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId == null) {
                return RedeemResult.Error("Authentication required. Please sign in to redeem codes.")
            }

            // Call Backend RPC in Supabase
            val rpcResult = supabase.postgrest.rpc(
                function = "redeem_code",
                parameters = buildJsonObject {
                    put("p_code", cleanCode)
                }
            )

            val response = json.decodeFromString<RedeemResponseDto>(rpcResult.data)

            if (response.success) {
                // Refresh local entitlements and profile from Supabase SSOT
                refreshEntitlementsFromCloud()
                authRepository?.refreshProfile(userId)

                RedeemResult.Success(
                    title = response.title ?: "Redemption Successful!",
                    description = response.message ?: "Code activated on your account.",
                    isProUnlocked = response.planType == "PRO_MONTHLY" || response.planType == "FAMILY_MONTHLY"
                )
            } else {
                RedeemResult.Error(response.error ?: "Invalid or expired redemption code.")
            }
        } catch (e: Exception) {
            android.util.Log.e("EntitlementManager", "Backend redeem code RPC failed", e)
            RedeemResult.Error("Backend verification failed. Please ensure you are online and try again.")
        }
    }
}
