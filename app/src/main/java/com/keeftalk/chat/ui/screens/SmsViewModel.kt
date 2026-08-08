package com.keeftalk.chat.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.repository.SmsConversation
import com.keeftalk.chat.domain.repository.SmsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SmsViewModel(
    application: Application,
    private val smsRepository: SmsRepository,
    private val chatRepository: ChatRepository
) : AndroidViewModel(application) {

    private val _isDefaultSmsApp = MutableStateFlow(false)
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived

    val conversations: StateFlow<List<SmsConversation>> = combine(
        smsRepository.getConversations(),
        chatRepository.getSearchableProfiles(),
        _showArchived
    ) { smsList, profiles, archived ->
        smsList.filter { it.isArchived == archived }.map { sms ->
            val profile = profiles.find { it.phone == sms.address || it.id == sms.recipientId }
            sms.copy(
                contactName = profile?.fullName ?: profile?.username ?: sms.address,
                isKeeftalkUser = profile != null,
                avatarUrl = profile?.avatarUrl
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkDefaultSmsApp()
    }

    fun onResume() {
        checkDefaultSmsApp()
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
}
