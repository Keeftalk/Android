package tn.keeftalk.chat.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.repository.ChatRepository
import tn.keeftalk.chat.domain.repository.SmsRepository
import tn.keeftalk.chat.util.SoundEffectManager

data class SmsContactInfo(
    val name: String,
    val avatarUrl: String? = null
)

class SmsDetailViewModel(
    application: Application,
    private val smsRepository: SmsRepository,
    private val chatRepository: ChatRepository,
    val threadId: Long,
    val address: String
) : AndroidViewModel(application) {

    val messages: Flow<PagingData<SmsChatItem>> = smsRepository.getMessages(threadId)
        .map { pagingData ->
            pagingData.map { SmsChatItem.MessageItem(it) as SmsChatItem }
        }
        .cachedIn(viewModelScope)

    val contactInfo: StateFlow<SmsContactInfo> = flow {
        val phoneContacts = try {
            AppModule.providePhoneContactManager(getApplication()).fetchPhoneContacts()
        } catch (_: Exception) {
            emptyList()
        }

        val phoneMatch = phoneContacts.find { isSamePhoneNumber(it.phone, address) }

        val profiles = try {
            chatRepository.getSearchableProfiles().first()
        } catch (_: Exception) {
            emptyList()
        }

        val profileMatch = profiles.find { isSamePhoneNumber(it.phone, address) }

        val displayName = profileMatch?.fullName
            ?: profileMatch?.username
            ?: phoneMatch?.name
            ?: address

        val avatarUrl = profileMatch?.avatarUrl ?: phoneMatch?.avatarUrl

        emit(SmsContactInfo(name = displayName, avatarUrl = avatarUrl))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SmsContactInfo(name = address))

    fun sendMessage(body: String) {
        if (body.isBlank()) return
        SoundEffectManager.playMediaSound(getApplication(), SoundEffectManager.RES_MSG_SENDING)
        viewModelScope.launch {
            smsRepository.sendSms(address, body)
        }
    }

    fun markAsRead() {
        viewModelScope.launch {
            smsRepository.markAsRead(threadId)
        }
    }

    fun deleteConversation() {
        viewModelScope.launch {
            smsRepository.deleteConversation(threadId)
        }
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
