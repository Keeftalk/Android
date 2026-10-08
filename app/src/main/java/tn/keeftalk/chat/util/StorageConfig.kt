package tn.keeftalk.chat.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StorageBuckets(
    @SerialName("private") val private: String = "keeftalk-weur-private",
    @SerialName("public") val public: String = "keeftalk-weur-public",
    @SerialName("market") val market: String = "keeftalk-weur-market",
    @SerialName("detention") val detention: String = "keeftalk-weur-detention",
    @SerialName("backups") val backups: String = "keeftalk-weur-backups"
)

@Serializable
data class UserStorageConfig(
    @SerialName("storage_region") val storageRegion: String = "WEUR",
    @SerialName("country_code") val countryCode: String = "TN",
    @SerialName("buckets") val buckets: StorageBuckets = StorageBuckets(),
    @SerialName("assignment_version") val assignmentVersion: Int = 1
)

/**
 * Single SSOT for Cloudflare R2 Storage Buckets, Account ID, and CDN Endpoints.
 *
 * Account ID: 5cfb75b2169676dc91bd6a57b7d6d7ab
 * S3 API Endpoint: https://5cfb75b2169676dc91bd6a57b7d6d7ab.r2.cloudflarestorage.com
 */
object StorageConfig {
    const val ACCOUNT_ID = "5cfb75b2169676dc91bd6a57b7d6d7ab"
    const val S3_ENDPOINT = "https://5cfb75b2169676dc91bd6a57b7d6d7ab.r2.cloudflarestorage.com"

    // Legacy Fallback Buckets
    const val BUCKET_PRIVATE = "keeftalk-private"
    const val BUCKET_PUBLIC = "keeftalk-public"
    const val BUCKET_MARKET = "keeftalk-market"
    const val BUCKET_DETENTION = "30-day-detention"

    // CDN Domains
    const val CDN_PUBLIC = "public.cdn.keeftalk.com"
    const val CDN_MARKET = "cdn.market.keeftalk.com"

    const val PUBLIC_CDN_BASE = "https://$CDN_PUBLIC"
    const val MARKET_CDN_BASE = "https://$CDN_MARKET"

    /**
     * Builds standard object key adhering to: {country_code}/{user_id}/{category}/...
     */
    fun buildUserKey(countryCode: String, userId: String, category: String, relativePath: String): String {
        val cleanCountry = if (countryCode.isNotBlank()) countryCode.uppercase().trim() else "TN"
        val cleanCat = category.trim('/')
        val cleanRel = relativePath.trim('/')
        return if (cleanCat.isEmpty()) "$cleanCountry/$userId/$cleanRel" else "$cleanCountry/$userId/$cleanCat/$cleanRel"
    }

    /**
     * Backwards-compatible overload defaulting to "TN"
     */
    fun buildUserKey(userId: String, category: String, relativePath: String): String {
        return buildUserKey("TN", userId, category, relativePath)
    }

    /**
     * Resolves public CDN URL for public media.
     * Example: https://public.cdn.keeftalk.com/TN/{user_id}/avatars/avatar_123.jpg
     */
    fun getPublicCdnUrl(objectKey: String): String {
        val cleanKey = objectKey.trim('/')
        return "$PUBLIC_CDN_BASE/$cleanKey"
    }

    /**
     * Resolves Market CDN URL for market media.
     * Example: https://cdn.market.keeftalk.com/TN/{user_id}/item_123.jpg
     */
    fun getMarketCdnUrl(objectKey: String): String {
        val cleanKey = objectKey.trim('/')
        return "$MARKET_CDN_BASE/$cleanKey"
    }
}
