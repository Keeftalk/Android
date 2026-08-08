package com.keeftalk.chat.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.domain.model.Message
import com.keeftalk.chat.domain.model.MessageType
import com.keeftalk.chat.domain.model.MediaItem
import com.keeftalk.chat.domain.model.MediaAlbum
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.repository.MediaRepository
import com.keeftalk.chat.ui.screens.chatdetail.formatDateSeparatorInternal
import com.keeftalk.chat.ui.screens.chatdetail.isDifferentDay
import com.keeftalk.chat.util.VoiceRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

enum class VoiceRecordingState {
    IDLE, RECORDING, PREVIEW, SENDING
}

sealed class ChatItem {
    data class MessageItem(val message: Message) : ChatItem()
    data class DateSeparatorItem(val date: String) : ChatItem()
    data class EncryptionNoticeItem(val id: String = "e2ee") : ChatItem()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDetailViewModel(
    application: Application,
    private val repository: ChatRepository,
    private val mediaRepository: MediaRepository,
    prefs: UserPreferencesRepository,
    private val chatId: String,
) : AndroidViewModel(application) {

    private val voiceRecorder = VoiceRecorder(application)
    private var recordingJob: Job? = null

    private val _recordingState = MutableStateFlow(VoiceRecordingState.IDLE)
    val recordingState = _recordingState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration = _recordingDuration.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow<List<Float>>(emptyList())
    val amplitudeHistory = _amplitudeHistory.asStateFlow()

    private var currentVoiceFile: File? = null

    private val _transferProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val transferProgress = _transferProgress.asStateFlow()

    private val _failedMessage = MutableStateFlow<Message?>(null)
    val failedMessage = _failedMessage.asStateFlow()

    private val _expandedMessageId = MutableStateFlow<String?>(null)
    val expandedMessageId = _expandedMessageId.asStateFlow()

    val userPreferences = prefs.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.keeftalk.chat.data.prefs.UserPreferences())


    val chat = repository.getChat(chatId)
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val typingUsers = repository.getTypingStatus(chatId)
        .combine(userPreferences) { users, prefs ->
            if (prefs.typingIndicatorsEnabled) users else emptySet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val messagesPagingData: Flow<PagingData<ChatItem>> = repository.getMessagePager(chatId)
        .map { pagingData ->
            pagingData.map { ChatItem.MessageItem(it) as ChatItem }
                .insertSeparators { before, after ->
                    val b = before as? ChatItem.MessageItem
                    val a = after as? ChatItem.MessageItem
                    if (b != null && (a == null || isDifferentDay(b.message.timestamp, a.message.timestamp))) {
                        ChatItem.DateSeparatorItem(formatDateSeparatorInternal(b.message.timestamp))
                    } else null
                }
                .insertSeparators { _, after ->
                    if (after == null) {
                        ChatItem.EncryptionNoticeItem()
                    } else null
                }
        }
        .cachedIn(viewModelScope)

    private val _selectedAlbum = MutableStateFlow<MediaAlbum?>(null)
    val selectedAlbum = _selectedAlbum.asStateFlow()

    val availableAlbums: StateFlow<List<MediaAlbum>> = mediaRepository.getAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaItems: Flow<PagingData<MediaItem>> = _selectedAlbum.flatMapLatest { album ->
        mediaRepository.getMediaPagingData(album?.id)
    }.cachedIn(viewModelScope)

    private val _selectedMediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val selectedMediaList = _selectedMediaList.asStateFlow()

    private val _mediaQualityMap = MutableStateFlow<Map<Long, com.keeftalk.chat.ui.screens.chatdetail.MediaQuality>>(emptyMap())
    val mediaQualityMap = _mediaQualityMap.asStateFlow()

    val chatMembers = repository.getChatMembersFlow(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readReceipts = chatMembers.map { members ->
        members.filter { it.userId != userPreferences.value.userId }
            .associate { it.userId to (it.lastReadMessageId to (it.lastReadAt ?: 0L)) }
            .filterValues { it.first != null }
            .mapValues { it.value as Pair<String, Long> }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val lastSeenByPeerId = userPreferences.flatMapLatest { prefs ->
        repository.getLastSeenMessageId(chatId, prefs.userId)
    }.flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        com.keeftalk.chat.util.PerformanceProfiler.logEvent("ChatDetailViewModel created", category = com.keeftalk.chat.util.PerformanceProfiler.Category.DI)
        viewModelScope.launch {
            // Preload model for user's language
            repository.preloadTranslationModel(java.util.Locale.getDefault().language)
        }

        // Load members for group chat
        viewModelScope.launch {
            chat.filterNotNull().collectLatest { c ->
                if (c.type == com.keeftalk.chat.domain.model.ChatType.GROUP) {
                    repository.getChatMembers(chatId).collect { members ->
                        val map = members.associateBy { it.id }
                        _userProfiles.update { it + map }
                    }
                }
            }
        }
    }

    fun loadMore() {
        // Paging handles this automatically
    }

    fun getMessage(id: String): Flow<Message?> = repository.getMessage(id)

    fun sendMessage(content: String, replyToId: String? = null) {
        viewModelScope.launch { 
            val isMarkdownCode = content.contains("'''")
            val isLongCode = !isMarkdownCode && content.length > 500 && (
                content.contains("{") || 
                content.contains("import ") || 
                content.contains("class ") || 
                content.contains("fun ") || 
                content.contains("def ") || 
                content.contains("public ") || 
                content.contains("var ") || 
                content.contains("val ")
            )

            try {
                if (isLongCode) {
                    val tempFile = File(getApplication<Application>().cacheDir, "snippet_${System.currentTimeMillis()}.txt")
                    tempFile.writeText(content)
                    repository.sendMessage(chatId, "", MessageType.FILE, tempFile.absolutePath, replyToId = replyToId) { progress ->
                        // Message ID is not yet known here, we might need a way to track it.
                        // For simplicity, let's assume we can track it by a temp ID or similar if needed.
                    }
                } else {
                    repository.sendMessage(chatId, content, replyToId = replyToId)
                }
            } catch (e: Exception) {
                // If it failed immediately (rare for local insert)
            }
            simulatePeerReply()
        }
    }

    fun retryMessage(message: Message) {
        _failedMessage.value = null
        viewModelScope.launch {
            repository.sendMessage(chatId, message.content, message.type, message.localFilePath, message.replyToId)
        }
    }

    fun cancelMessage(message: Message) {
        _failedMessage.value = null
        viewModelScope.launch {
            repository.deleteMessage(message.id)
        }
    }

    fun dismissFailure() {
        _failedMessage.value = null
    }

    private suspend fun simulatePeerReply() {
        delay(kotlin.random.Random.nextLong(1200, 2700).milliseconds)
        
        // Simulate typing
        repository.setTypingStatus(chatId, isTyping = true)
        delay(kotlin.random.Random.nextLong(1000, 2000).milliseconds)
        repository.setTypingStatus(chatId, isTyping = false)
    }

    fun sendMedia(type: MessageType, uri: String) {
        viewModelScope.launch { repository.sendMessage(chatId, "", type, uri) }
    }

    fun setAlbum(album: MediaAlbum?) {
        _selectedAlbum.value = album
    }

    fun toggleMediaSelection(item: MediaItem) {
        val current = _selectedMediaList.value
        if (current.any { it.id == item.id }) {
            _selectedMediaList.value = current.filter { it.id != item.id }
        } else {
            if (current.size < 30) {
                _selectedMediaList.value = current + item
            }
        }
    }

    fun setMediaQuality(mediaId: Long, quality: com.keeftalk.chat.ui.screens.chatdetail.MediaQuality) {
        _mediaQualityMap.value = _mediaQualityMap.value + (mediaId to quality)
    }

    fun clearMediaSelection() {
        _selectedMediaList.value = emptyList()
        _mediaQualityMap.value = emptyMap()
    }

    fun sendMediaWithCaption(caption: String) {
        val items = _selectedMediaList.value
        if (items.isEmpty()) {
            if (caption.isNotBlank()) sendMessage(caption)
            return
        }
        
        viewModelScope.launch {
            items.forEach { item ->
                // Note: In a real app, we might pass the quality from _mediaQualityMap to the upload pipeline
                val type = if (item.isVideo) MessageType.VIDEO else MessageType.IMAGE
                repository.sendMessage(chatId, caption, type, item.uri.toString())
            }
            clearMediaSelection()
            simulatePeerReply()
        }
    }

    fun sendMediaWithCaption(item: MediaItem?, caption: String) {
        // Kept for backward compatibility if needed, but we prefer the multi-item version
        if (item == null) {
            sendMessage(caption)
            return
        }
        viewModelScope.launch {
            val type = if (item.isVideo) MessageType.VIDEO else MessageType.IMAGE
            repository.sendMessage(chatId, caption, type, item.uri.toString())
            clearMediaSelection()
            simulatePeerReply()
        }
    }

    fun sendEditedMedia(items: List<EditorModel>) {
        if (items.isEmpty()) return
        
        viewModelScope.launch {
            for (editorModel in items) {
                val mediaItem = editorModel.mediaItem
                val docItem = editorModel.documentItem
                val type = when {
                    mediaItem?.isVideo == true -> MessageType.VIDEO
                    mediaItem?.isImage == true -> MessageType.IMAGE
                    docItem?.type == DocumentType.PDF -> MessageType.PDF
                    else -> MessageType.FILE
                }
                val path = (mediaItem?.uri ?: docItem?.uri)?.toString()
                
                repository.sendMessage(
                    chatId = chatId,
                    content = editorModel.caption,
                    type = type,
                    filePath = path
                )
            }
            clearMediaSelection()
            simulatePeerReply()
        }
    }

    fun setTyping(isTyping: Boolean) {
        if (!userPreferences.value.typingIndicatorsEnabled) return
        viewModelScope.launch { repository.setTypingStatus(chatId, isTyping) }
    }

    fun startRecording() {
        if (_recordingState.value != VoiceRecordingState.IDLE) return
        
        if (voiceRecorder.start()) {
            _recordingState.value = VoiceRecordingState.RECORDING
            _recordingDuration.value = 0L
            _amplitudeHistory.value = emptyList()
            
            recordingJob = viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                while (_recordingState.value == VoiceRecordingState.RECORDING) {
                    _recordingDuration.value = System.currentTimeMillis() - startTime
                    
                    // Sample amplitude
                    val amplitude = voiceRecorder.getMaxAmplitude()
                    val normalized = (amplitude.toFloat() / 32767f).coerceIn(0f, 1f)
                    _amplitudeHistory.value = (_amplitudeHistory.value + normalized).takeLast(50)
                    
                    delay(100.milliseconds)
                }
            }
        }
    }

    fun stopRecording(autoSend: Boolean = false) {
        if (_recordingState.value != VoiceRecordingState.RECORDING) return
        
        val duration = _recordingDuration.value
        currentVoiceFile = voiceRecorder.stop()
        recordingJob?.cancel()
        
        if (duration < 300) {
            cancelRecording()
            return
        }

        if (autoSend) {
            sendVoiceMessage()
        } else {
            _recordingState.value = VoiceRecordingState.PREVIEW
        }
    }

    fun cancelRecording() {
        voiceRecorder.cancel()
        recordingJob?.cancel()
        currentVoiceFile = null
        _recordingState.value = VoiceRecordingState.IDLE
        _recordingDuration.value = 0L
        _amplitudeHistory.value = emptyList()
    }

    fun sendVoiceMessage() {
        val file = currentVoiceFile ?: return
        _recordingState.value = VoiceRecordingState.SENDING
        
        viewModelScope.launch {
            repository.sendMessage(chatId, "", MessageType.VOICE, file.absolutePath)
            resetRecordingUI()
        }
    }

    private fun resetRecordingUI() {
        recordingJob?.cancel()
        currentVoiceFile = null
        _recordingState.value = VoiceRecordingState.IDLE
        _recordingDuration.value = 0L
        _amplitudeHistory.value = emptyList()
    }

    fun markAsRead() {
        viewModelScope.launch { repository.markAsRead(chatId) }
    }

    private var lastReadSentMessageId: String? = null
    fun onMessageVisible(message: Message) {
        val currentUserId = userPreferences.value.userId
        if (message.senderId == currentUserId) return
        if (message.status == com.keeftalk.chat.domain.model.MessageStatus.SEEN) return

        if (lastReadSentMessageId != message.id) {
            lastReadSentMessageId = message.id
            viewModelScope.launch {
                repository.sendReadReceipt(chatId, message.id)
            }
        }
    }
    
    fun setAutoDelete(timer: Long?) {
        viewModelScope.launch { repository.setAutoDeleteTimer(chatId, timer) }
    }

    fun toggleAutoTranslate(enabled: Boolean) {
        viewModelScope.launch { repository.toggleAutoTranslate(chatId, enabled) }
    }

    fun toggleMute(isMuted: Boolean) {
        viewModelScope.launch { repository.toggleMuteChat(chatId, isMuted) }
    }

    fun clearChat() {
        viewModelScope.launch { repository.clearChat(chatId) }
    }

    fun deleteChat() {
        viewModelScope.launch { repository.deleteChat(chatId) }
    }

    fun updateTheme(themeId: String?) {
        viewModelScope.launch { repository.updateChatTheme(chatId, themeId) }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch { repository.deleteMessage(messageId) }
    }

    fun translateMessage(messageId: String) {
        viewModelScope.launch {
            repository.translateMessage(messageId)
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch { repository.addReaction(messageId, emoji) }
    }

    fun removeReaction(messageId: String) {
        viewModelScope.launch { repository.removeReaction(messageId) }
    }

    fun bumpChat() {
        viewModelScope.launch { repository.bumpChat(chatId) }
    }

    fun reportMessage(messageId: String, reason: String) {
        viewModelScope.launch { repository.reportMessage(messageId, reason) }
    }

    fun toggleTimestamp(messageId: String) {
        _expandedMessageId.value = if (_expandedMessageId.value == messageId) null else messageId
    }

    fun toggleMediaLock(messageId: String, locked: Boolean) {
        viewModelScope.launch { repository.toggleMediaLock(messageId, locked) }
    }

    fun downloadMedia(message: Message) {
        viewModelScope.launch {
            repository.downloadMedia(message) { progress ->
                _transferProgress.value += (message.id to progress)
            }.onSuccess { result ->
                _transferProgress.value -= message.id
                val typeName = when(message.type) {
                    MessageType.IMAGE -> "Pic"
                    MessageType.VIDEO -> "Video"
                    MessageType.PDF -> "Pdf"
                    else -> "File"
                }
                val subFolder = when(message.type) {
                    MessageType.IMAGE -> "Picture"
                    MessageType.VIDEO -> "Video"
                    else -> "files"
                }
                val msg = if (result.alreadyExisted) {
                    "$typeName already saved in Keeftalk/$subFolder"
                } else {
                    "$typeName downloaded to Keeftalk/$subFolder"
                }
                android.widget.Toast.makeText(getApplication(), msg, android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                _transferProgress.value -= message.id
                android.widget.Toast.makeText(getApplication(), e.message ?: "Failed to download", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val _userProfiles = MutableStateFlow<Map<String, com.keeftalk.chat.domain.model.User>>(emptyMap())
    val userProfiles = _userProfiles.asStateFlow()

    fun getProfile(userId: String): User? {
        val current = _userProfiles.value[userId]
        if (current == null) {
            viewModelScope.launch {
                repository.getContact(userId).first()?.let { user ->
                    _userProfiles.update { it + (userId to user) }
                }
            }
        }
        return current
    }

    // Scroll State Preservation
    var savedScrollPosition: Int = 0
    var savedScrollOffset: Int = 0

    fun loadReactionProfiles(reactions: List<com.keeftalk.chat.domain.model.MessageReaction>) {
        viewModelScope.launch {
            val missingIds = reactions.asSequence().map { it.userId }.filter { it !in _userProfiles.value }.toList()
            if (missingIds.isNotEmpty()) {
                missingIds.forEach { id ->
                    repository.getContact(id).first()?.let { user ->
                        _userProfiles.value += (id to user)
                    }
                }
            }
        }
    }
}
