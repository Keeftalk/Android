package tn.keeftalk.chat.domain.model.entitlement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FeatureId(val key: String, val displayName: String, val isAlwaysFree: Boolean) {
    CALLS("calls_sync", "My Calls Sync", false),
    NOTES("notes_sync", "My Notes Sync", false),
    CALENDAR("calendar_sync", "My Calendar Sync", false),
    FINANCES("finances_sync", "My Finances Sync", false),
    BOOKS("books_sync", "My Books Sync", false),
    MY_DIN("mydin_sync", "My Din Sync", true),
    DOCS("docs_sync", "My Docs Sync", false),
    HABITS("habits_sync", "My Habits Sync", false),
    BROWSER("browser_sync", "My Browser Sync", false),
    TODO("todo_sync", "My Todo Sync", false),
    FEED("feed_sync", "My Feed Sync", false),
    SMS_MMS("sms_mms_sync", "My SMS/MMS Sync", false),
    EMAIL_DATA("email_sync", "My E-Mail Sync", false),
    SECRETS("secrets_sync", "My Secrets Sync", false),

    // Always Free Features
    CHATS("chats_sync", "My Chats", true),
    VAULT("vault_sync", "My Vault", true),
    MAPS("maps_sync", "My Maps", true),
    MARKET("market_sync", "My Market", true);

    companion object {
        fun fromKey(key: String): FeatureId? = values().firstOrNull { it.key == key }
    }
}

@Serializable
data class FeatureSyncPlan(
    val id: String,
    @SerialName("feature_id") val featureId: String,
    @SerialName("display_name") val displayName: String,
    val description: String = "",
    val price: Double = 0.99,
    val currency: String = "USD",
    @SerialName("billing_period") val billingPeriod: String = "MONTHLY",
    val enabled: Boolean = true
)

@Serializable
data class UserFeatureEntitlement(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("feature_id") val featureId: String,
    val enabled: Boolean = true,
    val status: String = "ACTIVE", // ACTIVE, EXPIRED, CANCELLED, REFUNDED
    @SerialName("purchased_at") val purchasedAt: Long = System.currentTimeMillis(),
    @SerialName("expires_at") val expiresAt: Long? = null
)

@Serializable
data class FamilyGroup(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("storage_limit_bytes") val storageLimitBytes: Long = 2199023255552L, // 2 TB shared
    @SerialName("max_total_users") val maxTotalUsers: Int = 6
)

@Serializable
data class FamilyMember(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("user_id") val userId: String,
    val role: String = "member", // owner or member
    val status: String = "ACTIVE",
    @SerialName("added_at") val addedAt: Long = System.currentTimeMillis(),
    @SerialName("eligible_for_removal_at") val eligibleForRemovalAt: Long = System.currentTimeMillis() + (365L * 24 * 3600 * 1000)
)

@Serializable
data class PremiumCapabilities(
    @SerialName("all_feature_sync") val allFeatureSync: Boolean = true,
    @SerialName("premium_themes") val premiumThemes: Boolean = true,
    @SerialName("market_boost") val marketBoost: Boolean = true,
    @SerialName("multi_device") val multiDevice: Boolean = true,
    @SerialName("verification_eligibility") val verificationEligibility: Boolean = true,
    @SerialName("family_shared_storage") val familySharedStorage: Boolean = false
)
