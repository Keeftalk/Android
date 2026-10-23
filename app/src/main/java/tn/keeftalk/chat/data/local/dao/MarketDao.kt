package tn.keeftalk.chat.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.MarketChatEntity
import tn.keeftalk.chat.data.local.entities.MarketListingEntity
import tn.keeftalk.chat.data.local.entities.MarketSavedEntity
import tn.keeftalk.chat.data.local.entities.MarketSellerEntity

@Dao
interface MarketDao {
    @Query("SELECT * FROM market_listings ORDER BY ts DESC")
    fun getAllListingsFlow(): Flow<List<MarketListingEntity>>

    @Query("SELECT * FROM market_listings ORDER BY ts DESC")
    suspend fun getAllListingsOnce(): List<MarketListingEntity>

    @Query("SELECT * FROM market_listings WHERE id = :id LIMIT 1")
    suspend fun getListingById(id: String): MarketListingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: MarketListingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListings(listings: List<MarketListingEntity>)

    @Query("DELETE FROM market_listings WHERE id = :id")
    suspend fun deleteListing(id: String)

    @Query("SELECT * FROM market_chats")
    fun getAllChatsFlow(): Flow<List<MarketChatEntity>>

    @Query("SELECT * FROM market_chats")
    suspend fun getAllChatsOnce(): List<MarketChatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: MarketChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<MarketChatEntity>)

    @Query("DELETE FROM market_chats WHERE listing_id = :listingId")
    suspend fun deleteChat(listingId: String)

    @Query("SELECT * FROM market_sellers")
    suspend fun getAllSellersOnce(): List<MarketSellerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSellers(sellers: List<MarketSellerEntity>)

    @Query("SELECT * FROM market_saved")
    fun getSavedListingsFlow(): Flow<List<MarketSavedEntity>>

    @Query("SELECT * FROM market_saved")
    suspend fun getSavedListingsOnce(): List<MarketSavedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveListing(saved: MarketSavedEntity)

    @Query("DELETE FROM market_saved WHERE listing_id = :listingId")
    suspend fun deleteSavedListing(listingId: String)
}
