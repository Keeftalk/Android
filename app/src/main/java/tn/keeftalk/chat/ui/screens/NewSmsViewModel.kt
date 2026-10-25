package tn.keeftalk.chat.ui.screens

import android.app.Application
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.repository.ChatRepository
import tn.keeftalk.chat.domain.repository.SmsRepository
import tn.keeftalk.chat.util.SoundEffectManager

data class SmsRecipient(
    val name: String,
    val phoneNumber: String,
    val avatarUrl: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NewSmsViewModel(
    application: Application,
    private val chatRepository: ChatRepository,
    private val smsRepository: SmsRepository,
    initialQuery: String? = null
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow(initialQuery ?: "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedRecipients = MutableStateFlow<List<SmsRecipient>>(emptyList())
    val selectedRecipients: StateFlow<List<SmsRecipient>> = _selectedRecipients.asStateFlow()

    private val _phoneContacts = MutableStateFlow<List<SmsRecipient>>(emptyList())

    init {
        loadPhoneContacts()
    }

    private fun loadPhoneContacts() {
        viewModelScope.launch {
            try {
                val phoneManager = AppModule.providePhoneContactManager(getApplication())
                val users = phoneManager.fetchPhoneContacts()
                val recipientList = users.mapNotNull { user ->
                    user.phone?.let { phone ->
                        SmsRecipient(
                            name = user.name,
                            phoneNumber = phone,
                            avatarUrl = user.avatarUrl
                        )
                    }
                }
                _phoneContacts.value = recipientList
            } catch (_: Exception) {}
        }
    }

    val searchResults: StateFlow<List<SmsRecipient>> = combine(
        _searchQuery.debounce(200),
        _phoneContacts,
        chatRepository.getSearchableProfiles(),
        _selectedRecipients
    ) { query, phoneContacts, profiles, selected ->
        val selectedPhones = selected.map { sanitizePhone(it.phoneNumber) }.toSet()

        val profileRecipients = profiles.mapNotNull { p ->
            p.phone?.let { phone ->
                SmsRecipient(
                    name = p.fullName ?: p.username,
                    phoneNumber = phone,
                    avatarUrl = p.avatarUrl
                )
            }
        }

        val allContacts = (phoneContacts + profileRecipients).distinctBy { sanitizePhone(it.phoneNumber) }

        if (query.isBlank()) {
            allContacts.filter { sanitizePhone(it.phoneNumber) !in selectedPhones }
        } else {
            allContacts.filter { contact ->
                sanitizePhone(contact.phoneNumber) !in selectedPhones &&
                (contact.name.contains(query, ignoreCase = true) ||
                 contact.phoneNumber.contains(query, ignoreCase = true))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isQueryValidNumber: StateFlow<Boolean> = _searchQuery.map { query ->
        val clean = query.trim()
        clean.matches(Regex("""^\+?[0-9]{3,15}$""")) && _selectedRecipients.value.none { sanitizePhone(it.phoneNumber) == sanitizePhone(clean) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addRecipient(recipient: SmsRecipient) {
        if (_selectedRecipients.value.none { sanitizePhone(it.phoneNumber) == sanitizePhone(recipient.phoneNumber) }) {
            _selectedRecipients.value = _selectedRecipients.value + recipient
            _searchQuery.value = ""
        }
    }

    fun addManualNumber(number: String) {
        val clean = number.trim()
        if (clean.isNotBlank()) {
            addRecipient(SmsRecipient(name = clean, phoneNumber = clean))
        }
    }

    fun removeRecipient(recipient: SmsRecipient) {
        _selectedRecipients.value = _selectedRecipients.value.filter { sanitizePhone(it.phoneNumber) != sanitizePhone(recipient.phoneNumber) }
    }

    suspend fun sendSmsToAll(messageText: String): Pair<Long, String>? {
        val recipients = _selectedRecipients.value
        if (recipients.isEmpty() || messageText.isBlank()) return null

        var firstThreadId: Long? = null
        var firstPhone: String? = null

        for (recipient in recipients) {
            val phone = recipient.phoneNumber
            smsRepository.sendSms(phone, messageText)
            SoundEffectManager.playMediaSound(getApplication(), SoundEffectManager.RES_MSG_SENDING)

            if (firstThreadId == null) {
                firstThreadId = try {
                    Telephony.Threads.getOrCreateThreadId(getApplication(), phone)
                } catch (_: Exception) { 0L }
                firstPhone = phone
            }
        }

        return if (firstThreadId != null && firstPhone != null) firstThreadId to firstPhone else null
    }

    private fun sanitizePhone(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "")
    }
}
