package tn.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import tn.keeftalk.chat.ui.market.MarketListing
import tn.keeftalk.chat.ui.market.MarketMedia

@Serializable
@Entity(
    tableName = "market_listings",
    indices = [
        Index(value = ["cat"]),
        Index(value = ["seller"]),
        Index(value = ["is_sold"])
    ]
)
data class MarketListingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val price: Double,
    val cat: String,
    val sub: String,
    val cd: String,
    val desc: String,
    val seller: String,
    @ColumnInfo(name = "as_identity") val asIdentity: String,
    val shape: String = "v",
    val ts: Long,
    val exp: Long,
    val ext: Int = 0,
    @ColumnInfo(name = "media_json") val mediaJson: String = "[]",
    @ColumnInfo(name = "is_sold") val isSold: Boolean = false,
    @ColumnInfo(name = "pos_lat") val posLat: Double? = null,
    @ColumnInfo(name = "pos_lng") val posLng: Double? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "market_chats",
    indices = [
        Index(value = ["listing_id"])
    ]
)
data class MarketChatEntity(
    @PrimaryKey @ColumnInfo(name = "listing_id") val listingId: String,
    @ColumnInfo(name = "as_identity") val asIdentity: String,
    val start: Long,
    val status: String? = null,
    @ColumnInfo(name = "messages_json") val messagesJson: String = "[]",
    @ColumnInfo(name = "review_state_json") val reviewStateJson: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "market_sellers")
data class MarketSellerEntity(
    @PrimaryKey val id: String,
    val alias: String,
    val real: String,
    val since: Int,
    val ver: Int = 0,
    @ColumnInfo(name = "reviews_json") val reviewsJson: String = "[]"
)

@Serializable
@Entity(
    tableName = "market_saved",
    indices = [
        Index(value = ["listing_id"])
    ]
)
data class MarketSavedEntity(
    @PrimaryKey @ColumnInfo(name = "listing_id") val listingId: String,
    @ColumnInfo(name = "saved_at") val savedAt: Long = System.currentTimeMillis()
)
