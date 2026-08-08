package com.keeftalk.chat.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.domain.model.calendar.*
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.domain.repository.CalendarRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

data class CalendarUiState(
    val items: List<CalendarItem> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val categories: List<CalendarCategory> = emptyList(),
    val familyMembers: List<FamilyMember> = emptyList(),
    val currentView: CalendarViewType = CalendarViewType.MONTH,
    val selectedDate: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val showShareModal: Boolean = false,
    val selectedItemForSharing: CalendarItem? = null,
    val filterType: CalendarItemType? = null,
    val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.WEEK,
    val currentUserProfile: com.keeftalk.chat.domain.model.Profile? = null,
    val filterShared: Boolean = false
)

enum class AnalyticsPeriod {
    WEEK, MONTH, YEAR
}

enum class CalendarViewType {
    MONTH, WEEK, DAY, AGENDA, TIMELINE
}

class CalendarViewModel(
    private val repository: CalendarRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _shareSearchQuery = MutableStateFlow("")
    val shareSearchQuery: StateFlow<String> = _shareSearchQuery.asStateFlow()

    private val _remoteSuggestions = MutableStateFlow<List<User>>(emptyList())

    val contacts: StateFlow<List<User>> = chatRepository.getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shareSuggestions: StateFlow<List<User>> = combine(
        contacts, 
        chatRepository.allChats,
        _remoteSuggestions,
        _shareSearchQuery
    ) { contacts, chats, remote, query ->
        if (query.isEmpty()) return@combine emptyList()
        
        // 1. Identify users from 1:1 chats
        val chatUsers = chats.filter { it.type == com.keeftalk.chat.domain.model.ChatType.ONE_TO_ONE && it.peerId != null }
            .map { chat ->
                val existingContact = contacts.find { it.id == chat.peerId }
                User(
                    id = chat.peerId!!,
                    name = chat.name ?: existingContact?.name ?: "Unknown",
                    username = existingContact?.username ?: chat.latestAuthorName ?: chat.name ?: "user_${chat.peerId.take(5)}",
                    avatarUrl = chat.avatarUrl ?: existingContact?.avatarUrl,
                    isActive = true,
                    lastSeen = chat.lastTimestamp,
                    isContact = existingContact != null
                )
            }
        
        // 2. Merge with contacts and remote results (unique by ID)
        val allAvailable = (contacts + chatUsers + remote).distinctBy { it.id }

        // 3. Filter by query
        val filtered = allAvailable.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.username.contains(query, ignoreCase = true) 
        }

        // 4. Prioritize those who have an existing chat
        val chatUserIds = chats.filter { it.type == com.keeftalk.chat.domain.model.ChatType.ONE_TO_ONE }
            .mapNotNull { it.peerId }.toSet()

        filtered.sortedByDescending { it.id in chatUserIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadData()
        observeRealtime()
        syncCalendar()
        setupSearchDebounce()
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        viewModelScope.launch {
            _shareSearchQuery
                .debounce(300.milliseconds)
                .filter { it.length >= 2 }
                .collectLatest { query ->
                    searchUsersRemotely(query)
                }
        }
    }

    private suspend fun searchUsersRemotely(query: String) {
        val result = chatRepository.searchUsers(query)
        result.onSuccess { profiles ->
            val users = profiles.map { profile ->
                User(
                    id = profile.id,
                    name = profile.fullName ?: profile.username,
                    username = profile.username,
                    avatarUrl = profile.avatarUrl,
                    isActive = false,
                    lastSeen = 0L,
                    isContact = false
                )
            }
            _remoteSuggestions.value = users
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getAllItems().collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
        viewModelScope.launch {
            repository.getAllHabits().collect { habits ->
                _uiState.update { it.copy(habits = habits) }
            }
        }
        viewModelScope.launch {
            repository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            repository.getAllFamilyMembers().collect { family ->
                _uiState.update { it.copy(familyMembers = family) }
            }
        }
        viewModelScope.launch {
            authRepository.currentUserProfile.collect { profile ->
                _uiState.update { it.copy(currentUserProfile = profile) }
            }
        }
    }

    fun setFilterType(type: CalendarItemType?) {
        _uiState.update { it.copy(filterType = type) }
    }

    fun setFilterShared(shared: Boolean) {
        _uiState.update { it.copy(filterShared = shared) }
    }

    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        _uiState.update { it.copy(analyticsPeriod = period) }
    }

    fun toggleFamilyMember(id: String, included: Boolean) {
        viewModelScope.launch {
            repository.updateFamilyMemberIncluded(id, included)
        }
    }

    fun toggleFamilyPermission(memberId: String, type: CalendarItemType, level: PermissionLevel) {
        viewModelScope.launch {
            repository.updateFamilyPermission(memberId, type, level)
        }
    }

    private fun observeRealtime() {
        viewModelScope.launch {
            repository.observeCalendarRealtime().collect {
                // repository already syncs internally
            }
        }
    }

    fun setViewType(viewType: CalendarViewType) {
        _uiState.update { it.copy(currentView = viewType) }
    }

    fun setSelectedDate(date: Long) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setShareSearchQuery(query: String) {
        _shareSearchQuery.value = query
    }

    fun setShowShareModal(show: Boolean, item: CalendarItem? = null) {
        _uiState.update { it.copy(showShareModal = show, selectedItemForSharing = item) }
        if (!show) _shareSearchQuery.value = ""
    }

    fun saveItem(item: CalendarItem) {
        viewModelScope.launch {
            repository.saveItem(item)
        }
    }

    fun deleteItem(item: CalendarItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun addAttendee(itemId: String, userId: String, role: AttendeeRole) {
        viewModelScope.launch {
            repository.addAttendee(itemId, userId, role)
            // Refresh item to show new attendee if necessary, 
            // but realtime observation should handle it if implemented.
        }
    }

    fun removeAttendee(itemId: String, attendeeId: String) {
        viewModelScope.launch {
            repository.removeAttendee(itemId, attendeeId)
        }
    }

    fun updateAttendeeRole(attendeeId: String, role: AttendeeRole) {
        viewModelScope.launch {
            repository.updateAttendeeRole(attendeeId, role)
        }
    }

    fun saveHabit(habit: Habit) {
        viewModelScope.launch {
            repository.saveHabit(habit)
        }
    }

    fun saveHabitLog(log: HabitLog) {
        viewModelScope.launch {
            repository.saveHabitLog(log)
        }
    }

    fun saveCategory(category: CalendarCategory) {
        viewModelScope.launch {
            repository.saveCategory(category)
        }
    }

    fun sendInvitation(usernameOrEmail: String, kind: String, initialPermissions: List<CalendarItemType>) {
        viewModelScope.launch {
            repository.sendInvitation(usernameOrEmail, kind, initialPermissions)
        }
    }

    fun syncCalendar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.syncCalendar()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
