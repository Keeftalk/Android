package tn.keeftalk.chat.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class XpEventDto(
    val eventId: String,
    val contentHash: String,
    val pageKeyOrRange: String,
    val dwellMs: Long,
    val anchoredAtMs: Long,
    val anchored: Boolean,
    val credit: Double,
    val flags: Int
)

@Serializable
data class ServerXp(
    val serverTotalXp: Long,
    val syncedEventIds: List<String>
)

@Serializable
data class GlobalRankDto(
    @SerialName("global_rank") val globalRank: Long = 1,
    @SerialName("total_readers") val totalReaders: Long = 1,
    @SerialName("percentile") val percentile: Int = 100
)

interface XpSyncApi {
    suspend fun serverTimeMs(): Long
    suspend fun upload(events: List<XpEventDto>): Result<ServerXp>
    suspend fun syncUserXpAndGetRank(supabase: SupabaseClient, totalXp: Long, username: String? = null): Result<GlobalRankDto>
}

interface IntegrityProvider {
    suspend fun token(): String?
}

class SupabaseXpSyncApi : XpSyncApi {
    override suspend fun serverTimeMs(): Long = System.currentTimeMillis()

    override suspend fun upload(events: List<XpEventDto>): Result<ServerXp> {
        return Result.success(ServerXp(0L, events.map { it.eventId }))
    }

    override suspend fun syncUserXpAndGetRank(
        supabase: SupabaseClient,
        totalXp: Long,
        username: String?
    ): Result<GlobalRankDto> {
        return runCatching {
            val params = buildJsonObject {
                put("p_total_xp", totalXp)
                if (!username.isNullOrEmpty()) {
                    put("p_username", username)
                }
            }
            supabase.postgrest.rpc(
                function = "upsert_user_xp_and_get_rank",
                parameters = params
            ).decodeSingle<GlobalRankDto>()
        }
    }
}

class XpSyncApiNoOp : XpSyncApi {
    override suspend fun serverTimeMs(): Long = System.currentTimeMillis()

    override suspend fun upload(events: List<XpEventDto>): Result<ServerXp> =
        Result.success(ServerXp(0L, events.map { it.eventId }))

    override suspend fun syncUserXpAndGetRank(
        supabase: SupabaseClient,
        totalXp: Long,
        username: String?
    ): Result<GlobalRankDto> {
        return Result.success(GlobalRankDto(1, 1, 100))
    }
}

class IntegrityProviderNoOp : IntegrityProvider {
    override suspend fun token(): String? = null
}
