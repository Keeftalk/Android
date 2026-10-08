package tn.keeftalk.chat.ui.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

enum class MarketTab {
    Browse, Saved, Chats, Me
}

sealed interface SheetContent {
    data class Details(val listingId: String, val mediaIndex: Int = 0) : SheetContent
    data class Seller(val listingId: String) : SheetContent
    data class Chat(val chatId: String, val focusInput: Boolean = false) : SheetContent
    data class Deal(val chatId: String) : SheetContent
    data class Review(val chatId: String, val outcome: String) : SheetContent
    data class Report(val kind: String, val id: String) : SheetContent
    data object Identity : SheetContent
    data object Post : SheetContent
    data class Location(val onAllowed: (() -> Unit)? = null) : SheetContent
    data object Gate : SheetContent
}

data class GateInfo(
    val k: Int,
    val done: Int,
    val need: List<String>
)

class MarketViewModel(
    private val repository: MarketRepository = MarketRepository()
) : ViewModel() {

    val state: StateFlow<MarketStateData> = repository.state

    private val _currentTab = MutableStateFlow(MarketTab.Browse)
    val currentTab: StateFlow<MarketTab> = _currentTab.asStateFlow()

    private val _selectedCat = MutableStateFlow("all")
    val selectedCat: StateFlow<String> = _selectedCat.asStateFlow()

    private val _selectedSub = MutableStateFlow("all")
    val selectedSub: StateFlow<String> = _selectedSub.asStateFlow()

    private val _selectedCond = MutableStateFlow("all")
    val selectedCond: StateFlow<String> = _selectedCond.asStateFlow()

    private val _selectedSort = MutableStateFlow("foryou")
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()

    private val _selectedMinR = MutableStateFlow(0.0)
    val selectedMinR: StateFlow<Double> = _selectedMinR.asStateFlow()

    private val _selectedRad = MutableStateFlow(0.0)
    val selectedRad: StateFlow<Double> = _selectedRad.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterPanelOpen = MutableStateFlow(false)
    val filterPanelOpen: StateFlow<Boolean> = _filterPanelOpen.asStateFlow()

    private val _activeSheet = MutableStateFlow<SheetContent?>(null)
    val activeSheet: StateFlow<SheetContent?> = _activeSheet.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val filteredListings: StateFlow<List<MarketListing>> = combine(
        state,
        _selectedCat,
        _selectedSub,
        _selectedCond,
        _selectedSort,
        _selectedMinR,
        _selectedRad,
        _searchQuery
    ) { params ->
        val data = params[0] as MarketStateData
        val cat = params[1] as String
        val sub = params[2] as String
        val cond = params[3] as String
        val sort = params[4] as String
        val minR = params[5] as Double
        val rad = params[6] as Double
        val query = params[7] as String

        var list = data.listings.filter { l ->
            !data.hidden.contains(l.id) &&
                    !l.sold &&
                    (cat == "all" || l.cat == cat) &&
                    (sub == "all" || l.sub == sub) &&
                    (cond == "all" || MarketConstants.getConditionType(l.cd) == cond) &&
                    (minR <= 0.0 || getSellerAvg(data, l.seller) >= minR) &&
                    (rad <= 0.0 || data.loc == null || (getListingDistance(data, l) != null && getListingDistance(data, l)!! <= rad)) &&
                    (query.isEmpty() || (l.title + l.desc).lowercase(Locale.ROOT).contains(query.lowercase(Locale.ROOT)))
        }

        list = when (sort) {
            "foryou" -> list.sortedByDescending { l -> computeTasteScore(data, l) }
            "low" -> list.sortedBy { it.price }
            "high" -> list.sortedByDescending { it.price }
            "rev" -> list.sortedByDescending { getSellerAvg(data, it.seller) }
            "near" -> list.sortedBy { getListingDistance(data, it) ?: 1e9 }
            "new" -> list.sortedByDescending { it.ts }
            else -> list.sortedByDescending { it.ts }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gateInfo: StateFlow<GateInfo> = state.map { data ->
        computeGateInfo(data)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GateInfo(0, 0, emptyList()))

    val wrapUpCount: StateFlow<Int> = state.map { data ->
        data.chats.keys.count { id ->
            val listing = data.listings.firstOrNull { it.id == id }
            listing != null && isChatPending(data.chats[id]!!)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setTab(tab: MarketTab) {
        _currentTab.value = tab
    }

    fun setCategory(cat: String) {
        _selectedCat.value = cat
        _selectedSub.value = "all"
    }

    fun setSubcategory(sub: String) {
        _selectedSub.value = sub
    }

    fun setCondition(cond: String) {
        _selectedCond.value = cond
    }

    fun setSort(sort: String) {
        _selectedSort.value = sort
        if (sort == "near" && state.value.loc == null) {
            openSheet(SheetContent.Location())
        }
    }

    fun setMinRating(minR: Double) {
        _selectedMinR.value = minR
    }

    fun setRadius(rad: Double) {
        _selectedRad.value = rad
        if (rad > 0.0 && state.value.loc == null) {
            openSheet(SheetContent.Location())
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun toggleFilterPanel() {
        _filterPanelOpen.value = !_filterPanelOpen.value
    }

    fun resetFilters() {
        _selectedSort.value = "foryou"
        _selectedMinR.value = 0.0
        _selectedRad.value = 0.0
        _selectedCond.value = "all"
        _selectedCat.value = "all"
        _selectedSub.value = "all"
        _searchQuery.value = ""
    }

    fun openSheet(sheet: SheetContent) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        val currentSheet = _activeSheet.value
        _activeSheet.value = null
        if (currentSheet is SheetContent.Chat) {
            val chat = state.value.chats[currentSheet.chatId]
            if (chat != null && isChatPending(chat)) {
                showToast("⏰ This deal is still open — wrap it up with a review")
            }
        }
    }

    fun toggleLike(listingId: String) {
        val listing = state.value.listings.firstOrNull { it.id == listingId } ?: return
        val isLiked = repository.likeListing(listingId)
        val catName = MarketConstants.CATEGORIES[listing.cat]?.name ?: listing.cat
        showToast(if (isLiked) "❤️ You'll see more $catName" else "Like removed")
    }

    fun toggleSave(listingId: String) {
        val isSaved = repository.toggleSaveListing(listingId)
        showToast(if (isSaved) "🔖 Saved until the post is deleted" else "Removed from saved")
    }

    fun hideHero() {
        repository.hideHero()
    }

    fun setLocation(loc: List<Double>?) {
        repository.setLocation(loc)
        showToast("Approximate area set")
    }

    fun resetTaste() {
        repository.resetTaste()
    }

    fun updateIdentity(realName: String, mode: String) {
        if (realName.trim().length < 2) {
            showToast("Name is too short")
            return
        }
        repository.updateIdentity(realName.trim(), mode)
        closeSheet()
        showToast("Identity updated")
    }

    fun extendListing(id: String) {
        val ok = repository.extendListing(id)
        if (ok) {
            showToast("Extended by 30 days (60-day max reached)")
            openSheet(SheetContent.Details(id))
        }
    }

    fun markSold(id: String) {
        val isSold = repository.markSold(id)
        showToast(if (isSold) "Marked as sold" else "Relisted")
        closeSheet()
    }

    fun deleteListing(id: String) {
        repository.deleteListing(id)
        closeSheet()
        showToast("Listing and chats deleted")
    }

    fun sendReport(kind: String, id: String, reason: String, text: String) {
        repository.sendReport(kind, id, reason, text)
        closeSheet()
        showToast("Report sent. Thanks for keeping Keeftalk safe.")
    }

    fun openChatForListing(listingId: String) {
        val gate = gateInfo.value
        if (!state.value.chats.containsKey(listingId) && gate.need.isNotEmpty()) {
            openSheet(SheetContent.Gate)
            return
        }
        repository.ensureChatExists(listingId)
        openSheet(SheetContent.Chat(listingId))
    }

    fun sendMessage(chatId: String, text: String, media: MarketMedia? = null, meetup: MarketMeetup? = null) {
        repository.sendMessage(chatId, text, media, meetup)
    }

    fun submitReview(chatId: String, outcome: String, stars: Int, tags: List<String>, comment: String) {
        if (stars <= 0) {
            showToast("Choose a star rating (required)")
            return
        }
        repository.submitReview(chatId, outcome, stars, tags, comment)
        showToast("Thanks — your review is sealed")
        openSheet(SheetContent.Chat(chatId))
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
        val im = media.count { it.t == "i" }
        val vd = media.count { it.t == "v" }

        if (im < 3) {
            showToast("Add at least 3 photos ($im/3)")
            return
        }
        if (vd < 1) {
            showToast("Add at least 1 video")
            return
        }
        if (title.trim().length < 3) {
            showToast("Title is required")
            return
        }
        if (price <= 0) {
            showToast("Price must be above 0")
            return
        }
        if (cat.isEmpty()) {
            showToast("Choose a category")
            return
        }
        if (sub.isEmpty()) {
            showToast("Choose a subcategory")
            return
        }
        if (cd.isEmpty()) {
            showToast("Choose the exact condition")
            return
        }
        if (desc.trim().length < 10) {
            showToast("Description is required (10+ characters)")
            return
        }

        repository.publishListing(
            title = title.trim(),
            price = price,
            cat = cat,
            sub = sub,
            cd = cd,
            desc = desc.trim(),
            asIdentity = asIdentity,
            shape = shape,
            media = media
        )

        closeSheet()
        setTab(MarketTab.Browse)
        setCategory("all")
        showToast("Your item is live 🎉")
    }

    private fun getSellerAvg(data: MarketStateData, sellerId: String): Double {
        val s = if (sellerId == "me") data.me.rev else data.sellers[sellerId]?.rev ?: emptyList()
        return if (s.isNotEmpty()) s.sumOf { it.s.toDouble() } / s.size else 0.0
    }

    private fun getListingDistance(data: MarketStateData, l: MarketListing): Double? {
        return MarketConstants.calculateDistanceKm(data.loc, l.off, l.pos)
    }

    private fun computeTasteScore(data: MarketStateData, l: MarketListing): Double {
        var c = 0
        var s = 0
        data.likes.forEach { x ->
            if (x.cat == l.cat) c++
            if (x.sub == l.sub) s++
        }
        val aff = minOf(12, c * 2 + s * 3)
        val ageDays = (System.currentTimeMillis() - l.ts) / MarketConstants.DAY_MS.toDouble()
        return aff - ageDays
    }

    private fun isChatBothSent(chat: MarketChat): Boolean {
        return chat.msgs.any { it.f == "me" } && chat.msgs.any { it.f == "them" }
    }

    private fun getMeetupTimestamp(chat: MarketChat): Long {
        val m = chat.msgs.lastOrNull { it.meet != null } ?: return 0L
        return try {
            java.time.LocalDateTime.parse(m.meet!!.whenTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    private fun getChatLevel(chat: MarketChat): Int {
        if (chat.status != null || !isChatBothSent(chat)) return 0
        val days = (System.currentTimeMillis() - chat.start) / MarketConstants.DAY_MS.toDouble()
        val mt = getMeetupTimestamp(chat)
        return when {
            (mt > 0 && mt < System.currentTimeMillis()) || days >= 3 -> 3
            days >= 1 -> 2
            else -> 1
        }
    }

    fun isChatPending(chat: MarketChat): Boolean {
        return getChatLevel(chat) > 0
    }

    private fun computeGateInfo(data: MarketStateData): GateInfo {
        val bothChats = data.chats.entries
            .filter { data.listings.any { l -> l.id == it.key } && isChatBothSent(it.value) }
            .sortedByDescending { it.value.start }
            .map { it.key }

        val k = kotlin.math.ceil(bothChats.size * 0.6).toInt()
        val h = bothChats.take(k)
        val done = h.count { data.chats[it]?.status != null }
        val need = h.filter { data.chats[it]?.status == null }
        return GateInfo(k, done, need)
    }
}
