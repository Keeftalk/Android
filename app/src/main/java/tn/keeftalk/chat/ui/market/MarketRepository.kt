package tn.keeftalk.chat.ui.market

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tn.keeftalk.chat.util.KeeftalkStore
import java.util.Locale

class MarketRepository {

    private val store: KeeftalkStore by lazy { KeeftalkStore.getInstance() }
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(loadOrSeedState())
    val state: StateFlow<MarketStateData> = _state.asStateFlow()

    init {
        purgeExpiredListings()
    }

    private fun loadOrSeedState(): MarketStateData {
        val raw = try { store.getString("kt_market_v2") } catch (_: Exception) { null }
        if (!raw.isNullOrEmpty()) {
            try {
                val data = json.decodeFromString<MarketStateData>(raw)
                // Filter out legacy mockup data
                data.listings.removeAll { it.id.matches(Regex("l\\d+")) || it.seller.matches(Regex("s\\d+")) }
                data.chats.keys.removeAll { it.matches(Regex("l\\d+")) }
                data.sellers.keys.removeAll { it.matches(Regex("s\\d+")) }
                return data
            } catch (e: Exception) {
                android.util.Log.e("MarketRepository", "Error parsing saved market state, preserving original raw JSON without reseeding", e)
                // DO NOT reseed or overwrite raw payload on parse failure
                return MarketStateData(me = UserProfile(alias = "me", real = "User", mode = "alias"))
            }
        }
        val seeded = createSeedState()
        persistState(seeded)
        return seeded
    }

    private fun persistState(data: MarketStateData = _state.value) {
        try {
            val encoded = json.encodeToString(data)
            store.putString("kt_market_v2", encoded)
        } catch (e: Exception) {
            android.util.Log.e("MarketRepository", "Error persisting market state", e)
        }
    }

    private fun updateState(transform: (MarketStateData) -> Unit) {
        val current = _state.value
        transform(current)
        _state.value = current
        persistState(current)
    }

    fun purgeExpiredListings() {
        val now = System.currentTimeMillis()
        val dead = _state.value.listings.filter { it.exp <= now }.map { it.id }
        if (dead.isNotEmpty()) {
            updateState { state ->
                state.listings.removeAll { dead.contains(it.id) }
                dead.forEach { state.chats.remove(it) }
                state.saved.removeAll(dead)
            }
        }
    }

    fun likeListing(id: String): Boolean {
        var isLiked = false
        val listing = _state.value.listings.firstOrNull { it.id == id } ?: return false
        updateState { state ->
            val idx = state.likes.indexOfFirst { it.id == id }
            if (idx >= 0) {
                state.likes.removeAt(idx)
                isLiked = false
            } else {
                state.likes.add(UserLike(id, listing.cat, listing.sub))
                isLiked = true
            }
        }
        return isLiked
    }

    fun toggleSaveListing(id: String): Boolean {
        var isSaved = false
        updateState { state ->
            if (state.saved.contains(id)) {
                state.saved.remove(id)
                isSaved = false
            } else {
                state.saved.add(id)
                isSaved = true
            }
        }
        return isSaved
    }

    fun hideHero() {
        updateState { state ->
            _state.value = state.copy(uiHero = 0)
        }
    }

    fun setLocation(loc: List<Double>?) {
        updateState { state ->
            state.loc = loc
        }
    }

    fun resetTaste() {
        updateState { state ->
            state.likes.clear()
        }
    }

    fun updateIdentity(realName: String, mode: String) {
        updateState { state ->
            state.me.real = realName
            if (mode.isNotEmpty()) {
                state.me.mode = mode
            }
        }
    }

    fun extendListing(id: String): Boolean {
        var extended = false
        updateState { state ->
            val listing = state.listings.firstOrNull { it.id == id }
            if (listing != null && listing.ext == 0) {
                val updatedListings = state.listings.map { item ->
                    if (item.id == id) {
                        item.copy(exp = item.exp + 30 * MarketConstants.DAY_MS, ext = 1)
                    } else item
                }.toMutableList()
                state.listings.clear()
                state.listings.addAll(updatedListings)
                extended = true
            }
        }
        return extended
    }

    fun extendListingOnQualifyingBuyerMessage(listingId: String, buyerId: String, sellerId: String, isSellerPremium: Boolean): Boolean {
        if (buyerId.isBlank() || sellerId.isBlank() || buyerId == sellerId || !isSellerPremium) {
            return false
        }
        var extended = false
        updateState { state ->
            val listing = state.listings.firstOrNull { it.id == listingId }
            if (listing != null && !listing.sold) {
                val sevenDaysMs = 7 * MarketConstants.DAY_MS
                val updatedListings = state.listings.map { item ->
                    if (item.id == listingId) {
                        item.copy(exp = item.exp + sevenDaysMs)
                    } else item
                }.toMutableList()
                state.listings.clear()
                state.listings.addAll(updatedListings)
                extended = true
            }
        }
        return extended
    }

    fun markSold(id: String): Boolean {
        var isSold = false
        updateState { state ->
            val updated = state.listings.map { item ->
                if (item.id == id) {
                    isSold = !item.sold
                    item.copy(sold = isSold)
                } else item
            }.toMutableList()
            state.listings.clear()
            state.listings.addAll(updated)
        }
        return isSold
    }

    fun deleteListing(id: String) {
        updateState { state ->
            state.listings.removeAll { it.id == id }
            state.chats.remove(id)
            state.saved.remove(id)
        }
    }

    fun sendReport(kind: String, id: String, reason: String, text: String) {
        updateState { state ->
            state.reports.add(MarketReport(kind, id, reason, text, System.currentTimeMillis()))
            if (kind == "post") {
                state.hidden.add(id)
            }
        }
    }

    fun ensureChatExists(listingId: String) {
        if (!_state.value.chats.containsKey(listingId)) {
            updateState { state ->
                val now = System.currentTimeMillis()
                val chat = MarketChat(
                    asIdentity = state.me.mode,
                    start = now,
                    msgs = mutableListOf(
                        MarketMessage(
                            f = "sys",
                            t = "🔒 Chats are end-to-end encrypted. Photos & videos on listings are not.",
                            ts = now
                        )
                    )
                )
                state.chats[listingId] = chat
            }
        }
    }

    fun sendMessage(chatId: String, text: String = "", media: MarketMedia? = null, meetup: MarketMeetup? = null) {
        ensureChatExists(chatId)
        val now = System.currentTimeMillis()
        updateState { state ->
            val chat = state.chats[chatId] ?: return@updateState
            chat.msgs.add(
                MarketMessage(
                    f = "me",
                    t = text,
                    ts = now,
                    m = media,
                    meet = meetup
                )
            )
        }

        // Trigger bot auto-reply
        triggerBotReply(chatId, text, media, meetup)
    }

    private fun triggerBotReply(chatId: String, text: String, media: MarketMedia?, meetup: MarketMeetup?) {
        scope.launch {
            delay(1100)
            updateState { state ->
                val chat = state.chats[chatId] ?: return@updateState
                val replyText = when {
                    media != null -> "Thanks for sharing! Let me know if you have questions."
                    meetup != null -> "Sounds good, see you then 👍"
                    else -> matchBotReply(text.lowercase(Locale.ROOT))
                }
                chat.msgs.add(
                    MarketMessage(
                        f = "them",
                        t = replyText,
                        ts = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun matchBotReply(input: String): String {
        return when {
            input.contains("on my way") || input.contains("arrived") -> "Great, see you in a moment 👍"
            input.contains("late") -> "No worries, I will wait a few minutes."
            input.contains("thank") || input.contains("pleasure") || input.contains("take it") -> "Great doing business with you! 🤝"
            input.contains("changed my mind") || input.contains("no longer") -> "No problem, thanks for letting me know."
            input.contains("available") || input.contains("still") -> "Yes, it is still available 👍"
            input.contains("accept") || input.contains("offer") || input.contains("can you do") || input.contains("budget") -> "I can meet you halfway if you can pick it up today."
            input.contains("price") || input.contains("negot") || input.contains("less") || input.contains("discount") -> "I can do a small discount if you can pick it up soon."
            input.contains("photo") || input.contains("video") -> "Sure, I will send more later today 📸"
            input.contains("defect") || input.contains("scratch") || input.contains("work") || input.contains("condition") || input.contains("old") || input.contains("box") || input.contains("accessor") || input.contains("why") -> "Everything works and it is as described in the listing."
            input.contains("entrance") || input.contains("exactly") -> "Got it, I am coming 👋"
            input.contains("inspect") || input.contains("public") || input.contains("café") || input.contains("mall") || input.contains("where") || input.contains("meet") || input.contains("free") || input.contains("weekend") || input.contains("morning") || input.contains("today") || input.contains("tomorrow") -> "We can meet in a public place in the city centre."
            else -> "Thanks for your message! Ask me anything about the item."
        }
    }

    fun submitReview(chatId: String, outcome: String, stars: Int, tags: List<String>, comment: String) {
        val now = System.currentTimeMillis()
        var newBadgeTitle: String? = null
        updateState { state ->
            val chat = state.chats[chatId] ?: return@updateState
            val listing = state.listings.firstOrNull { it.id == chatId } ?: return@updateState
            val sellerName = if (listing.seller == "me") {
                if (listing.asIdentity == "real") state.me.real else state.me.alias
            } else {
                val s = state.sellers[listing.seller]
                if (listing.asIdentity == "real") s?.real ?: "" else s?.alias ?: ""
            }

            val beforeLevel = MarketConstants.getLevelTitle(state.me.given)
            val sealed = SealedReview(
                n = if (chat.asIdentity == "real") state.me.real else state.me.alias,
                s = stars,
                t = comment,
                tags = tags,
                ts = now,
                deal = outcome,
                mine = 1
            )
            chat.rv = ChatReviewState(
                mine = sealed,
                out = outcome,
                done = false
            )
            state.me.given++
            chat.status = outcome
            val afterLevel = MarketConstants.getLevelTitle(state.me.given)
            if (beforeLevel != afterLevel) {
                newBadgeTitle = afterLevel
            }

            chat.msgs.add(
                MarketMessage(
                    f = "sys",
                    t = "🔐 Review sealed. Both reviews go public together once $sellerName reviews you.",
                    ts = now
                )
            )
        }

        // Trigger reveal after 1.8 seconds
        scope.launch {
            delay(1800)
            revealReview(chatId)
        }
    }

    fun revealReview(chatId: String) {
        updateState { state ->
            val chat = state.chats[chatId] ?: return@updateState
            val listing = state.listings.firstOrNull { it.id == chatId } ?: return@updateState
            val rvState = chat.rv ?: return@updateState
            if (rvState.done) return@updateState

            val mineRev = SellerReview(
                n = rvState.mine.n,
                s = rvState.mine.s,
                t = rvState.mine.t,
                ts = rvState.mine.ts,
                deal = rvState.mine.deal,
                tags = rvState.mine.tags
            )

            if (listing.seller != "me") {
                val seller = state.sellers[listing.seller]
                seller?.rev?.add(mineRev)
            }

            val theirs = MarketConstants.THEIRS_REVIEWS[rvState.out]
            if (theirs != null) {
                val sellerName = if (listing.asIdentity == "real") {
                    state.sellers[listing.seller]?.real ?: "Seller"
                } else {
                    state.sellers[listing.seller]?.alias ?: "Seller"
                }
                state.me.rev.add(
                    SellerReview(
                        n = sellerName,
                        s = theirs.first,
                        t = theirs.second.first,
                        tags = theirs.second.second,
                        ts = System.currentTimeMillis(),
                        deal = rvState.out,
                        role = "buyer"
                    )
                )
            }

            rvState.done = true
            val sellerName = if (listing.asIdentity == "real") {
                state.sellers[listing.seller]?.real ?: "Seller"
            } else {
                state.sellers[listing.seller]?.alias ?: "Seller"
            }
            val sysText = if (theirs != null) {
                "🔓 Both reviews are public — $sellerName rated you ${theirs.first}★"
            } else {
                "🔓 Your review is public — they can’t review back"
            }
            chat.msgs.add(
                MarketMessage(
                    f = "sys",
                    t = sysText,
                    ts = System.currentTimeMillis()
                )
            )
        }
    }

    fun publishListing(
        title: String,
        price: Double,
        cat: String,
        sub: String,
        cd: String,
        desc: String,
        asIdentity: String,
        shape: String,
        media: List<MarketMedia>
    ) {
        val now = System.currentTimeMillis()
        val newListing = MarketListing(
            id = "l$now",
            title = title,
            price = price,
            cat = cat,
            sub = sub,
            cd = cd,
            desc = desc,
            seller = "me",
            asIdentity = asIdentity,
            shape = shape,
            media = media,
            ts = now,
            exp = now + 30 * MarketConstants.DAY_MS,
            ext = 0,
            pos = _state.value.loc
        )
        updateState { state ->
            state.listings.add(0, newListing)
        }
    }

    private fun createSeedState(): MarketStateData {
        val myAlias = MarketConstants.generateRandomAlias()

        val me = UserProfile(
            alias = myAlias,
            real = "Your Name",
            mode = "alias",
            given = 0,
            since = 2026,
            ver = 0
        )

        val sellers = mutableMapOf(
            "me" to SellerProfile(myAlias, "Your Name", 2026, 0, mutableListOf())
        )

        return MarketStateData(
            me = me,
            uiHero = 1,
            loc = null,
            sellers = sellers,
            listings = mutableListOf(),
            chats = mutableMapOf()
        )
    }
}
