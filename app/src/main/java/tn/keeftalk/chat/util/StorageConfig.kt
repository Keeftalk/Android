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
     * Builds standard object key:
     * - Public categories (avatars, covers, wallpapers, market, public): {user_id}/{category}/{relative_path}
     * - Private categories (files, archives, etc.): {country_code}/{user_id}/{category}/{relative_path}
     */
    fun buildUserKey(countryCode: String, userId: String, category: String, relativePath: String): String {
        val cleanCat = category.trim('/').lowercase()
        val cleanRel = relativePath.trim('/')
        
        if (cleanCat in listOf("avatars", "covers", "wallpapers", "market", "public")) {
            return if (cleanCat.isEmpty()) "$userId/$cleanRel" else "$userId/$cleanCat/$cleanRel"
        }

        val cleanCountry = if (countryCode.isNotBlank()) countryCode.uppercase().trim() else "TN"
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
     * Example: https://public.cdn.keeftalk.com/{user_id}/avatars/{picture_id}/original
     */
    fun getPublicCdnUrl(objectKey: String): String {
        var cleanKey = objectKey.trim('/')
        // Strip duplicate bucket prefix or legacy country prefix if present in public CDN keys
        if (cleanKey.startsWith("keeftalk-public/")) {
            cleanKey = cleanKey.removePrefix("keeftalk-public/").trim('/')
        }
        // Handle legacy URLs with country prefix (e.g. TN/avatars/...)
        val parts = cleanKey.split('/')
        if (parts.size >= 3 && parts[0].length == 2 && parts[1].lowercase() in listOf("avatars", "covers", "wallpapers", "market", "public")) {
            cleanKey = parts.drop(1).joinToString("/")
        }
        return "$PUBLIC_CDN_BASE/$cleanKey"
    }

    /**
     * Resolves Market CDN URL for market media.
     * Example: https://cdn.market.keeftalk.com/{user_id}/item_123.jpg
     */
    fun getMarketCdnUrl(objectKey: String): String {
        var cleanKey = objectKey.trim('/')
        if (cleanKey.startsWith("keeftalk-market/")) {
            cleanKey = cleanKey.removePrefix("keeftalk-market/").trim('/')
        }
        return "$MARKET_CDN_BASE/$cleanKey"
    }
}
