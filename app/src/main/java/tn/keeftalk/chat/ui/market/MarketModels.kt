package tn.keeftalk.chat.ui.market

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import kotlin.math.*

fun getCategoryIcon(catId: String): ImageVector {
    return when (catId) {
        "vehicles" -> Icons.Default.DirectionsCar
        "realestate" -> Icons.Default.Home
        "phones" -> Icons.Default.Smartphone
        "computers" -> Icons.Default.Computer
        "electronics" -> Icons.Default.Headphones
        "home" -> Icons.Default.Chair
        "appliances" -> Icons.Default.LocalLaundryService
        "garden" -> Icons.Default.Park
        "fashion" -> Icons.Default.Checkroom
        "beauty" -> Icons.Default.Face
        "baby" -> Icons.Default.ChildCare
        "sports" -> Icons.AutoMirrored.Filled.DirectionsBike
        "books" -> Icons.Default.Book
        "music" -> Icons.Default.MusicNote
        "art" -> Icons.Default.Palette
        "toys" -> Icons.Default.SportsEsports
        "pets" -> Icons.Default.Pets
        "food" -> Icons.Default.Restaurant
        "business" -> Icons.Default.Business
        "other" -> Icons.Default.Category
        else -> Icons.Default.Inventory2
    }
}

@Serializable
data class CategoryDef(
    val id: String,
    val emoji: String,
    val name: String,
    val subcategories: List<String>
)

@Serializable
data class ConditionDef(
    val key: String,
    val label: String,
    val type: String, // "n" for new, "u" for used
    val shortLabel: String
)

@Serializable
data class MarketMedia(
    val t: String, // "i" for image, "v" for video
    val src: String? = null
)

@Serializable
data class MarketListing(
    val id: String,
    val title: String,
    val price: Double,
    val cat: String,
    val sub: String,
    val cd: String, // condition key
    val desc: String,
    val seller: String, // seller id e.g. "s1", "me"
    val asIdentity: String, // "alias" or "real"
    val shape: String = "v", // "v" or "h"
    val ts: Long,
    val exp: Long,
    var ext: Int = 0,
    val off: List<Double>? = null, // [dx, dy] offset relative to user location
    val media: List<MarketMedia> = emptyList(),
    var sold: Boolean = false,
    val pos: List<Double>? = null
)

@Serializable
data class SellerReview(
    val n: String, // reviewer name
    val s: Int, // star rating 1-5
    val t: String = "", // comment
    val ts: Long,
    val deal: String = "", // "bought", "notbought", "nomeet", "noshow", "dishonest", "gone", "ghost"
    val tags: List<String> = emptyList(),
    val role: String? = null
)

@Serializable
data class SellerProfile(
    val alias: String,
    val real: String,
    val since: Int,
    val ver: Int, // 0 or 1
    val rev: MutableList<SellerReview> = mutableListOf()
)

@Serializable
data class MarketMeetup(
    val whenTime: String,
    val wherePlace: String
)

@Serializable
data class MarketMessage(
    val f: String, // "sys", "me", "them"
    val t: String = "",
    val ts: Long,
    val m: MarketMedia? = null,
    val meet: MarketMeetup? = null
)

@Serializable
data class SealedReview(
    val n: String,
    val s: Int,
    val t: String,
    val tags: List<String>,
    val ts: Long,
    val deal: String,
    val mine: Int = 1
)

@Serializable
data class ChatReviewState(
    val mine: SealedReview,
    val out: String,
    var done: Boolean = false
)

@Serializable
data class MarketChat(
    val asIdentity: String,
    val start: Long,
    var status: String? = null, // deal status if completed/closed
    var typing: Boolean = false,
    val msgs: MutableList<MarketMessage> = mutableListOf(),
    var rv: ChatReviewState? = null
)

@Serializable
data class UserProfile(
    var alias: String,
    var real: String,
    var mode: String, // "alias" or "real"
    var given: Int = 0,
    val rev: MutableList<SellerReview> = mutableListOf(),
    val since: Int = 2026,
    val ver: Int = 0
)

@Serializable
data class UserLike(
    val id: String,
    val cat: String,
    val sub: String
)

@Serializable
data class MarketReport(
    val kind: String, // "post" or "user"
    val id: String,
    val reason: String,
    val text: String,
    val ts: Long
)

@Serializable
data class MarketStateData(
    val me: UserProfile,
    val uiHero: Int = 1,
    var loc: List<Double>? = null, // [lat, lng]
    val hidden: MutableList<String> = mutableListOf(),
    val reports: MutableList<MarketReport> = mutableListOf(),
    val saved: MutableList<String> = mutableListOf(),
    val likes: MutableList<UserLike> = mutableListOf(),
    val sellers: MutableMap<String, SellerProfile> = mutableMapOf(),
    val listings: MutableList<MarketListing> = mutableListOf(),
    val chats: MutableMap<String, MarketChat> = mutableMapOf()
)

object MarketConstants {
    val TUNIS_LOCATION = listOf(36.8, 10.18)
    const val DAY_MS = 86400000L

    val CATEGORIES = mapOf(
        "vehicles" to CategoryDef("vehicles", "🚗", "Vehicles", listOf("Cars", "Motorcycles", "Trucks & vans", "Bicycles", "Boats", "Parts & accessories")),
        "realestate" to CategoryDef("realestate", "🏠", "Real estate", listOf("Apartments", "Houses", "Land", "Commercial", "Rooms & shared", "Vacation rentals")),
        "phones" to CategoryDef("phones", "📱", "Phones & tablets", listOf("Smartphones", "Tablets", "Accessories", "Smartwatches")),
        "computers" to CategoryDef("computers", "💻", "Computers", listOf("Laptops", "Desktops", "Components", "Monitors", "Printers & network")),
        "electronics" to CategoryDef("electronics", "🎧", "Electronics", listOf("TV & audio", "Cameras", "Gaming", "Drones", "Other electronics")),
        "home" to CategoryDef("home", "🛋️", "Home & furniture", listOf("Sofas & chairs", "Tables & desks", "Beds & mattresses", "Storage", "Decor & lighting", "Kitchenware")),
        "appliances" to CategoryDef("appliances", "🧺", "Appliances", listOf("Kitchen", "Laundry", "Heating & cooling", "Small appliances")),
        "garden" to CategoryDef("garden", "🌿", "Garden & tools", listOf("Power tools", "Hand tools", "Garden", "Building materials")),
        "fashion" to CategoryDef("fashion", "👕", "Fashion", listOf("Men", "Women", "Kids", "Shoes", "Bags", "Watches & jewelry")),
        "beauty" to CategoryDef("beauty", "💄", "Beauty & health", listOf("Skincare", "Makeup", "Fragrance", "Health equipment")),
        "baby" to CategoryDef("baby", "🧸", "Baby & kids", listOf("Strollers", "Baby gear", "Kids clothing", "School supplies")),
        "sports" to CategoryDef("sports", "🚲", "Sports & outdoors", listOf("Fitness", "Cycling", "Camping", "Team sports", "Water sports")),
        "books" to CategoryDef("books", "📚", "Books & education", listOf("Textbooks", "Novels", "Courses", "Stationery")),
        "music" to CategoryDef("music", "🎸", "Music & instruments", listOf("Guitars", "Keys & pianos", "Drums", "DJ & studio")),
        "art" to CategoryDef("art", "🖼️", "Art & collectibles", listOf("Paintings", "Antiques", "Coins & stamps", "Memorabilia")),
        "toys" to CategoryDef("toys", "🎮", "Toys & games", listOf("Board games", "Video games", "Toys", "Hobbies")),
        "pets" to CategoryDef("pets", "🐾", "Pet supplies", listOf("Food & care", "Cages & aquariums", "Accessories")),
        "food" to CategoryDef("food", "🌾", "Food & agriculture", listOf("Farm equipment", "Produce", "Animal feed")),
        "business" to CategoryDef("business", "🏭", "Business & industrial", listOf("Machinery", "Office equipment", "Restaurant equipment", "Wholesale lots")),
        "other" to CategoryDef("other", "📦", "Other", listOf("Other"))
    )

    val CONDITIONS = listOf(
        ConditionDef("nwt", "New with tags (NWT)", "n", "NWT"),
        ConditionDef("nwot", "New without tags (NWOT)", "n", "NWOT"),
        ConditionDef("ndef", "New with defects", "n", "Defects"),
        ConditionDef("open", "Open box", "n", "Open box"),
        ConditionDef("cert", "Certified refurbished", "u", "Refurb."),
        ConditionDef("sref", "Seller refurbished", "u", "Refurb."),
        ConditionDef("mint", "Used – Like new / Mint", "u", "Like new"),
        ConditionDef("vg", "Used – Very good", "u", "Very good"),
        ConditionDef("good", "Used – Good", "u", "Good"),
        ConditionDef("fair", "Used – Fair / Acceptable", "u", "Fair"),
        ConditionDef("parts", "For parts or not working", "u", "For parts"),
        ConditionDef("coll", "Collectible", "u", "Collectible")
    )

    val DEAL_MAP = mapOf(
        "bought" to ("Verified purchase" to "ok"),
        "notbought" to ("Met in person" to "gr"),
        "nomeet" to ("Deal cancelled" to "gr"),
        "noshow" to ("No-show" to "bad"),
        "dishonest" to ("Not as described" to "bad"),
        "gone" to ("Item unavailable" to "gr"),
        "ghost" to ("Ghosted" to "bad")
    )

    val POSITIVE_TAGS = listOf("On time", "As described", "Honest", "Polite", "Fast replies", "Fair price")
    val NEGATIVE_TAGS = listOf("No-show", "Not as described", "Dishonest", "Rude", "Always late", "Asked advance payment", "Stopped replying")

    val RATING_CAPS = mapOf(
        "noshow" to 2,
        "dishonest" to 2,
        "ghost" to 3
    )

    val THEIRS_REVIEWS = mapOf(
        "bought" to Pair(5, Pair("Showed up on time, smooth deal.", listOf("On time", "Polite"))),
        "notbought" to Pair(4, Pair("Polite, no pressure.", listOf("Polite"))),
        "nomeet" to Pair(4, Pair("Clear communication.", listOf("Fast replies"))),
        "dishonest" to Pair(4, Pair("Fine to deal with.", listOf("Polite"))),
        "gone" to Pair(4, Pair("Clear communication.", listOf("Fast replies")))
    )

    val REPORT_REASONS_POST = listOf(
        "Scam or fraud",
        "Prohibited or illegal item",
        "Misleading photos or description",
        "Duplicate or spam",
        "Wrong category",
        "Other"
    )

    val REPORT_REASONS_USER = listOf(
        "Scam or fraud",
        "Harassment or abuse",
        "Fake or stolen identity",
        "Asked for payment in advance",
        "Spam",
        "Other"
    )

    val ADJECTIVES = listOf("Calm", "Swift", "Brave", "Quiet", "Lucky", "Bright", "Gentle", "Clever", "Sunny", "Misty", "Bold", "Cosmic")
    val ANIMALS = listOf("Otter", "Falcon", "Fox", "Panda", "Lynx", "Heron", "Koala", "Gecko", "Wolf", "Dolphin", "Owl", "Tiger")

    fun generateRandomAlias(): String {
        val adj = ADJECTIVES.random()
        val ani = ANIMALS.random()
        val num = (1000..9999).random()
        return "$adj $ani #$num"
    }

    fun getCondition(key: String): ConditionDef {
        return CONDITIONS.firstOrNull { it.key == key } ?: CONDITIONS.first()
    }

    fun getConditionType(key: String): String {
        return getCondition(key).type
    }

    fun formatMoney(price: Double): String {
        val formatted = if (price % 1.0 == 0.0) {
            String.format("%,d", price.toLong())
        } else {
            String.format("%,.2f", price)
        }
        return "$formatted TND"
    }

    fun formatTimeAgo(timestamp: Long): String {
        val minutes = (System.currentTimeMillis() - timestamp) / 60000.0
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes.toInt()}m ago"
            minutes < 1440 -> "${(minutes / 60).toInt()}h ago"
            else -> "${(minutes / 1440).toInt()}d ago"
        }
    }

    fun formatPostedAgo(timestamp: Long): String {
        val days = floor((System.currentTimeMillis() - timestamp) / DAY_MS.toDouble()).toInt()
        return if (days < 1) "Posted today" else "Posted ${days}d ago"
    }

    fun daysLeft(expTimestamp: Long): Int {
        return maxOf(0, ceil((expTimestamp - System.currentTimeMillis()) / DAY_MS.toDouble()).toInt())
    }

    fun calculateTrust(seller: SellerProfile): Pair<String, String> {
        val reviews = seller.rev
        val count = reviews.size
        val avgRating = if (count > 0) reviews.sumOf { it.s.toDouble() } / count else 0.0
        val ns = reviews.count { it.deal == "noshow" }
        val dh = reviews.count { it.deal == "dishonest" }
        return when {
            ns + dh >= 2 -> "Unreliable" to "bad"
            count < 3 -> "New seller" to "wa"
            avgRating >= 4.5 && count >= 4 -> "Highly trusted" to "ok"
            avgRating >= 3.5 -> "Reliable" to "ok"
            else -> "Be careful" to "bad"
        }
    }

    fun getLevelTitle(reviewsGivenCount: Int): String {
        return when {
            reviewsGivenCount >= 5 -> "Community champion"
            reviewsGivenCount >= 3 -> "Trusted reviewer"
            reviewsGivenCount >= 1 -> "Reviewer"
            else -> "No reviews yet"
        }
    }

    fun calculateDistanceKm(userLoc: List<Double>?, listingLocOffset: List<Double>?, pos: List<Double>?): Double? {
        val target = when {
            listingLocOffset != null -> {
                val base = userLoc ?: TUNIS_LOCATION
                val lat = base[0] + listingLocOffset[1] / 111.0
                val lng = base[1] + listingLocOffset[0] / (111.0 * cos(base[0] * Math.PI / 180.0))
                listOf(lat, lng)
            }
            pos != null -> pos
            else -> null
        } ?: return null

        val user = userLoc ?: return null
        val r = { x: Double -> x * Math.PI / 180.0 }
        val dLat = r(target[0] - user[0])
        val dLng = r(target[1] - user[1])
        val a = sin(dLat / 2).pow(2) + cos(r(user[0])) * cos(r(target[0])) * sin(dLng / 2).pow(2)
        return 2.0 * 6371.0 * asin(sqrt(a))
    }
}
