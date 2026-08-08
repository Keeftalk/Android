package com.keeftalk.chat.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.repository.SmsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    val contact = chatRepository.getSearchableProfiles()
        .map { profiles ->
            profiles.find { it.phone == address }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun sendMessage(body: String) {
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
}
