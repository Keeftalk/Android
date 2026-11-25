package tn.keeftalk.chat.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tn.keeftalk.chat.domain.model.Profile
import tn.keeftalk.chat.domain.repository.AuthRepository
import tn.keeftalk.chat.domain.repository.ChatRepository
import tn.keeftalk.chat.domain.service.CountryService
import tn.keeftalk.chat.domain.service.PhoneNumberService
import tn.keeftalk.chat.data.local.CallLogEntry
import tn.keeftalk.chat.data.local.CallLogManager
import tn.keeftalk.chat.domain.repository.SmsMessage
import tn.keeftalk.chat.domain.repository.SmsRepository
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
    private val relationshipRepository: tn.keeftalk.chat.domain.repository.RelationshipRepository,
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

    val relationship: StateFlow<tn.keeftalk.chat.domain.model.Relationship?> = 
        if (userId == null) MutableStateFlow(null).asStateFlow()
        else relationshipRepository.getCachedRelationship(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    private var currentUserId: String? = null

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

    fun onCountryChange(country: tn.keeftalk.chat.domain.model.Country) {
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

    data class PendingImage(
        val uri: String,
        val bytes: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PendingImage
            if (uri != other.uri) return false
            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            var result = uri.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    private val _pendingAvatar = MutableStateFlow<PendingImage?>(null)
    val pendingAvatar: StateFlow<PendingImage?> = _pendingAvatar.asStateFlow()

    private val _pendingCover = MutableStateFlow<PendingImage?>(null)
    val pendingCover: StateFlow<PendingImage?> = _pendingCover.asStateFlow()

    fun onAvatarSelected(uri: String, bytes: ByteArray) {
        _pendingAvatar.value = PendingImage(uri, bytes)
        val updated = (_pendingProfile.value ?: _profile.value)?.copy(avatarUrl = uri)
        updated?.let {
            _profile.value = it
            if (_isEditing.value) _pendingProfile.value = it
        }
    }

    fun onCoverSelected(uri: String, bytes: ByteArray) {
        _pendingCover.value = PendingImage(uri, bytes)
        val updated = (_pendingProfile.value ?: _profile.value)?.copy(coverUrl = uri)
        updated?.let {
            _profile.value = it
            if (_isEditing.value) _pendingProfile.value = it
        }
    }

    fun saveProfile() {
        val baseProfile = _pendingProfile.value ?: _profile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            var profileToSave = baseProfile
            val avatarState = _pendingAvatar.value
            val coverState = _pendingCover.value

            if (avatarState != null) {
                val avatarRes = authRepository.uploadAvatar(avatarState.bytes)
                if (avatarRes.isFailure) {
                    _error.value = avatarRes.exceptionOrNull()?.message ?: "Failed to upload avatar"
                    _isLoading.value = false
                    return@launch
                }
                val newAvatarUrl = avatarRes.getOrThrow()
                profileToSave = profileToSave.copy(avatarUrl = newAvatarUrl)
            }

            if (coverState != null) {
                val coverRes = authRepository.uploadCover(coverState.bytes)
                if (coverRes.isFailure) {
                    _error.value = coverRes.exceptionOrNull()?.message ?: "Failed to upload cover"
                    _isLoading.value = false
                    return@launch
                }
                val newCoverUrl = coverRes.getOrThrow()
                profileToSave = profileToSave.copy(coverUrl = newCoverUrl)
            }

            authRepository.updateProfile(profileToSave).onSuccess {
                _pendingAvatar.value = null
                _pendingCover.value = null
                _pendingProfile.value = null
                _profile.value = profileToSave
                _isEditing.value = false
                _error.value = null
            }.onFailure { e ->
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }
    
    fun setEditing(editing: Boolean) {
        if (editing) {
            _isEditing.value = true
            _pendingProfile.value = _profile.value
            _pendingAvatar.value = null
            _pendingCover.value = null
        } else {
            cancelEditing()
        }
    }

    fun cancelEditing() {
        _pendingAvatar.value = null
        _pendingCover.value = null
        _pendingProfile.value = null
        _isEditing.value = false
        loadProfile()
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
