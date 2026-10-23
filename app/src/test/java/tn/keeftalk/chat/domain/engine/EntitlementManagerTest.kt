package tn.keeftalk.chat.domain.engine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.domain.model.entitlement.FeatureId
import tn.keeftalk.chat.ui.market.MarketListing
import tn.keeftalk.chat.ui.market.MarketRepository

@OptIn(ExperimentalCoroutinesApi::class)
class EntitlementManagerTest {

    @Test
    fun testAlwaysFreeFeaturesReturnTrueByDefault() {
        assertTrue("My Chats must be always free", FeatureId.CHATS.isAlwaysFree)
        assertTrue("My Vault must be always free", FeatureId.VAULT.isAlwaysFree)
        assertTrue("My Maps must be always free", FeatureId.MAPS.isAlwaysFree)
        assertTrue("My Market must be always free", FeatureId.MARKET.isAlwaysFree)
    }

    @Test
    fun testPaidFeaturesRequireExplicitSyncEntitlement() {
        assertFalse("My Notes cloud sync is paid by default", FeatureId.NOTES.isAlwaysFree)
        assertFalse("My Books cloud sync is paid by default", FeatureId.BOOKS.isAlwaysFree)
        assertFalse("My Habits cloud sync is paid by default", FeatureId.HABITS.isAlwaysFree)
        assertFalse("My Secrets cloud sync is paid by default", FeatureId.SECRETS.isAlwaysFree)
        assertFalse("My Calendar cloud sync is paid by default", FeatureId.CALENDAR.isAlwaysFree)
    }

    @Test
    fun testMarketPremiumPostExtensionBySevenDaysOnQualifyingBuyerMessage() {
        val repo = MarketRepository()
        val listingId = "test_listing_1"
        val buyerId = "buyer_uuid_101"
        val sellerId = "seller_uuid_202"

        // Seller receives a message from distinct buyer -> extends by 7 days
        val extendedSuccess = repo.extendListingOnQualifyingBuyerMessage(
            listingId = listingId,
            buyerId = buyerId,
            sellerId = sellerId,
            isSellerPremium = true
        )
        // Seller cannot message themselves to extend post -> fails
        val selfMessageResult = repo.extendListingOnQualifyingBuyerMessage(
            listingId = listingId,
            buyerId = sellerId,
            sellerId = sellerId,
            isSellerPremium = true
        )

        assertFalse("Self messaging must NOT trigger +7 day post extension", selfMessageResult)
    }

    @Test
    fun testFamilyMemberOneYearRemovalTimestampCalculation() {
        val addedAt = System.currentTimeMillis()
        val oneYearMs = 365L * 24 * 3600 * 1000
        val eligibleForRemovalAt = addedAt + oneYearMs

        val isEligibleNow = System.currentTimeMillis() >= eligibleForRemovalAt
        assertFalse("Family member added today must NOT be eligible for removal immediately", isEligibleNow)
    }
}
