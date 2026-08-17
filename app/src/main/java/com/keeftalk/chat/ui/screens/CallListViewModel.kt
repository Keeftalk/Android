package com.keeftalk.chat.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.service.CountryService
import com.keeftalk.chat.domain.service.PhoneNumberService
import com.keeftalk.chat.data.local.CallLogManager
import com.keeftalk.chat.data.local.PhoneContactManager
import com.keeftalk.chat.data.local.CallLogEntry
import com.keeftalk.chat.util.VcfUtils
import com.keeftalk.chat.data.local.entities.toEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DialerSuggestion(
    val userId: String? = null,
    val name: String,
    val phoneNumber: String,
    val avatarUrl: String? = null,
    val isKeeftalkUser: Boolean = false,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)

sealed class DialerDecision {
    object Idle : DialerDecision()
    data class KeeftalkCall(val profile: Profile, val isOnline: Boolean) : DialerDecision()
    data class GsmCall(val phoneNumber: String) : DialerDecision()
    data class UssdCode(val code: String) : DialerDecision()
    data class Invite(val phoneNumber: String) : DialerDecision()
}

sealed class DialerEvent {
    data class LaunchGsmCall(val phoneNumber: String) : DialerEvent()
    data class LaunchSms(val phoneNumber: String) : DialerEvent()
    data class NavigateToChat(val chatId: String) : DialerEvent()
    data class NavigateToSmsDetail(val phoneNumber: String) : DialerEvent()
    data class NavigateToCall(val callId: String, val isVideo: Boolean) : DialerEvent()
    data class OfferGsmCall(val phoneNumber: String) : DialerEvent()
    data class Error(val message: String) : DialerEvent()
}

class CallListViewModel(
    private val app: Application,
    private val repository: ChatRepository,
    private val countryService: CountryService,
    private val phoneNumberService: PhoneNumberService,
    private val callLogManager: CallLogManager,
    private val phoneContactManager: PhoneContactManager
) : AndroidViewModel(app) {

    private val _dialerInput = MutableStateFlow("")
    val dialerInput: StateFlow<String> = _dialerInput

    val currentCountry = countryService.currentRegion

    private val _searchResult = MutableStateFlow<Profile?>(null)
    val searchResult: StateFlow<Profile?> = _searchResult

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _decision = MutableStateFlow<DialerDecision>(DialerDecision.Idle)
    val decision: StateFlow<DialerDecision> = _decision

    private val _events = MutableSharedFlow<DialerEvent>()
    val events: SharedFlow<DialerEvent> = _events

    private val _isDefaultDialer = MutableStateFlow(true)
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer

    val formattedNumber: StateFlow<String> = combine(_dialerInput, currentCountry) { input, country ->
        if (input.isEmpty()) "" 
        else if (isUssdCode(input)) input
        else phoneNumberService.formatAsYouType(input, country.isoCode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val recentCalls: Flow<List<CallLogEntry>> = callLogManager.getUnifiedCallLog()
        .map { entries -> entries.take(50) }

    private val _phoneContacts = MutableStateFlow<List<User>>(emptyList())
    val contacts: Flow<List<User>> = combine(
        repository.getContacts(),
        _phoneContacts
    ) { local, phone ->
        (local + phone).distinctBy { it.phone ?: it.id }.sortedBy { it.name }
    }

    private val _suggestions = MutableStateFlow<List<DialerSuggestion>>(emptyList())
    val suggestions: StateFlow<List<DialerSuggestion>> = _suggestions

    private val lookupCache = mutableMapOf<String, Profile?>()
    private var searchJob: Job? = null
    private var t9Job: Job? = null

    init {
        checkDefaultDialer()
        combine(
            repository.getSearchableProfiles(),
            repository.getContacts(),
            callLogManager.getUnifiedCallLog(),
            _dialerInput
        ) { profiles, contacts, calls, input ->
            if (input.isEmpty()) {
                _suggestions.value = emptyList()
                return@combine
            }
            updateAdvancedSuggestions(profiles, contacts, calls, input)
        }.launchIn(viewModelScope)
    }

    fun onResume() {
        checkDefaultDialer()
        refreshPhoneContacts()
        syncCallLogsWithSupabase()
    }

    private fun refreshPhoneContacts() {
        viewModelScope.launch {
            val phoneList = phoneContactManager.fetchPhoneContacts()
            _phoneContacts.value = phoneList
        }
    }

    private fun checkDefaultDialer() {
        val packageName = app.packageName
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager = app.getSystemService(android.app.role.RoleManager::class.java)
            val isHeld = roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER) ?: false
            _isDefaultDialer.value = isHeld
        } else {
            val telecomManager = app.getSystemService(android.content.Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            val defaultPack = telecomManager.defaultDialerPackage
            _isDefaultDialer.value = defaultPack == packageName
        }
    }

    private fun updateAdvancedSuggestions(
        profiles: List<Profile>,
        contacts: List<User>,
        calls: List<CallLogEntry>,
        input: String
    ) {
        t9Job?.cancel()
        t9Job = viewModelScope.launch(Dispatchers.Default) {
            val suggestionMap = mutableMapOf<String, DialerSuggestion>()

            profiles.forEach { profile ->
                val name = profile.fullName ?: profile.username
                val phone = profile.phone ?: ""
                if (phone.contains(input) || matchT9(name, input)) {
                    suggestionMap[phone] = DialerSuggestion(
                        userId = profile.id,
                        name = name,
                        phoneNumber = phone,
                        avatarUrl = profile.avatarUrl,
                        isKeeftalkUser = true,
                        isOnline = profile.lastSeen > System.currentTimeMillis() - 60_000,
                        lastSeen = profile.lastSeen
                    )
                }
            }

            contacts.forEach { user ->
                val phone = user.phone ?: ""
                if (phone.isNotEmpty() && (phone.contains(input) || matchT9(user.name, input))) {
                    if (!suggestionMap.containsKey(phone)) {
                        suggestionMap[phone] = DialerSuggestion(
                            userId = user.id,
                            name = user.name,
                            phoneNumber = phone,
                            avatarUrl = user.avatarUrl,
                            isKeeftalkUser = user.isActive
                        )
                    }
                }
            }

            calls.forEach { entry ->
                if (entry.number.contains(input) || (entry.name?.let { matchT9(it, input) } == true)) {
                    if (!suggestionMap.containsKey(entry.number)) {
                        suggestionMap[entry.number] = DialerSuggestion(
                            userId = entry.peerId,
                            name = entry.name ?: entry.number,
                            phoneNumber = entry.number,
                            avatarUrl = entry.avatarUrl,
                            isKeeftalkUser = entry.isKeeftalk
                        )
                    }
                }
            }

            _suggestions.value = suggestionMap.values.take(10).toList()
        }
    }

    private fun matchT9(name: String, input: String): Boolean {
        if (input.isEmpty()) return false
        val words = name.lowercase().split(" ")
        return words.any { word ->
            if (word.length < input.length) return@any false
            val t9 = word.map { char ->
                when (char) {
                    'a', 'b', 'c' -> '2'
                    'd', 'e', 'f' -> '3'
                    'g', 'h', 'i' -> '4'
                    'j', 'k', 'l' -> '5'
                    'm', 'n', 'o' -> '6'
                    'p', 'q', 'r', 's' -> '7'
                    't', 'u', 'v' -> '8'
                    'w', 'x', 'y', 'z' -> '9'
                    else -> char
                }
            }.joinToString("")
            t9.contains(input)
        }
    }

    fun onDialerInput(input: String) {
        _dialerInput.value = input
        searchIfNeeded()
    }

    fun appendDigit(digit: String) {
        _dialerInput.value += digit
        searchIfNeeded()
    }

    fun backspace() {
        if (_dialerInput.value.isNotEmpty()) {
            _dialerInput.value = _dialerInput.value.dropLast(1)
        }
        searchIfNeeded()
    }

    fun clearDialer() {
        _dialerInput.value = ""
        _searchResult.value = null
        searchJob?.cancel()
        _isSearching.value = false
        _decision.value = DialerDecision.Idle
    }

    fun updateCountryCode(country: com.keeftalk.chat.domain.model.Country) {
        viewModelScope.launch {
            countryService.setAccountCountry(country)
            searchIfNeeded()
        }
    }

    private fun searchIfNeeded() {
        searchJob?.cancel()
        val currentInput = _dialerInput.value
        if (currentInput.isEmpty()) {
            _decision.value = DialerDecision.Idle
            _searchResult.value = null
            _isSearching.value = false
            return
        }

        if (isUssdCode(currentInput)) {
            _decision.value = DialerDecision.UssdCode(currentInput)
            _searchResult.value = null
            _isSearching.value = false
            return
        }

        if (currentInput.length >= 3) {
            val normalized = phoneNumberService.normalizeToE164(currentInput, currentCountry.value.isoCode)
            if (normalized == null) {
                _decision.value = DialerDecision.GsmCall(currentInput)
                return
            }
            if (lookupCache.containsKey(normalized)) {
                val profile = lookupCache[normalized]
                _searchResult.value = profile
                _isSearching.value = false
                updateDecision(normalized, profile)
                return
            }
            _isSearching.value = true
            searchJob = viewModelScope.launch {
                delay(500)
                searchUser(normalized)
            }
        } else {
            _decision.value = DialerDecision.GsmCall(currentInput)
            _searchResult.value = null
            _isSearching.value = false
        }
    }

    private suspend fun searchUser(normalizedPhone: String) {
        repository.searchUserByPhone(normalizedPhone).onSuccess { profile ->
            lookupCache[normalizedPhone] = profile
            _searchResult.value = profile
            _isSearching.value = false
            updateDecision(normalizedPhone, profile)
        }.onFailure {
            _isSearching.value = false
            _decision.value = DialerDecision.GsmCall(normalizedPhone)
        }
    }

    private fun updateDecision(phone: String, profile: Profile?) {
        if (profile != null) {
            val isOnline = profile.lastSeen > System.currentTimeMillis() - 60_000
            _decision.value = DialerDecision.KeeftalkCall(profile, isOnline)
        } else {
            _decision.value = DialerDecision.Invite(phone)
        }
    }

    private fun isUssdCode(phone: String): Boolean = (phone.startsWith("*") || phone.startsWith("#")) && phone.endsWith("#")

    fun onCallClick(isVideo: Boolean = false) {
        viewModelScope.launch {
            val currentInput = _dialerInput.value
            if (currentInput.isEmpty()) return@launch

            val isEmergency = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                app.getSystemService(android.telephony.TelephonyManager::class.java).isEmergencyNumber(currentInput)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.PhoneNumberUtils.isEmergencyNumber(currentInput)
            }

            if (isEmergency) {
                _events.emit(DialerEvent.LaunchGsmCall(currentInput))
                return@launch
            }

            startCallWithNumber(currentInput, if (isVideo) "VIDEO" else "VOICE")
        }
    }

    fun onMessageClick() {
        viewModelScope.launch {
            when (val d = _decision.value) {
                is DialerDecision.KeeftalkCall -> {
                    repository.getOrCreateOneToOneChat(d.profile.id).onSuccess {
                        _events.emit(DialerEvent.NavigateToChat(it))
                    }
                }
                is DialerDecision.GsmCall -> _events.emit(DialerEvent.NavigateToSmsDetail(d.phoneNumber))
                is DialerDecision.Invite -> _events.emit(DialerEvent.NavigateToSmsDetail(d.phoneNumber))
                is DialerDecision.Idle -> {
                    if (_dialerInput.value.isNotEmpty()) _events.emit(DialerEvent.NavigateToSmsDetail(_dialerInput.value))
                }
                else -> {}
            }
        }
    }

    fun requestDefaultDialerRole(activity: android.app.Activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(android.app.role.RoleManager::class.java)
            if (roleManager?.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER) == true) {
                val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                activity.startActivityForResult(intent, 1001)
            }
        } else {
            val intent = android.content.Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, activity.packageName)
            }
            activity.startActivity(intent)
        }
    }

    suspend fun startCallWithNumber(phone: String, type: String): Result<String> {
        if (isUssdCode(phone)) {
            _events.emit(DialerEvent.LaunchGsmCall(phone))
            return Result.success("USSD")
        }
        val normalized = phoneNumberService.normalizeToE164(phone, currentCountry.value.isoCode) 
            ?: return Result.failure(Exception("Invalid phone number"))
            
        val profile = lookupCache[normalized] ?: repository.searchUserByPhone(normalized).getOrNull()
        
        if (profile != null) {
            val isOnline = profile.lastSeen > System.currentTimeMillis() - 60_000
            if (isOnline) {
                return repository.getOrCreateOneToOneChat(profile.id).onSuccess { chatId ->
                     val callId = java.util.UUID.randomUUID().toString()
                     repository.startCall(chatId, type, callId)
                     _events.emit(DialerEvent.NavigateToCall(callId, type.uppercase() == "VIDEO"))
                }
            }
        }
        
        // Default Fallback
        _events.emit(DialerEvent.LaunchGsmCall(phone))
        return Result.success("GSM")
    }

    fun smartCall(phoneOrId: String) {
        viewModelScope.launch {
            // 1. Try resolving as peerId (UUID)
            val profileById = repository.getSearchableProfiles().first().find { it.id == phoneOrId }
            if (profileById != null) {
                val isOnline = profileById.lastSeen > System.currentTimeMillis() - 60_000
                if (isOnline) {
                    repository.getOrCreateOneToOneChat(profileById.id).onSuccess { chatId ->
                        val callId = java.util.UUID.randomUUID().toString()
                        repository.startCall(chatId, "VOICE", callId)
                        _events.emit(DialerEvent.NavigateToCall(callId, false))
                        return@launch
                    }
                }
                // Fallback to GSM using their phone number if they are offline
                profileById.phone?.let {
                    _events.emit(DialerEvent.LaunchGsmCall(it))
                    return@launch
                }
            }

            // 2. Try resolving as Phone Number
            val normalized = phoneNumberService.normalizeToE164(phoneOrId, currentCountry.value.isoCode)
            if (normalized != null) {
                val profile = repository.searchUserByPhone(normalized).getOrNull()
                if (profile != null) {
                    val isOnline = profile.lastSeen > System.currentTimeMillis() - 60_000
                    if (isOnline) {
                        repository.getOrCreateOneToOneChat(profile.id).onSuccess { chatId ->
                            val callId = java.util.UUID.randomUUID().toString()
                            repository.startCall(chatId, "VOICE", callId)
                            _events.emit(DialerEvent.NavigateToCall(callId, false))
                            return@launch
                        }
                    }
                }
                // Default fallback for normalized phone
                _events.emit(DialerEvent.LaunchGsmCall(normalized))
            } else {
                // Last resort: raw input
                _events.emit(DialerEvent.LaunchGsmCall(phoneOrId))
            }
        }
    }
    
    fun deleteCallLog(entryId: String) {
        viewModelScope.launch {
            callLogManager.deleteEntry(entryId)
        }
    }

    fun blockUser(userId: String, isBlocked: Boolean) {
        viewModelScope.launch {
            repository.blockUser(userId, isBlocked)
        }
    }

    fun importContactsFromVcf(content: String) {
        viewModelScope.launch {
            val imported = VcfUtils.parseVcf(content)
            imported.forEach { entity ->
                repository.saveLocalContact(
                    name = entity.name,
                    phone = entity.phone ?: "",
                    secondaryPhone = entity.secondaryPhone,
                    email = entity.email,
                    birthday = entity.birthday,
                    callingCard = entity.callingCard,
                    avatarUrl = entity.avatarUrl
                )
            }
            _events.emit(DialerEvent.Error("Imported ${imported.size} contacts"))
        }
    }

    suspend fun exportContactsToVcf(): String {
        val allContacts = repository.getContacts().first()
        return VcfUtils.generateVcf(allContacts.map { it.toEntity() })
    }

    fun syncContactsWithSupabase() {
        viewModelScope.launch {
            try {
                repository.syncContacts()
            } catch (e: Exception) {
                _events.emit(DialerEvent.Error("Sync failed: ${e.message}"))
            }
        }
    }

    fun syncCallLogsWithSupabase() {
        viewModelScope.launch {
            try {
                repository.syncCallLogs()
            } catch (e: Exception) {
                _events.emit(DialerEvent.Error("Sync failed: ${e.message}"))
            }
        }
    }
}
