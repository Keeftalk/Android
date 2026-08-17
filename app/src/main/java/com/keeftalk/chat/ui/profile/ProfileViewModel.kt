package com.keeftalk.chat.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.service.CountryService
import com.keeftalk.chat.domain.service.PhoneNumberService
import com.keeftalk.chat.data.local.CallLogEntry
import com.keeftalk.chat.data.local.CallLogManager
import com.keeftalk.chat.domain.repository.SmsMessage
import com.keeftalk.chat.domain.repository.SmsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private const val TAG = "KEEFTALK_PROFILE"

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val countryService: CountryService,
    private val phoneNumberService: PhoneNumberService,
    private val callLogManager: CallLogManager,
    private val smsRepository: SmsRepository,
    private val relationshipRepository: com.keeftalk.chat.domain.repository.RelationshipRepository,
    private val userId: String? = null
) : ViewModel() {

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    val callHistory: Flow<List<CallLogEntry>> = combine(profile, callLogManager.getUnifiedCallLog()) { p, logs ->
        if (p == null) emptyList()
        else logs.filter { (it.peerId == p.id) || (it.number == p.phone) }
    }

    val smsHistory: Flow<List<SmsMessage>> = profile.flatMapLatest { p ->
        if (p == null) flowOf(emptyList())
        else {
            smsRepository.getConversations().flatMapLatest { conversations ->
                val conv = conversations.find { it.address == p.phone }
                if (conv != null) smsRepository.getMessagesList(conv.threadId)
                else flowOf(emptyList())
            }
        }
    }

    private val _isEditing = MutableStateFlow(value = false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    val usernameAvailable: StateFlow<Boolean?> = _usernameAvailable.asStateFlow()

    val relationship: StateFlow<com.keeftalk.chat.domain.model.Relationship?> = 
        if (userId == null) MutableStateFlow(null).asStateFlow()
        else relationshipRepository.getCachedRelationship(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    private var currentUserId: String? = null
    private var nudgeBatchJob: Job? = null
    private var pendingNudgeCount = 0

    val currentCountry = countryService.currentRegion

    init {
        viewModelScope.launch {
            authRepository.currentUserProfile.collect {
                currentUserId = it?.id
            }
        }
        if (userId != null) {
            viewModelScope.launch {
                chatRepository.reportProfileView(userId)
                // Fetch in background to update cache
                relationshipRepository.getRelationship(userId)
            }
        }
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            if (userId == null) {
                Log.d(TAG, "SUPABASE_PROFILE_FETCH current_user")
                authRepository.currentUserProfile.collect {
                    _profile.value = it
                    _isLoading.value = false
                }
            } else {
                Log.d(TAG, "SUPABASE_PROFILE_FETCH userId=$userId")
                authRepository.getProfile(userId).onSuccess {
                    _profile.value = it
                    // Trigger relationship fetch in background
                    relationshipRepository.getRelationship(userId)
                }.onFailure {
                    Log.e(TAG, "SUPABASE_ERROR message=${it.message}")
                    _error.value = it.message
                }
                _isLoading.value = false
            }
        }
    }

    private val _pendingProfile = MutableStateFlow<Profile?>(null)

    fun updateProfileLocal(profile: Profile) {
        _profile.value = profile
        _pendingProfile.value = profile
    }

    fun onPhoneChange(value: String) {
        val iso = _profile.value?.countryCode ?: currentCountry.value.isoCode
        val formatted = phoneNumberService.formatAsYouType(value, iso)
        val updatedProfile = (_profile.value ?: _pendingProfile.value)?.copy(phone = formatted)
        updatedProfile?.let { updateProfileLocal(it) }
    }

    fun onCountryChange(country: com.keeftalk.chat.domain.model.Country) {
        val updatedProfile = (_profile.value ?: _pendingProfile.value)?.copy(
            country = country.name,
            countryCode = country.isoCode
        )
        updatedProfile?.let { 
            updateProfileLocal(it)
            // Re-format phone with new country
            onPhoneChange(it.phone ?: "")
        }
    }

    fun saveProfile() {
        val profileToSave = _pendingProfile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.updateProfile(profileToSave).onSuccess {
                _isEditing.value = false
                _pendingProfile.value = null
                _error.value = null
            }.onFailure {
                _error.value = it.message
            }
            _isLoading.value = false
        }
    }

    fun uploadAvatar(byteArray: ByteArray) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.uploadAvatar(byteArray).onSuccess { url ->
                val updatedProfile = (_profile.value ?: _pendingProfile.value)?.copy(avatarUrl = url)
                updatedProfile?.let { 
                    _profile.value = it
                    if (_isEditing.value) {
                        _pendingProfile.value = it
                    }
                    authRepository.updateProfile(it).onSuccess {
                        _error.value = null
                    }.onFailure { e ->
                        _error.value = e.message
                    }
                }
            }.onFailure {
                _error.value = it.message
            }
            _isLoading.value = false
        }
    }

    fun uploadCover(byteArray: ByteArray) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.uploadCover(byteArray).onSuccess { url ->
                val updatedProfile = (_profile.value ?: _pendingProfile.value)?.copy(coverUrl = url)
                updatedProfile?.let { 
                    _profile.value = it
                    if (_isEditing.value) {
                        _pendingProfile.value = it
                    }
                    authRepository.updateProfile(it).onSuccess {
                        _error.value = null
                    }.onFailure { e ->
                        _error.value = e.message
                    }
                }
            }.onFailure {
                _error.value = it.message
            }
            _isLoading.value = false
        }
    }
    
    fun setEditing(editing: Boolean) {
        if (editing) {
            _isEditing.value = true
            _pendingProfile.value = _profile.value
        } else {
            // Revert changes if cancelled
            _pendingProfile.value = null
            _isEditing.value = false
        }
    }

    fun cancelEditing() {
        setEditing(false)
    }

    fun checkUsername(username: String) {
        if (username.length < 3) {
            _usernameAvailable.value = null
            return
        }
        viewModelScope.launch {
            authRepository.checkUsernameAvailability(username).onSuccess {
                _usernameAvailable.value = it
            }
        }
    }

    fun onNudgeClick() {
        val targetId = userId ?: _profile.value?.id ?: return
        
        pendingNudgeCount++
        nudgeBatchJob?.cancel()
        
        nudgeBatchJob = viewModelScope.launch {
            delay(2.seconds) // Wait for 2 seconds of inactivity
            val countToUpload = pendingNudgeCount
            pendingNudgeCount = 0
            
            _isLoading.value = true
            relationshipRepository.sendNudge(targetId, countToUpload).onSuccess {
                relationshipRepository.getRelationship(targetId)
            }.onFailure {
                _error.value = "Failed to nudge user: ${it.message}"
            }
            _isLoading.value = false
        }
    }

    fun startChat(onChatCreated: (String) -> Unit) {
        val targetId = userId ?: _profile.value?.id ?: return
        Log.d(TAG, "NAV_CHAT_START → targetId=$targetId")

        if (targetId == currentUserId) {
            _error.value = "You cannot message yourself."
            return
        }

        _error.value = null
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getOrCreateOneToOneChat(targetId).onSuccess { chatId ->
                Log.d(TAG, "SUPABASE_CHAT_CREATE chatId=$chatId")
                onChatCreated(chatId)
            }.onFailure {
                Log.e(TAG, "SUPABASE_ERROR message=${it.message}")
                _error.value = it.message ?: "Failed to start chat."
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
