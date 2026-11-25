package tn.keeftalk.chat.domain.engine

import android.content.Context
import android.util.Log
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.entitlement.FeatureId
import java.util.Locale

@Serializable
private data class RedeemResponseDto(
    val success: Boolean,
    val message: String? = null,
    val title: String? = null,
    val error: String? = null,
    @SerialName("plan_type") val planType: String? = null
)

sealed class RedeemResult {
    data class Success(
        val title: String,
        val description: String,
        val isProUnlocked: Boolean,
        val unlockedFeatures: List<String> = emptyList()
    ) : RedeemResult()
    data class Error(val message: String) : RedeemResult()
}

class EntitlementManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _activeEntitlements = MutableStateFlow<Set<String>>(FeatureId.entries.map { it.key }.toSet())
    val activeEntitlements = _activeEntitlements.asStateFlow()

    private val _isUniversalPremium = MutableStateFlow(true)
    val isUniversalPremium = _isUniversalPremium.asStateFlow()

    init {
        scope.launch {
            _activeEntitlements.value = FeatureId.entries.map { it.key }.toSet()
            _isUniversalPremium.value = true
        }
    }

    suspend fun refreshEntitlementsFromCloud() {
        _activeEntitlements.value = FeatureId.entries.map { it.key }.toSet()
        _isUniversalPremium.value = true
    }

    fun hasFeatureSync(featureId: FeatureId): Flow<Boolean> = flowOf(true)

    fun hasUniversalPremium(): Flow<Boolean> = flowOf(true)

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

            val rpcResult = supabase.postgrest.rpc(
                function = "redeem_code",
                parameters = buildJsonObject {
                    put("p_code", cleanCode)
                }
            )

            val response = json.decodeFromString<RedeemResponseDto>(rpcResult.data)

            if (response.success) {
                refreshEntitlementsFromCloud()
                authRepository?.refreshProfile(userId)

                RedeemResult.Success(
                    title = response.title ?: "Redemption Successful!",
                    description = response.message ?: "Code activated on your account.",
                    isProUnlocked = true,
                    unlockedFeatures = listOf("All Super App Features Unlocked")
                )
            } else {
                RedeemResult.Error(response.error ?: "Invalid or expired redemption code.")
            }
        } catch (e: Exception) {
            Log.e("EntitlementManager", "Error redeeming code: ${e.message}", e)
            RedeemResult.Error(e.message ?: "Redemption failed due to network error.")
        }
    }
}
