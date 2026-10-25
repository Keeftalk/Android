package tn.keeftalk.chat.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.User
import tn.keeftalk.chat.domain.repository.ChatRepository
import tn.keeftalk.chat.domain.repository.SmsConversation
import tn.keeftalk.chat.domain.repository.SmsRepository

class SmsViewModel(
    application: Application,
    private val smsRepository: SmsRepository,
    private val chatRepository: ChatRepository
) : AndroidViewModel(application) {

    private val _isDefaultSmsApp = MutableStateFlow(false)
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived

    private val _phoneContacts = MutableStateFlow<List<User>>(emptyList())

    val conversations: StateFlow<List<SmsConversation>> = combine(
        smsRepository.getConversations(),
        chatRepository.getSearchableProfiles(),
        _phoneContacts,
        _showArchived
    ) { smsList, profiles, phoneContacts, archived ->
        val profileMap = mutableMapOf<String, tn.keeftalk.chat.domain.model.Profile>()
        for (p in profiles) {
            val key = p.phone?.filter { it.isDigit() }?.takeLast(7)
            if (!key.isNullOrBlank()) profileMap[key] = p
        }
        val contactMap = mutableMapOf<String, User>()
        for (c in phoneContacts) {
            val key = c.phone?.filter { it.isDigit() }?.takeLast(7)
            if (!key.isNullOrBlank()) contactMap[key] = c
        }

        smsList.filter { it.isArchived == archived }.map { sms ->
            val digits = sms.address.filter { it.isDigit() }.takeLast(7)
            val profile = if (digits.isNotBlank()) profileMap[digits] else profiles.find { it.id == sms.recipientId }
            val phoneContact = if (digits.isNotBlank()) contactMap[digits] else null

            val displayName = profile?.fullName ?: profile?.username ?: phoneContact?.name ?: sms.address
            val avatar = profile?.avatarUrl ?: phoneContact?.avatarUrl
            sms.copy(
                contactName = displayName,
                isKeeftalkUser = profile != null,
                avatarUrl = avatar
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkDefaultSmsApp()
        refresh()
    }

    fun onResume() {
        checkDefaultSmsApp()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _phoneContacts.value = AppModule.providePhoneContactManager(getApplication()).fetchPhoneContacts()
                AppModule.provideTelephonySyncManager(getApplication()).syncThreads()
            } catch (_: Exception) {}
        }
    }

    private fun checkDefaultSmsApp() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager = getApplication<Application>().getSystemService(android.app.role.RoleManager::class.java)
            _isDefaultSmsApp.value = roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_SMS) ?: false
        } else {
            val defaultSmsApp = android.provider.Telephony.Sms.getDefaultSmsPackage(getApplication())
            _isDefaultSmsApp.value = defaultSmsApp == getApplication<Application>().packageName
        }
    }

    fun requestDefaultSmsRole(activity: android.app.Activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(android.app.role.RoleManager::class.java)
            val intent = roleManager?.createRequestRoleIntent(android.app.role.RoleManager.ROLE_SMS)
            if (intent != null) {
                activity.startActivityForResult(intent, 1002)
            }
        } else {
            val intent = android.content.Intent(android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.packageName)
            }
            activity.startActivity(intent)
        }
    }

    fun deleteConversation(threadId: Long) {
        viewModelScope.launch {
            smsRepository.deleteConversation(threadId)
        }
    }

    fun toggleArchive(threadId: Long, archived: Boolean) {
        viewModelScope.launch {
            smsRepository.toggleArchive(threadId, archived)
        }
    }

    fun markAsRead(threadId: Long) {
        viewModelScope.launch {
            smsRepository.markAsRead(threadId)
        }
    }

    fun setShowArchived(show: Boolean) {
        _showArchived.value = show
    }

    private fun isSamePhoneNumber(p1: String?, p2: String?): Boolean {
        if (p1.isNullOrBlank() || p2.isNullOrBlank()) return false
        val clean1 = p1.replace(Regex("[^0-9+]"), "")
        val clean2 = p2.replace(Regex("[^0-9+]"), "")
        if (clean1 == clean2) return true
        val digits1 = clean1.replace(Regex("[^0-9]"), "")
        val digits2 = clean2.replace(Regex("[^0-9]"), "")
        if (digits1.length >= 7 && digits2.length >= 7) {
            return digits1.takeLast(9) == digits2.takeLast(9) || digits1.takeLast(7) == digits2.takeLast(7)
        }
        return false
    }
}
