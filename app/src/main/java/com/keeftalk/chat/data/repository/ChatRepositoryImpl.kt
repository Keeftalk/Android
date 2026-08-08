package com.keeftalk.chat.data.repository

import android.content.Context
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.room.withTransaction
import com.keeftalk.chat.data.local.MessageRemoteMediator
import com.keeftalk.chat.data.local.dao.ChatDao
import com.keeftalk.chat.data.local.dao.ChatListCacheDao
import com.keeftalk.chat.data.local.dao.MessageDao
import com.keeftalk.chat.data.local.dao.MessageWithReactions
import com.keeftalk.chat.data.local.dao.NotificationDao
import com.keeftalk.chat.data.local.dao.ProfileDao
import com.keeftalk.chat.data.local.dao.ReceiptSyncQueueDao
import com.keeftalk.chat.data.local.dao.UserDao
import com.keeftalk.chat.data.local.dao.CallLogDao
import com.keeftalk.chat.data.local.entities.*
import com.keeftalk.chat.data.remote.*
import com.keeftalk.chat.data.remote.toDto
import com.keeftalk.chat.data.local.entities.toDomain
import com.keeftalk.chat.data.local.entities.toEntity
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.data.local.converters.KeeftalkConverters
import com.keeftalk.chat.security.crypto.*
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.repository.DownloadResult
import com.keeftalk.chat.domain.repository.CallSession
import com.keeftalk.chat.domain.repository.CallSignaling
import com.keeftalk.chat.util.PerformanceProfiler
import com.keeftalk.chat.util.TimestampSerializer
import com.keeftalk.chat.util.TranslationManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.*
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.*
import java.util.*
import java.io.File
import java.io.FileOutputStream
import android.os.Environment
import android.net.Uri
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import io.github.jan.supabase.postgrest.query.Order
import androidx.core.net.toUri
import androidx.paging.map

class ChatRepositoryImpl(
    private val context: Context,
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val profileDao: ProfileDao,
    private val notificationDao: NotificationDao,
    private val chatListCacheDao: ChatListCacheDao,
    private val receiptSyncQueueDao: ReceiptSyncQueueDao,
    private val callLogDao: com.keeftalk.chat.data.local.dao.CallLogDao,
    private val fileDao: com.keeftalk.chat.data.local.dao.FileDao,
    private val fileUploadManager: com.keeftalk.chat.util.FileUploadManager,
    private val userPrefsRepo: UserPreferencesRepository,
    private val databaseWriteDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChatRepository {

    private val decryptionManager get() = AppModule.provideMessageDecryptionManager(context)
    private val cryptoManager get() = AppModule.provideCryptoManager(context)
    private val mediaEncryptionManager get() = AppModule.provideMediaEncryptionManager(context)

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val incomingMessagesChannel = Channel<Pair<MessageDto, String>>(Channel.UNLIMITED)
    private val pendingStatusUpdates = Channel<Pair<String, MessageStatus>>(Channel.UNLIMITED)
    private val typingUsers = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    
    private var messagesChannel: RealtimeChannel? = null

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    private suspend fun getAdminSupabase(): SupabaseClient {
        return AppModule.provideAdminSupabaseClient()
    }

    private suspend fun getTranslationManager(): TranslationManager {
        return AppModule.provideTranslationManager()
    }

    private suspend fun getCurrentUserId(): String? = try { 
        getSupabase().auth.currentUserOrNull()?.id 
    } catch(e: Exception) { 
        Log.e(TAG, "Failed to get current user ID", e)
        null 
    }

    init {
        startMessageBatcher()
        startStatusUpdateProcessor()
        observeDecryptionEvents()
    }

    override val allChats: StateFlow<List<Chat>> = chatDao.getAllChats()
        .map { entities -> entities.map { it.toDomainInternal() } }
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    override fun startBackgroundTasks() {
        repositoryScope.launch {
            ensureBucketExists()
            observeRealtimeChanges()
            observeTypingStatus()
            flushReceiptSyncQueue() // Initial flush

            // Maintenance: Fix snippets for existing chats
            // OPTIMIZATION: Move to low-priority background task with LIMIT to avoid startup lag
            try {
                withContext(databaseWriteDispatcher) {
                    val db = AppModule.provideDatabase(context)
                    
                    // Only fix a small batch at a time to prevent blocking the DB
                    db.openHelper.writableDatabase.execSQL(
                        """
                        UPDATE chats 
                        SET lastMessage = CASE 
                                WHEN snippetType = 'IMAGE' THEN 'Sent a picture'
                                WHEN snippetType = 'VIDEO' THEN 'Sent a video'
                                WHEN snippetType = 'VOICE' THEN 'Sent a voice message'
                                WHEN snippetType = 'FILE' THEN 'Sent a document'
                                ELSE lastMessage 
                            END
                        WHERE id IN (
                            SELECT id FROM chats 
                            WHERE lastMessage LIKE '[%]' AND snippetType IS NOT NULL
                            LIMIT 50
                        )
                        """.trimIndent()
                    )

                    // Optimization: Only run heavy snippet repair if really needed
                    db.openHelper.writableDatabase.execSQL(
                        """
                        UPDATE chats 
                        SET lastMessage = (
                            SELECT content FROM messages 
                            WHERE chatId = chats.id 
                            AND decryptionState = 'SUCCESS'
                            AND content != ''
                            ORDER BY timestamp DESC LIMIT 1
                        ),
                        lastDecryptedMessage = (
                            SELECT content FROM messages 
                            WHERE chatId = chats.id 
                            AND decryptionState = 'SUCCESS'
                            AND content != ''
                            ORDER BY timestamp DESC LIMIT 1
                        )
                        WHERE id IN (
                            SELECT id FROM chats
                            WHERE (lastMessage LIKE '[Encrypted%' 
                               OR lastMessage LIKE '[Unable to decrypt%' 
                               OR lastMessage LIKE '[Decryption Pending%'
                               OR lastMessage = 'New message'
                               OR lastDecryptedMessage IS NULL)
                            LIMIT 20
                        );
                        """.trimIndent()
                    )
                }

                val initialChats = chatDao.getActiveChatsOnce(15, 0)
                if (initialChats.isNotEmpty()) {
                    updateChatListCache(initialChats)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Initial cache hydration/maintenance failed", e)
            }
        }
    }

    private fun observeRealtimeChanges() {
        repositoryScope.launch {
            try {
                Log.i("CHAT_PIPELINE", "REALTIME_SUBSCRIBE_START")
                val userId = getCurrentUserId() ?: run {
                    Log.w("CHAT_PIPELINE", "REALTIME_SUBSCRIBE_FAILURE | reason=no_user")
                    return@launch
                }
                val supabase = getSupabase()
                Log.d(TAG, "observeRealtimeChanges: Starting subscription for user $userId")

                val mChannel = supabase.realtime.channel("public:messages")

                mChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                }.onEach { action ->
                    try {
                        val dto = action.decodeRecord<MessageDto>()
                        Log.i("CHAT_PIPELINE", "REALTIME_INSERT_RECEIVED | id=${dto.id} | sender=${dto.senderId}")
                        handleIncomingMessage(dto, "REALTIME")
                    } catch (e: Exception) {
                        Log.e("CHAT_PIPELINE", "REALTIME_INSERT_REJECTED | error=${e.message}")
                    }
                }.launchIn(this)

                mChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "messages"
                }.onEach { action ->
                    try {
                        val dto = action.decodeRecord<MessageDto>()
                        Log.i("CHAT_PIPELINE", "REALTIME_UPDATE_RECEIVED | id=${dto.id} | status=${dto.status}")
                        handleIncomingMessage(dto, "REALTIME")
                    } catch (e: Exception) {
                        Log.e("CHAT_PIPELINE", "REALTIME_UPDATE_REJECTED | error=${e.message}")
                    }
                }.launchIn(this)

                mChannel.broadcastFlow<TypingBroadcast>("typing").onEach { broadcast ->
                    val current = typingUsers.value.toMutableMap()
                    val chatTyping = current[broadcast.chatId]?.toMutableSet() ?: mutableSetOf()
                    if (broadcast.isTyping) {
                        chatTyping.add(broadcast.userId)
                    } else {
                        chatTyping.remove(broadcast.userId)
                    }
                    current[broadcast.chatId] = chatTyping
                    typingUsers.value = current
                }.launchIn(this)

                mChannel.status.onEach { status ->
                    Log.d(TAG, "Supabase Realtime Status: $status")
                    if (status == RealtimeChannel.Status.SUBSCRIBED) {
                        Log.i("CHAT_PIPELINE", "REALTIME_SUBSCRIBE_SUCCESS")
                    } else {
                        Log.d("CHAT_PIPELINE", "REALTIME_STATUS_CHANGED | status=$status")
                    }
                }.launchIn(this)

                mChannel.subscribe()
                messagesChannel = mChannel

                // Observe chat members for Read/Delivery receipts (Pointers)
                val membersChannel = supabase.realtime.channel("public:chat_members")
                membersChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "chat_members"
                }.onEach { action ->
                    try {
                        val chatId = action.record["chat_id"]?.jsonPrimitive?.content ?: return@onEach
                        val userId = action.record["user_id"]?.jsonPrimitive?.content ?: return@onEach
                        
                        val readId = action.record["last_read_message_id"]?.jsonPrimitive?.contentOrNull
                        val deliveredId = action.record["last_delivered_message_id"]?.jsonPrimitive?.contentOrNull
                        val readAt = action.record["last_read_at"]?.jsonPrimitive?.contentOrNull?.let { TimestampSerializer.parseTimestamp(it) }
                        val deliveredAt = action.record["last_delivered_at"]?.jsonPrimitive?.contentOrNull?.let { TimestampSerializer.parseTimestamp(it) }

                        if (readId != null) {
                            chatDao.updateReadPointer(chatId, userId, readId, readAt ?: 0L)
                        }
                        if (deliveredId != null) {
                            chatDao.updateDeliveryPointer(chatId, userId, deliveredId, deliveredAt ?: 0L)
                        }
                        
                        Log.d("CHAT_PIPELINE", "REALTIME_RECEIPT_RECEIVED | chat=$chatId | user=$userId | read=$readId")
                    } catch (e: Exception) {
                        Log.e("CHAT_PIPELINE", "REALTIME_RECEIPT_ERROR | error=${e.message}")
                    }
                }.launchIn(this)
                membersChannel.subscribe()

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("CHAT_PIPELINE", "REALTIME_SUBSCRIBE_FAILURE | error=${e.message}")
            }
        }
    }

    private fun observeTypingStatus() {}

    private fun startStatusUpdateProcessor() {
        repositoryScope.launch {
            for ((messageId, status) in pendingStatusUpdates) {
                try {
                    getSupabase().postgrest["messages"].update(
                        buildJsonObject {
                            put("status", status.name)
                        }
                    ) {
                        filter { eq("id", messageId) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update status remotely", e)
                }
            }
        }
    }

    private fun getStatusPriority(status: String): Int = when (status.uppercase()) {
        "SENDING" -> 0
        "SENT" -> 1
        "DELIVERED" -> 2
        "FCM_RECEIVED" -> 3
        "SEEN" -> 4
        else -> -1
    }

    private fun startMessageBatcher() {
        repositoryScope.launch(databaseWriteDispatcher) {
            val batch = mutableListOf<Pair<MessageDto, String>>()
            while (isActive) {
                try {
                    val message = withTimeoutOrNull(100.milliseconds) {
                        incomingMessagesChannel.receive()
                    }
                    if (message != null) {
                        batch.add(message)
                        while (batch.size < 30) {
                            val next = incomingMessagesChannel.tryReceive().getOrNull() ?: break
                            batch.add(next)
                        }
                    }
                    if (batch.isNotEmpty()) {
                        processMessageBatch(batch)
                        batch.clear()
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Log.e(TAG, "Error in message batcher: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun processMessageBatch(batch: List<Pair<MessageDto, String>>) {
        PerformanceProfiler.startStage("DB: Process Message Batch")
        val db = AppModule.provideDatabase(context)
        
        // Log counts per source for transparency
        batch.groupBy { it.second }.forEach { (source, items) ->
            Log.d(TAG, "[$source] Processing batch of ${items.size} messages")
        }

        // --- STEP 1: DECRYPT OUTSIDE TRANSACTION ---
        val processedBatch = mutableListOf<DecryptedMessageBatchItem>()
        batch.forEach { (dto, source) ->
            processedBatch.add(decryptBatchItem(dto, source))
        }

        // --- STEP 2: FAST DB UPSERT ---
        db.withTransaction {
            processedBatch.forEach { item ->
                handleIncomingMessageInternalDecrypted(item)
            }
        }
        
        repositoryScope.launch(Dispatchers.IO) {
            try {
                val initialChats = chatDao.getActiveChatsOnce(15, 0)
                if (initialChats.isNotEmpty()) {
                    updateChatListCache(initialChats)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh FastPath after batch", e)
            }
        }
        PerformanceProfiler.endStage("DB: Process Message Batch", "Count: ${batch.size}")
    }

    private suspend fun decryptBatchItem(dto: MessageDto, source: String): DecryptedMessageBatchItem {
        // --- ROOM-FIRST OPTIMIZATION ---
        val existing = messageDao.getMessageById(dto.id)
        if (existing != null && existing.decryptionState == DecryptionState.SUCCESS) {
            return DecryptedMessageBatchItem(dto, source, existing.content, existing.decryptionState, null, null)
        }

        var decryptedContent = dto.content
        var decryptionState = DecryptionState.SUCCESS
        var wrappedFek: String? = null
        var mediaIv: String? = null
        
        if (!dto.ciphertext.isNullOrEmpty()) {
            val entityForDecryption = MessageEntity(
                id = dto.id, chatId = dto.chatId, senderId = dto.senderId, content = dto.content,
                timestamp = dto.createdAt, status = dto.status, type = dto.type,
                ciphertext = dto.ciphertext, envelopeType = dto.envelopeType, cryptoVersion = dto.cryptoVersion,
                nonce = dto.nonce,
                decryptionState = DecryptionState.PENDING
            )
            
            when (val result = decryptionManager.decrypt(entityForDecryption, updateDb = false)) {
                is com.keeftalk.chat.security.crypto.MessageDecryptionManager.DecryptionResult.Success -> {
                    decryptedContent = result.plaintext
                    decryptionState = DecryptionState.SUCCESS
                    wrappedFek = result.wrappedFek
                    mediaIv = result.mediaIv
                }
                is com.keeftalk.chat.security.crypto.MessageDecryptionManager.DecryptionResult.Pending -> {
                    decryptedContent = "" // Keep empty, don't show placeholders
                    decryptionState = DecryptionState.PENDING
                }
                is com.keeftalk.chat.security.crypto.MessageDecryptionManager.DecryptionResult.Failed -> {
                    decryptedContent = "" // Keep empty, don't show placeholders
                    decryptionState = if (result.permanent) DecryptionState.PERMANENT_FAILURE else DecryptionState.RETRY_REQUIRED
                }
            }
        }
        
        return DecryptedMessageBatchItem(dto, source, decryptedContent, decryptionState, wrappedFek, mediaIv)
    }

    private data class DecryptedMessageBatchItem(
        val dto: MessageDto,
        val source: String,
        val decryptedContent: String,
        val decryptionState: DecryptionState,
        val wrappedFek: String?,
        val mediaIv: String?
    )

    private suspend fun handleIncomingMessageInternalDecrypted(item: DecryptedMessageBatchItem) {
        val dto = item.dto
        val source = item.source
        val decryptedContent = item.decryptedContent
        val decryptionState = item.decryptionState
        val wrappedFek = item.wrappedFek
        val mediaIv = item.mediaIv
        
        val currentUserId = getCurrentUserId()
        val chatId = dto.chatId
        val senderId = dto.senderId
        val status = dto.status.uppercase()
        val timestamp = when {
            dto.timestampMs > 0 -> dto.timestampMs
            dto.createdAt > 0 -> dto.createdAt
            else -> System.currentTimeMillis()
        }
        val normalizedType = dto.type.uppercase()
        var normalizedStatus = status
        val isFromMe = senderId == currentUserId

        if (!isFromMe && (normalizedStatus == "SENT" || normalizedStatus == "FCM_RECEIVED")) {
            normalizedStatus = "DELIVERED"
            Log.d(TAG, "[$source] Incoming message from peer: auto-marking as DELIVERED locally and sending update")
            repositoryScope.launch { sendDeliveryReceipt(chatId, dto.id) }
        }

        val existingMessage = messageDao.getMessageById(dto.id)
        if (existingMessage != null) {
            val statusPriorityCurrent = getStatusPriority(existingMessage.status)
            val statusPriorityNew = getStatusPriority(normalizedStatus)
            val statusChanged = statusPriorityNew > statusPriorityCurrent
            
            if (statusChanged) {
                Log.d(TAG, "[$source] Updating message status: ${dto.id} ${existingMessage.status} -> $normalizedStatus")
                messageDao.updateMessageStatus(dto.id, normalizedStatus)
            }
            
            val timestampUpdated = existingMessage.timestamp <= 0 && timestamp > 0
            val lockUpdated = dto.mediaLockUpdatedAt ?: 0L > existingMessage.mediaLockUpdatedAt ?: 0L
            
            val needsDecryptionUpdate = existingMessage.decryptionState != DecryptionState.SUCCESS && decryptionState == DecryptionState.SUCCESS

            if (statusChanged || timestampUpdated || lockUpdated || needsDecryptionUpdate) {
                if (statusChanged && normalizedStatus == "SEEN" && isFromMe) {
                    messageDao.markEarlierMessagesAsRead(chatId, senderId, timestamp)
                }
                if (timestampUpdated) messageDao.updateMessageTimestamp(dto.id, timestamp)
                if (needsDecryptionUpdate) {
                    Log.d(TAG, "[$source] Updating previously encrypted message with decrypted content: ${dto.id}")
                    messageDao.updateMessageContent(dto.id, decryptedContent)
                    messageDao.updateDecryptionState(dto.id, DecryptionState.SUCCESS, existingMessage.retryCount)
                    
                    if (wrappedFek != null && mediaIv != null) {
                        // Update wrapped FEK in associated file entities
                        val attachments = fileDao.getFilesForMessage(dto.id)
                        attachments.forEach { file ->
                            // In PART 3, we store the wrapped FEK inside the encryption_metadata JSON
                            // We need to parse existing or create new FileEncryptionMetadata
                            val currentMeta = try {
                                file.encryptionMetadata?.let { Json.decodeFromString<FileEncryptionMetadata>(it) }
                            } catch (e: Exception) { null }
                            
                            val pck = AppModule.provideConversationKeyManager(context).getOrLoadKey(dto.chatId)
                            if (pck != null) {
                                val wrappedFekObj = Json.decodeFromString<EncryptedObject>(wrappedFek)
                                val envelope = EncryptionEnvelope(dto.chatId, wrappedFekObj, EnvelopeType.CONVERSATION)
                                
                                val newFileMeta = FileEncryptionMetadata(
                                    envelopes = (currentMeta?.envelopes ?: emptyList()) + envelope,
                                    fileIv = mediaIv,
                                    encryptedAttributes = currentMeta?.encryptedAttributes
                                )
                                fileDao.updateFile(file.copy(encryptionMetadata = Json.encodeToString(newFileMeta)))
                            }
                        }
                    }
                }
                if (lockUpdated) messageDao.updateMediaLockStatus(
                    dto.id, 
                    dto.mediaLocked, 
                    dto.mediaLockUpdatedAt ?: 0L, 
                    dto.mediaLockUpdatedBy ?: ""
                )

                val chat = chatDao.getChatById(chatId)
                if (chat != null) {
                    val unreadDelta = if (existingMessage.status != "SEEN" && normalizedStatus == "SEEN" && !isFromMe) -1 else 0
                    if (timestamp >= chat.lastTimestamp || unreadDelta != 0) {
                        updateChatMetadataInternalSync(chat, dto, normalizedType, normalizedStatus, timestamp, unreadDelta, decryptedContent)
                    }
                }
            }
            
            // Check if we need to fetch missing attachments for an existing message (e.g. from Sync)
            if (normalizedType in listOf("IMAGE", "VIDEO", "VOICE", "FILE", "PDF")) {
                val currentAttachments = fileDao.getFilesForMessage(dto.id)
                if (currentAttachments.isEmpty() && dto.attachments.isEmpty()) {
                    repositoryScope.launch { fetchAndSaveAttachments(dto.id, wrappedFek, mediaIv) }
                }
            }
        } else {
            Log.i("CHAT_PIPELINE", "MESSAGE_ROOM_UPSERT | id=${dto.id} | chat=$chatId | source=$source")
            messageDao.insertMessage(MessageEntity(
                id = dto.id, chatId = chatId, senderId = senderId, content = decryptedContent,
                timestamp = timestamp, status = normalizedStatus,
                type = normalizedType, replyToId = dto.replyToId, sourceLanguage = dto.sourceLanguage,
                mediaLocked = dto.mediaLocked,
                mediaLockUpdatedAt = dto.mediaLockUpdatedAt,
                mediaLockUpdatedBy = dto.mediaLockUpdatedBy,
                ciphertext = dto.ciphertext,
                cryptoVersion = dto.cryptoVersion,
                envelopeType = dto.envelopeType,
                nonce = dto.nonce,
                decryptionState = decryptionState
            ))

            // Handle Attachments
            if (dto.attachments.isNotEmpty()) {
                saveAttachmentDtos(dto.id, dto.attachments, wrappedFek, mediaIv)
            } else if (normalizedType in listOf("IMAGE", "VIDEO", "VOICE", "FILE", "PDF")) {
                // If it's a media message but no attachments in DTO (common for Realtime), fetch them
                repositoryScope.launch { 
                    delay(500) // Small delay to allow sender to finish attachment insert
                    fetchAndSaveAttachments(dto.id, wrappedFek, mediaIv) 
                }
            }

            dto.reactions.forEach { reactionDto ->
                val rUserId = reactionDto.userId
                if (rUserId.isNotEmpty() && reactionDto.emoji.isNotEmpty()) {
                    messageDao.insertReaction(MessageReactionEntity(dto.id, rUserId, reactionDto.emoji))
                }
            }
            val chat = chatDao.getChatById(chatId)
            if (chat == null) {
                repositoryScope.launch {
                    fetchAndCacheChatInfo(chatId)
                }
            } else {
                val unreadDelta = if (!isFromMe && normalizedStatus != "SEEN") 1 else 0
                updateChatMetadataInternalSync(chat, dto, normalizedType, normalizedStatus, timestamp, unreadDelta, decryptedContent)
            }
        }

        // --- AUTO-DOWNLOAD MEDIA TRIGGER ---
        // Trigger for BOTH new and existing messages if they are successfully decrypted
        if (decryptionState == DecryptionState.SUCCESS) {
            val isPdf = normalizedType == "FILE" && dto.attachments.firstOrNull()?.files?.fileName?.lowercase()?.endsWith(".pdf") == true
            if (normalizedType == "IMAGE" || normalizedType == "VIDEO" || normalizedType == "VOICE" || normalizedType == "PDF" || isPdf) {
                repositoryScope.launch {
                    // Small delay to ensure DB transaction from the caller (db.withTransaction) is committed
                    delay(800) 
                    val msg = messageDao.getMessageWithReactionsById(dto.id).firstOrNull()
                    if (msg != null) {
                        Log.i(TAG, "[AUTO_DOWNLOAD] Triggered for message ${msg.message.id} (${msg.message.type})")
                        ensureMediaLocal(msg.toDomainInternal())
                    }
                }
            }
        }
    }

    private fun observeDecryptionEvents() {
        decryptionManager.decryptionEvents
            .filterNotNull()
            .onEach { messageId ->
                try {
                    val msg = messageDao.getMessageById(messageId)
                    if (msg != null && msg.decryptionState == DecryptionState.SUCCESS) {
                        val chat = chatDao.getChatById(msg.chatId)
                        if (chat != null) {
                            val currentUserId = getCurrentUserId()
                            val isFromMe = msg.senderId == currentUserId
                            
                            // Calculate unread delta only if we haven't processed this message's metadata yet.
                            // We can check if chat.lastTimestamp is less than this message's timestamp.
                            val isNewer = msg.timestamp >= chat.lastTimestamp
                            val unreadDelta = if (isNewer && !isFromMe && msg.status != "SEEN") 1 else 0

                            Log.d(TAG, "[DECRYPTION_EVENT] Message $messageId decrypted. Updating chat ${msg.chatId} metadata. isNewer=$isNewer")
                            
                            updateChatMetadataInternalSync(
                                chat, 
                                msg.toDto(), 
                                msg.type.uppercase(), 
                                msg.status.uppercase(), 
                                msg.timestamp, 
                                unreadDelta, 
                                msg.content
                            )
                        }

                        // --- AUTO-DOWNLOAD ON LATE DECRYPTION ---
                        if (msg.type == "IMAGE" || msg.type == "VIDEO" || msg.type == "PDF" || msg.type == "VOICE") {
                            Log.i(TAG, "[AUTO_DOWNLOAD] Triggering after background decryption for message ${msg.id}")
                            messageDao.getMessageWithReactionsById(messageId).firstOrNull()?.let {
                                ensureMediaLocal(it.toDomainInternal())
                            }
                        }
                        
                        // --- REFRESH FASTPATH CACHE ---
                        repositoryScope.launch(Dispatchers.IO) {
                            try {
                                val initialChats = chatDao.getActiveChatsOnce(10, 0)
                                if (initialChats.isNotEmpty()) {
                                    updateChatListCache(initialChats)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to refresh FastPath after late decryption", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update chat metadata after decryption", e)
                }
            }
            .launchIn(repositoryScope)
    }

    private fun MessageEntity.toDto() = MessageDto(
        id = id,
        chatId = chatId,
        senderId = senderId,
        content = content,
        createdAt = timestamp,
        type = type,
        status = status,
        ciphertext = ciphertext,
        cryptoVersion = cryptoVersion,
        envelopeType = envelopeType
    )

    internal suspend fun handleIncomingMessageInternal(dto: MessageDto, source: String) {
        handleIncomingMessageInternalDecrypted(decryptBatchItem(dto, source))
    }

    private suspend fun updateChatListCache(chats: List<ChatEntity>) {
        val cacheEntities = chats.map { it.toCacheEntity() }
        chatListCacheDao.clearAll()
        chatListCacheDao.insertAll(cacheEntities)
        
        // Proactive Avatar Caching
        chats.forEach { chat ->
            if (!chat.avatarUrl.isNullOrEmpty()) {
                proactiveCacheAvatar(chat.peerId ?: chat.id, chat.avatarUrl)
            }
        }
        
        // Update FastPath KV cache for instant cold start
        try {
            val uiModels = cacheEntities.map { entity ->
                ChatListItemUiModel(
                    id = entity.chatId,
                    name = entity.username ?: "Unknown",
                    avatarUrl = entity.avatarPath,
                    initials = com.keeftalk.chat.util.AvatarUtils.getInitials(entity.username),
                    lastMessage = entity.lastMessage,
                    lastTimestamp = entity.lastMessageTimestamp,
                    formattedTimestamp = com.keeftalk.chat.ui.screens.formatTimestamp(entity.lastMessageTimestamp),
                    unreadCount = entity.unreadCount,
                    isPinned = false,
                    isMuted = false,
                    lastMessageStatus = entity.messageStatus?.let { try { MessageStatus.valueOf(it) } catch(_: Exception) { null } },
                    lastMessageSenderId = entity.lastMessageSenderId,
                    isArchived = false,
                    peerId = entity.userId,
                    type = ChatType.ONE_TO_ONE,
                    snippetType = entity.snippetType,
                    snippetUri = entity.snippetUri
                )
            }
            val json = Json.encodeToString(uiModels)
            userPrefsRepo.updateInstantChatCache(json)
            com.keeftalk.chat.util.FastPathInbox.update(uiModels)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update FastPath KV cache", e)
        }
    }

    private fun proactiveCacheAvatar(identifier: String, url: String) {
        repositoryScope.launch(Dispatchers.IO) {
            com.keeftalk.chat.util.PersistentAvatarManager.downloadAndCacheAvatar(context, identifier, url)
        }
    }

    private fun ChatEntity.toCacheEntity() = ChatListCacheEntity(
        chatId = id,
        userId = peerId,
        username = name,
        avatarPath = avatarUrl,
        lastMessage = lastMessage,
        lastMessageTimestamp = lastTimestamp,
        messageStatus = lastMessageStatus,
        lastMessageSenderId = lastMessageSenderId,
        unreadCount = unreadCount,
        sortOrder = 0,
        snippetType = snippetType,
        snippetUri = snippetUri
    )

    private suspend fun handleIncomingReaction(dto: MessageReactionDto, isDeleted: Boolean) {
        if (isDeleted) {
            if (dto.messageId.isNotEmpty() && dto.userId.isNotEmpty()) messageDao.deleteReaction(dto.messageId, dto.userId)
        } else {
            if (dto.messageId.isNotEmpty() && dto.userId.isNotEmpty() && dto.emoji.isNotEmpty()) {
                messageDao.insertReaction(MessageReactionEntity(dto.messageId, dto.userId, dto.emoji))
            }
        }
    }

    private suspend fun fetchAndSaveAttachments(messageId: String, wrappedFek: String?, mediaIv: String?) {
        try {
            val supabase = getSupabase()
            val attachments = supabase.postgrest["message_attachment"].select(
                columns = Columns.raw("*, files(*)")
            ) {
                filter { eq("message_id", messageId) }
            }.decodeList<MessageAttachmentDto>()
            
            if (attachments.isNotEmpty()) {
                saveAttachmentDtos(messageId, attachments, wrappedFek, mediaIv)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch attachments for message $messageId", e)
        }
    }

    private suspend fun saveAttachmentDtos(messageId: String, attachmentDtos: List<MessageAttachmentDto>, wrappedFek: String?, mediaIv: String?) {
        attachmentDtos.forEach { attachmentDto ->
            attachmentDto.files?.let { fileDto ->
                val fileEntity = FileEntity(
                    id = fileDto.id,
                    ownerId = fileDto.ownerId,
                    storagePath = fileDto.storagePath,
                    fileHash = fileDto.fileHash,
                    fileName = fileDto.fileName,
                    mimeType = fileDto.mimeType,
                    fileSize = fileDto.fileSize,
                    fileType = fileDto.fileType,
                    sourceType = "CHAT",
                    width = fileDto.width,
                    height = fileDto.height,
                    duration = null,
                    thumbnailPath = null,
                    encryptionMetadata = fileDto.encryptionMetadata ?: if (wrappedFek != null && mediaIv != null) {
                        val pck = AppModule.provideConversationKeyManager(context).getOrLoadKey(messageDao.getMessageById(messageId)?.chatId ?: "")
                        if (pck != null) {
                            val wrappedFekObj = Json.decodeFromString<EncryptedObject>(wrappedFek)
                            val envelope = EncryptionEnvelope("chat", wrappedFekObj, EnvelopeType.CONVERSATION)
                            Json.encodeToString(FileEncryptionMetadata(listOf(envelope), mediaIv))
                        } else null
                    } else null,
                    securityMetadata = null
                )
                fileDao.insertFile(fileEntity)
                fileDao.insertMessageAttachment(MessageAttachmentEntity(
                    id = attachmentDto.id,
                    messageId = messageId,
                    fileId = fileDto.id
                ))
            }
        }
    }

    private suspend fun handleIncomingMessage(dto: MessageDto, source: String) { incomingMessagesChannel.send(dto to source) }

    private suspend fun updateChatMetadataInternalSync(
        c: ChatEntity, 
        dto: MessageDto, 
        normalizedType: String, 
        normalizedStatus: String, 
        timestamp: Long, 
        unreadDelta: Int = 0,
        decryptedSnippet: String? = null
    ) {
        // Fetch latest version to avoid race conditions on unreadCount
        val current = chatDao.getChatById(c.id) ?: c
        val currentUserId = getCurrentUserId()
        
        val authorName = com.keeftalk.chat.util.LiveRecipientCache.get(dto.senderId)?.name
        val metadata = com.keeftalk.chat.util.ThreadMetadata(
            latestAuthorName = authorName
        ).toBlob()

        val isFromMe = dto.senderId == currentUserId
        val isGroup = current.type == "GROUP"
        
        // Ensure snippetText is NEVER raw encrypted content or placeholders
        val rawContent = decryptedSnippet ?: dto.content
        val isTrulyEncrypted = rawContent == "[Encrypted]" || rawContent.startsWith("[Decryption Pending") || rawContent.startsWith("[Encrypted Message")
        
        if (isTrulyEncrypted) {
            Log.d(TAG, "[METADATA_GUARD] Skipping update for message ${dto.id} as it is still encrypted.")
            return
        }

        // Relaxed Guard: Update snippet if it's a newer message OR if we are replacing an encrypted snippet with a decrypted one at the same timestamp
        val isNewer = timestamp > current.lastTimestamp
        val isReplacingEncrypted = timestamp == current.lastTimestamp && (current.lastDecryptedMessage == null || current.lastMessage?.startsWith("[") == true)
        
        if (!isNewer && !isReplacingEncrypted) {
            Log.d(TAG, "[METADATA_GUARD] Skipping update for message ${dto.id} as it is older/duplicate.")
            return
        }

        val effectiveType = if (normalizedType == "FILE" && dto.attachments.firstOrNull()?.files?.fileName?.lowercase()?.endsWith(".pdf") == true) "PDF" else normalizedType
        
        val snippetText = when (effectiveType) {
            "TEXT" -> rawContent
            "IMAGE" -> when {
                isFromMe -> "You sent a picture"
                isGroup -> "$authorName sent a picture"
                else -> "You received a picture"
            }
            "VIDEO" -> when {
                isFromMe -> "You sent a video"
                isGroup -> "$authorName sent a video"
                else -> "You received a video"
            }
            "VOICE" -> when {
                isFromMe -> "You sent a voice message"
                isGroup -> "$authorName sent a voice message"
                else -> "You received a voice message"
            }
            "FILE" -> {
                val fileName = dto.attachments.firstOrNull()?.files?.fileName ?: "document"
                when {
                    isFromMe -> "You sent $fileName"
                    isGroup -> "$authorName sent $fileName"
                    else -> "You received $fileName"
                }
            }
            "PDF" -> when {
                isFromMe -> "You sent a PDF"
                isGroup -> "$authorName sent a PDF"
                else -> "You received a PDF"
            }
            "LOCATION" -> when {
                isFromMe -> "You shared a location"
                isGroup -> "$authorName shared a location"
                else -> "You received a location"
            }
            "CONTACT" -> when {
                isFromMe -> "You shared a contact"
                isGroup -> "$authorName shared a contact"
                else -> "You received a contact"
            }
            else -> "[$normalizedType]"
        }

        val decryptedSnippetForChat = snippetText

        chatDao.insertChat(current.copy(
            lastMessage = snippetText,
            lastDecryptedMessage = decryptedSnippetForChat,
            lastTimestamp = if (isNewer) timestamp else current.lastTimestamp,
            lastMessageStatus = if (isNewer || isReplacingEncrypted) normalizedStatus else current.lastMessageStatus,
            lastMessageSenderId = if (isNewer || isReplacingEncrypted) dto.senderId else current.lastMessageSenderId,
            unreadCount = (current.unreadCount + unreadDelta).coerceAtLeast(0),
            snippetType = if (isNewer || isReplacingEncrypted) normalizedType else current.snippetType,
            snippetUri = if (isNewer || isReplacingEncrypted) {
                fileDao.getFilesForMessage(dto.id).firstOrNull()?.storagePath
            } else current.snippetUri,
            metadata = metadata
        ))
    }

    private suspend fun fetchAndCacheChatInfo(chatId: String) {
        try {
            val currentUserId = getCurrentUserId() ?: return
            val supabase = getSupabase()
            val supabaseChat = supabase.postgrest["chats"].select { filter { eq("id", chatId) } }.decodeSingleOrNull<JsonObject>() ?: return
            val members = supabase.postgrest["chat_members"].select { filter { eq("chat_id", chatId) } }.decodeList<JsonObject>()
            val otherMemberId = members.asSequence().map { it["user_id"]?.jsonPrimitive?.content }.find { it != currentUserId }
            var name = supabaseChat["name"]?.jsonPrimitive?.contentOrNull
            var avatarUrl = supabaseChat["avatar_url"]?.jsonPrimitive?.contentOrNull
            var peerId: String? = null
            val type = supabaseChat["type"]?.jsonPrimitive?.content ?: "ONE_TO_ONE"
            
            val isPlaceholderName = name == null || name == "Unknown" || name.equals("Direct Chat", ignoreCase = true)
            
            if (type == "ONE_TO_ONE" && otherMemberId != null) {
                peerId = otherMemberId
                val otherProfile = supabase.postgrest["profiles"].select { filter { eq("id", otherMemberId) } }.decodeSingleOrNull<Profile>()
                if (otherProfile != null) {
                    if (isPlaceholderName) {
                        name = otherProfile.fullName ?: otherProfile.username
                    }
                    avatarUrl = otherProfile.avatarUrl
                    userDao.insertUser(UserEntity(
                        id = otherProfile.id,
                        name = otherProfile.fullName ?: otherProfile.username,
                        username = otherProfile.username,
                        phone = otherProfile.phone,
                        avatarUrl = avatarUrl,
                        isActive = true,
                        lastSeen = otherProfile.lastSeen,
                        isOnline = true
                    ))
                }
            }
            val lastTimestampLocal = messageDao.getLastMessageTimestampForChat(chatId) ?: 0L
            val lastTimestampRemote = supabaseChat["last_message_time_ms"]?.jsonPrimitive?.longOrNull
                ?: supabaseChat["last_message_time"]?.jsonPrimitive?.contentOrNull?.let { TimestampSerializer.parseTimestamp(it) } ?: 0L
            val lastMessageRemote = supabaseChat["last_message_text"]?.jsonPrimitive?.contentOrNull

            val existing = chatDao.getChatById(chatId)
            val isRemoteEncrypted = lastMessageRemote?.startsWith("[Encrypted]") == true
            
            val (finalLastMessage, finalLastDecrypted) = if (isRemoteEncrypted && existing != null && existing.lastDecryptedMessage != null) {
                existing.lastDecryptedMessage to existing.lastDecryptedMessage
            } else if (!isRemoteEncrypted) {
                lastMessageRemote to lastMessageRemote
            } else {
                // NEVER display [Encrypted] or placeholders
                null to null
            }

            chatDao.insertChat(ChatEntity(
                id = chatId,
                name = name,
                avatarUrl = avatarUrl,
                type = type,
                lastMessage = finalLastMessage,
                lastDecryptedMessage = finalLastDecrypted,
                lastTimestamp = maxOf(lastTimestampLocal, lastTimestampRemote),
                unreadCount = 0,
                peerId = peerId,
                autoDeleteTimer = supabaseChat["auto_delete_timer"]?.jsonPrimitive?.longOrNull,
                containerId = supabaseChat["container_id"]?.jsonPrimitive?.contentOrNull
            ))
            
            if (!avatarUrl.isNullOrEmpty()) {
                proactiveCacheAvatar(peerId ?: chatId, avatarUrl)
            }
            
            members.forEach { member -> member["user_id"]?.jsonPrimitive?.content?.let { chatDao.insertChatMember(ChatMemberEntity(chatId, it)) } }
        } catch (_: Exception) {}
    }

    override fun getChats(): Flow<List<Chat>> = chatDao.getActiveChats().map { entities -> entities.map { it.toDomainInternal() } }
    override fun getChats(limit: Int, offset: Int): Flow<List<Chat>> = chatDao.getActiveChats(limit, offset).map { entities -> entities.map { it.toDomainInternal() } }
    override fun getArchivedChats(): Flow<List<Chat>> = chatDao.getArchivedChats().map { entities -> entities.map { it.toDomainInternal() } }
    override fun getChat(chatId: String): Flow<Chat?> = chatDao.getChatFlowById(chatId).map { 
        if (it == null) repositoryScope.launch { fetchAndCacheChatInfo(chatId) }
        it?.toDomainInternal() 
    }

    override fun getChatPager(filter: String, query: String): Flow<androidx.paging.PagingData<Chat>> {
        return androidx.paging.Pager(
            config = androidx.paging.PagingConfig(
                pageSize = 30,
                prefetchDistance = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                if (query.isNotBlank()) {
                    chatDao.searchChatsPaging("%$query%")
                } else {
                    when (filter) {
                        "unread" -> chatDao.getUnreadChatsPaging()
                        "groups" -> chatDao.getGroupChatsPaging()
                        "archived" -> chatDao.getArchivedChatsPaging()
                        "favorites" -> chatDao.getFavoriteChatsPaging()
                        else -> chatDao.getActiveChatsPaging()
                    }
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainInternal() }
        }.flowOn(Dispatchers.IO)
    }
    
    override fun getMessage(messageId: String): Flow<Message?> = 
        messageDao.getMessageWithReactionsById(messageId).map { it?.toDomainInternal() }

    override fun getMessages(chatId: String): Flow<List<Message>> = getMessages(chatId, 20, 0)
    override suspend fun syncChat(chatId: String) { syncChatMessages(chatId) }

    private suspend fun syncChatMessages(chatId: String) {
        if (chatDao.getChatById(chatId) == null) fetchAndCacheChatInfo(chatId)
        repositoryScope.launch { flushReceiptSyncQueue() }
        try {
            val currentUserId = getCurrentUserId() ?: return
            val supabase = getSupabase()
            
            // Mark pending messages as delivered
            repositoryScope.launch { 
                try { 
                    supabase.postgrest["messages"].update({ set("status", "DELIVERED") }) { 
                        filter { 
                            eq("chat_id", chatId)
                            eq("status", "SENT")
                            neq("sender_id", currentUserId) 
                        } 
                    } 
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mark pending messages as delivered", e)
                } 
            }

            val lastTimestamp = messageDao.getLastMessageTimestampForChat(chatId) ?: 0L
            
            val newMessages = withContext(Dispatchers.IO) {
                supabase.postgrest["messages"].select(Columns.raw("*, message_attachment(*, files(*))")) { 
                    filter { 
                        eq("chat_id", chatId)
                        if (lastTimestamp > 0) {
                            gt("timestamp", lastTimestamp)
                        }
                    } 
                    order("timestamp", order = if (lastTimestamp > 0) Order.ASCENDING else Order.DESCENDING) 
                    limit(20) 
                }.decodeList<MessageDto>()
            }
            
            Log.d(TAG, "[SYNC] chat=$chatId returned=${newMessages.size}")
            newMessages.forEach { handleIncomingMessage(it, "SYNC") }
        } catch (e: Exception) {
            Log.e(TAG, "syncChatMessages failed for $chatId", e)
        }
    }

    override fun getMessages(chatId: String, limit: Int, offset: Int): Flow<List<Message>> {
        if (offset == 0) repositoryScope.launch { syncChatMessages(chatId) } else repositoryScope.launch { loadOlderMessages(chatId, limit, offset) }
        return messageDao.getMessagesForChatWithReactions(chatId, limit, offset).map { entities -> entities.map { it.toDomainInternal() }.reversed() }.flowOn(Dispatchers.IO)
    }

    @OptIn(androidx.paging.ExperimentalPagingApi::class)
    override fun getMessagePager(chatId: String): Flow<androidx.paging.PagingData<Message>> {
        val db = AppModule.provideDatabase(context)
        PerformanceProfiler.logEvent("getMessagePager called", category = PerformanceProfiler.Category.UI)
        PerformanceProfiler.startStage("Chat Room Query")
        
        // Diagnostic Room Check
        repositoryScope.launch {
            val messages = messageDao.getMessagesForChatWithReactionsOnce(chatId, 100, 0)
            val success = messages.count { it.message.decryptionState == com.keeftalk.chat.domain.model.DecryptionState.SUCCESS }
            val pending = messages.count { it.message.decryptionState == com.keeftalk.chat.domain.model.DecryptionState.PENDING || it.message.decryptionState == com.keeftalk.chat.domain.model.DecryptionState.RETRY_REQUIRED }
            val failed = messages.count { it.message.decryptionState == com.keeftalk.chat.domain.model.DecryptionState.PERMANENT_FAILURE }
            
            PerformanceProfiler.logEvent("ChatDetail: Decryption State Snapshot", info = "chatId=$chatId success=$success pending=$pending failed=$failed", category = PerformanceProfiler.Category.CHAT)
            PerformanceProfiler.endStage("Chat Room Query")
        }

        return androidx.paging.Pager<Int, MessageWithReactions>(
            config = androidx.paging.PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                prefetchDistance = 1,
                enablePlaceholders = false
            ),
            remoteMediator = MessageRemoteMediator(
                chatId = chatId,
                database = db,
                supabaseProvider = { getSupabase() },
                handleIncomingMessage = { dto: MessageDto, source: String -> handleIncomingMessageInternal(dto, source) }
            ),
            pagingSourceFactory = { messageDao.getMessagesForChatWithReactionsPaging(chatId) }
        ).flow.onEach {
            PerformanceProfiler.logEvent("Paging Data Emitted", info = "Count: 20", category = PerformanceProfiler.Category.DATABASE)
        }.map { pagingData ->
            pagingData.map { it.toDomainInternal() }
        }.flowOn(Dispatchers.IO)
    }

    override fun getMediaMessagePager(chatId: String): Flow<androidx.paging.PagingData<Message>> {
        return androidx.paging.Pager<Int, MessageWithReactions>(
            config = androidx.paging.PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { messageDao.getMediaMessagesForChatPaging(chatId) }
        ).flow.map { pagingData: androidx.paging.PagingData<MessageWithReactions> ->
            pagingData.map { it.toDomainInternal() }
        }
    }

    override suspend fun getMediaMessageIds(chatId: String): List<String> = messageDao.getMediaMessageIds(chatId)

    override suspend fun loadOlderMessages(chatId: String, limit: Int, offset: Int) {
        try {
            val oldestMsg = messageDao.getMessagesForChatWithReactionsOnce(chatId, 1, 0).firstOrNull()?.message
            val oldestTimestamp = oldestMsg?.timestamp ?: System.currentTimeMillis()
            val oldestId = oldestMsg?.id
            
            val olderMessages = getSupabase().postgrest["messages"].select(
                Columns.raw("*, message_attachment(*, files(*))")
            ) { 
                filter { 
                    eq("chat_id", chatId)
                    if (oldestId != null) {
                        or {
                            lt("created_at", TimestampSerializer.formatTimestamp(oldestTimestamp))
                            and {
                                eq("created_at", TimestampSerializer.formatTimestamp(oldestTimestamp))
                                lt("id", oldestId)
                            }
                        }
                    } else {
                        lt("created_at", TimestampSerializer.formatTimestamp(oldestTimestamp))
                    }
                }
                order("created_at", order = Order.DESCENDING)
                limit(limit.toLong()) 
            }.decodeList<MessageDto>()
            Log.d(TAG, "[PAGING_APPEND_MANUAL] chat=$chatId requested=$limit returned=${olderMessages.size}")
            olderMessages.forEach { handleIncomingMessage(it, "PAGING_APPEND_MANUAL") }
        } catch (_: Exception) {}
    }

    override suspend fun loadOlderMessages(chatId: String) { loadOlderMessages(chatId, 20, 0) }
    override fun getContacts(): Flow<List<User>> = userDao.getContacts().map { entities -> entities.map { it.toDomain() } }
    override fun getRecentChatUsers(): Flow<List<User>> = userDao.getRecentChatUsers().map { entities -> entities.map { it.toDomain() } }.flowOn(Dispatchers.IO)
    override fun getBlockedUsers(): Flow<List<User>> = userDao.getBlockedUsers().map { entities -> entities.map { it.toDomain() } }
    override fun getContact(userId: String): Flow<User?> = flow {
        val cached = com.keeftalk.chat.util.LiveRecipientCache.get(userId)
        if (cached != null) emit(cached)
        userDao.getUserFlowById(userId).collect { entity -> 
            val domain = entity?.toDomain()
            if (domain != null) com.keeftalk.chat.util.LiveRecipientCache.put(domain)
            emit(domain) 
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    override fun getChatMembers(chatId: String): Flow<List<User>> = 
        chatDao.getChatMembers(chatId).map { entities -> entities.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    override suspend fun reportProfileView(viewedUserId: String) {
        val currentUserId = getCurrentUserId() ?: return
        if (currentUserId == viewedUserId) return
        try { 
            getSupabase().postgrest["profile_views"].insert(buildJsonObject { put("viewer_id", currentUserId); put("viewed_id", viewedUserId) }) 
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report profile view", e)
        }
    }

    override suspend fun syncBroadcasts() {}
    override suspend fun createReminderNotification(chatId: String, senderName: String, count: Int) {
        if (notificationDao.getActiveReminderForChat(chatId) != null) return
        val reminder = AppNotification(
            id = UUID.randomUUID().toString(),
            type = NotificationType.REMINDER,
            priority = NotificationPriority.NORMAL,
            title = "Forgotten Conversation",
            message = "$senderName sent you $count new messages.",
            timestamp = System.currentTimeMillis(),
            data = NotificationData(chatId = chatId)
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(reminder))
    }

    private fun resolveFileMetadata(uri: android.net.Uri): Pair<String?, Long?> {
        var name: String? = null
        var size: Long? = null
        
        try {
            // Strategy 1: ContentResolver Query (Standard)
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val queriedName = cursor.getString(nameIndex)
                        if (!queriedName.isNullOrBlank() && !queriedName.startsWith("document:")) {
                            name = queriedName
                        }
                    }
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }

            // Strategy 2: DocumentFile (Powerful for content://)
            if (name == null) {
                val documentFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
                if (documentFile != null && documentFile.exists()) {
                    val docName = documentFile.name
                    if (!docName.isNullOrBlank() && !docName.startsWith("document:")) {
                        name = docName
                    }
                    if (size == null || size == 0L) size = documentFile.length()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving metadata for $uri", e)
        }

        // Strategy 3: URI Path Parsing (Fallback)
        if (name == null) {
            val lastSegment = uri.lastPathSegment
            if (lastSegment != null) {
                name = if (lastSegment.contains(":")) lastSegment.substringAfterLast(":") else lastSegment
            }
        }
        
        // Final sanity check: if it's just a number, null, or missing extension
        if (name == null || !name.contains(".") || name.all { it.isDigit() } || name.contains(":")) {
            val mimeType = context.contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
            
            var baseName = if (name != null) {
                if (name.contains(":")) name.substringAfterLast(":") else name
            } else {
                "file_" + System.currentTimeMillis().toString().takeLast(4)
            }
            
            if (baseName.all { it.isDigit() }) {
                baseName = "file_$baseName"
            }
            
            name = if (baseName.contains(".")) baseName else "$baseName.$extension"
        }

        return name to size
    }

    override suspend fun sendMessage(chatId: String, content: String, type: MessageType, filePath: String?, replyToId: String?, onProgress: ((Float) -> Unit)?) {
        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val currentUserId = userPrefsRepo.getUserIdFast() ?: ""
        
        Log.i("CHAT_PIPELINE", "MESSAGE_SEND_START | id=$messageId | chat=$chatId | type=$type | user=$currentUserId (FAST)")
        
        // 1. IMMEDIATE LOCAL INSERT
        withContext(databaseWriteDispatcher) {
            try { 
                messageDao.insertMessage(MessageEntity(
                    id = messageId, chatId = chatId, senderId = currentUserId, content = content, 
                    timestamp = now, status = "SENDING", 
                    type = type.name, replyToId = replyToId,
                    ciphertext = null, cryptoVersion = 2, envelopeType = 100, nonce = null,
                    decryptionState = DecryptionState.SUCCESS
                )) 
                Log.i("CHAT_PIPELINE", "MESSAGE_LOCAL_INSERT | id=$messageId | status=SENDING")
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage: Failed to insert local message", e)
            }
        }

        // 2. BACKGROUND NETWORK SYNC & MEDIA UPLOAD
        repositoryScope.launch {
            try {
                Log.d("CHAT_PIPELINE", "MESSAGE_NETWORK_INSERT_START | id=$messageId")
                
                var textContent = content
                var uploadedFile: com.keeftalk.chat.domain.model.File? = null
                var attachmentId: String? = null

                if (filePath != null && type != MessageType.TEXT && type != MessageType.LOCATION) {
                    val file = try {
                        if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
                            val uri = Uri.parse(filePath)
                            val tempFile = File(context.cacheDir, "upload_${UUID.randomUUID()}")
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            if (tempFile.exists()) tempFile else null
                        } else {
                            val f = File(filePath)
                            if (f.exists()) f else null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to resolve file for upload: $filePath", e)
                        null
                    }

                    if (file != null) {
                        Log.d("CHAT_PIPELINE", "Uploading file: ${file.absolutePath} for message $messageId")
                        val result = fileUploadManager.uploadFile(file, SourceType.CHAT, currentUserId)
                        if (result.isSuccess) {
                            val uploaded = result.getOrThrow()
                            uploadedFile = uploaded
                            attachmentId = UUID.randomUUID().toString()
                            fileDao.insertMessageAttachment(MessageAttachmentEntity(
                                id = attachmentId!!,
                                messageId = messageId,
                                fileId = uploaded.id
                            ))
                            
                            // --- SECURE ENVELOPE SHARING ---
                            val fek = AppModule.provideFileRepository(context).getDecryptedFEK(uploaded).getOrThrow()
                            val pck = AppModule.provideConversationKeyManager(context).getOrLoadKey(chatId) ?: throw Exception("PCK not found")
                            val wrappedFek = StorageCryptoService.wrapKey(fek, pck)
                            
                            textContent = buildJsonObject {
                                put("text", content)
                                put("wrappedFek", Json.encodeToString(EncryptedObject.serializer(), wrappedFek))
                                put("mediaIv", Json.decodeFromString<FileEncryptionMetadata>(uploaded.encryptionMetadata!!).fileIv)
                            }.toString()
                        } else {
                            throw result.exceptionOrNull() ?: Exception("File upload failed")
                        }
                    } else {
                        Log.w("CHAT_PIPELINE", "File not found or could not be read: $filePath")
                    }
                }
                
                // --- E2E ENCRYPTION ---
                var finalCiphertext: String? = null
                var finalEnvelopeType = 100
                var finalNonce: String? = null
                var finalCryptoVersion = 2

                try {
                    val (envelope, version) = cryptoManager.encryptMessage(chatId, textContent)
                    finalCiphertext = android.util.Base64.encodeToString(envelope.ciphertext, android.util.Base64.NO_WRAP)
                    finalEnvelopeType = envelope.type
                    finalNonce = envelope.nonce
                    finalCryptoVersion = version
                } catch (e: Exception) {
                    Log.e(TAG, "CRITICAL: Failed to encrypt message for $chatId. Aborting send.", e)
                    withContext(databaseWriteDispatcher) { messageDao.updateMessageStatus(messageId, "FAILED") }
                    return@launch
                }

                val dto = MessageDto(
                    id = messageId, 
                    chatId = chatId, 
                    senderId = currentUserId, 
                    content = if (finalCiphertext != null) "[Encrypted]" else content.ifBlank { "[${type.name}]" }, 
                    createdAt = now, 
                    timestampMs = now,
                    type = if (type == MessageType.PDF) "FILE" else type.name, 
                    status = "SENT", 
                    replyToId = replyToId,
                    ciphertext = finalCiphertext,
                    envelopeType = finalEnvelopeType,
                    cryptoVersion = finalCryptoVersion,
                    nonce = finalNonce
                )
                
                val supabase = getSupabase()
                // Step 1: Upsert message
                supabase.postgrest["messages"].upsert(dto)
                
                // Step 2: Sync attachment reference if exists (MUST succeed before completion)
                if (uploadedFile != null && attachmentId != null) {
                    supabase.postgrest["message_attachment"].insert(buildJsonObject {
                        put("id", attachmentId)
                        put("message_id", messageId)
                        put("file_id", uploadedFile.id)
                    })
                }

                Log.i("CHAT_PIPELINE", "MESSAGE_NETWORK_INSERT_SUCCESS | id=$messageId")

                withContext(databaseWriteDispatcher) { 
                    messageDao.updateMessageStatus(messageId, "SENT")
                    if (finalCiphertext != null) {
                        messageDao.updateMessageCiphertext(messageId, finalCiphertext, finalCryptoVersion, finalEnvelopeType, finalNonce)
                    }
                    Log.i("CHAT_PIPELINE", "MESSAGE_SEND_COMPLETE | id=$messageId")
                }
            } catch (e: Exception) {
                Log.e("CHAT_PIPELINE", "MESSAGE_NETWORK_INSERT_FAILURE | id=$messageId | error=${e.message}")
                withContext(databaseWriteDispatcher) { messageDao.updateMessageStatus(messageId, "FAILED") } 
            }
        }
    }

    private suspend fun ensureBucketExists() {
        try { 
            val admin = getAdminSupabase()
            Log.d(TAG, "Ensuring storage buckets exist...")
            admin.storage.createBucket("files") { public = true }
            Log.d(TAG, "Bucket 'files' ensured.")
        } catch (e: Exception) {
            Log.w(TAG, "Bucket creation/check failed (likely already exists): ${e.message}")
        }
    }


    override suspend fun createGroup(name: String, memberIds: List<String>, avatarUrl: String?): String {
        val currentUserId = getCurrentUserId() ?: throw Exception("Unauthorized")
        val chatId = UUID.randomUUID().toString()
        val supabase = getSupabase()
        
        var finalAvatarUrl = avatarUrl
        
        if (avatarUrl != null && (avatarUrl.startsWith("content://") || avatarUrl.startsWith("file://"))) {
            try {
                val uri = avatarUrl.toUri()
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val adminSupabase = getAdminSupabase()
                    val bucketName = "files"
                    try { adminSupabase.storage.createBucket(bucketName) { public = true } } catch (_: Exception) {}
                    
                    val bucket = adminSupabase.storage[bucketName]
                    val remotePath = "groups/$chatId/avatar_${System.currentTimeMillis()}.jpg"
                    bucket.upload(remotePath, bytes) { upsert = true }
                    finalAvatarUrl = bucket.publicUrl(remotePath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload group avatar", e)
            }
        }

        try {
            // 1. Insert chat
            supabase.postgrest["chats"].insert(buildJsonObject { 
                put("id", chatId)
                put("name", name)
                put("type", "GROUP")
                if (finalAvatarUrl != null) put("avatar_url", finalAvatarUrl)
                put("created_at", System.currentTimeMillis())
            })

            // 2. Insert members including creator as admin
            val now = System.currentTimeMillis()
            val members = (memberIds.map { it to "member" } + (currentUserId to "admin")).map { (uid, role) ->
                buildJsonObject {
                    put("chat_id", chatId)
                    put("user_id", uid)
                    put("role", role)
                    put("joined_at", now)
                }
            }
            supabase.postgrest["chat_members"].insert(members)

            fetchAndCacheChatInfo(chatId)
            return chatId
        } catch (e: Exception) { 
            Log.e(TAG, "createGroup failed", e)
            throw e 
        }
    }

    override suspend fun syncMessages(
        @Suppress("UNUSED_PARAMETER") peerId: String,
        @Suppress("UNUSED_PARAMETER") lastSyncTimestamp: Long
    ): Long = 0L
    override suspend fun syncChats(limit: Int, offset: Int) {
        try {
            val currentUserId = getCurrentUserId() ?: return
            val supabase = getSupabase()
            
            // 1. Fetch chat memberships with new columns
            val memberships = supabase.postgrest["chat_members"].select(
                columns = Columns.raw("*, chats(*, chat_members(*, profiles(*)))")
            ) {
                filter {
                    eq("user_id", currentUserId)
                }
                limit(limit.toLong())
                range(offset.toLong(), (offset + limit - 1).toLong())
            }.decodeList<ChatMembershipDto>()

            if (memberships.isEmpty()) return

            val chatEntities = mutableListOf<ChatEntity>()
            val memberEntities = mutableListOf<ChatMemberEntity>()

            memberships.forEach { membership ->
                val dto = membership.chats ?: return@forEach
                
                var chatName = dto.name
                var peerId: String? = null

                if (dto.type == "ONE_TO_ONE") {
                    val otherMember = dto.members.find { it.userId != currentUserId }
                    if (otherMember != null) {
                        peerId = otherMember.userId
                        if (chatName == null || chatName.isBlank() || chatName == "Unknown" || chatName == "Direct Chat") {
                            chatName = otherMember.profiles?.fullName ?: otherMember.profiles?.username ?: "Unknown"
                        }
                        
                        otherMember.profiles?.let { profile ->
                            profileDao.insertProfile(profile.toEntity())
                            userDao.insertUser(UserEntity(
                                id = profile.id,
                                name = profile.fullName ?: profile.username,
                                username = profile.username,
                                avatarUrl = profile.avatarUrl,
                                isActive = true,
                                lastSeen = profile.lastSeen,
                                isContact = false
                            ))
                        }
                    }
                }
                
                // Use bigint timestamp (lastMessageTimeMs) if available, fallback to parsed ISO
                val lastTimestamp = dto.lastMessageTimeMs ?: dto.lastMessageTime?.let { TimestampSerializer.parseTimestamp(it) } ?: 0L

                chatEntities.add(ChatEntity(
                    id = dto.id,
                    name = chatName,
                    avatarUrl = dto.avatarUrl,
                    type = dto.type,
                    lastMessage = dto.lastMessage,
                    lastTimestamp = lastTimestamp,
                    unreadCount = 0,
                    isPinned = membership.isPinned,
                    isMuted = membership.isMuted,
                    isArchived = membership.isArchived,
                    themeId = membership.themeId,
                    autoTranslateEnabled = dto.autoTranslateEnabled,
                    lastMessageStatus = dto.lastMessageStatus,
                    lastMessageSenderId = dto.lastMessageSenderId,
                    peerId = peerId,
                    autoDeleteTimer = dto.autoDeleteTimer,
                    containerId = dto.containerId
                ))

                memberEntities.add(ChatMemberEntity(
                    chatId = dto.id,
                    userId = currentUserId,
                    lastReadMessageId = membership.lastReadMessageId,
                    lastDeliveredMessageId = membership.lastDeliveredMessageId,
                    lastReadAt = membership.lastReadAt?.let { TimestampSerializer.parseTimestamp(it) },
                    lastDeliveredAt = membership.lastDeliveredAt?.let { TimestampSerializer.parseTimestamp(it) }
                ))

                // Also add other members for groups
                dto.members.forEach { mSyncDto ->
                    if (mSyncDto.userId != currentUserId) {
                        memberEntities.add(ChatMemberEntity(
                            chatId = dto.id,
                            userId = mSyncDto.userId,
                            lastReadMessageId = mSyncDto.lastReadMessageId,
                            lastDeliveredMessageId = mSyncDto.lastDeliveredMessageId,
                            lastReadAt = mSyncDto.lastReadAt?.let { TimestampSerializer.parseTimestamp(it) },
                            lastDeliveredAt = mSyncDto.lastDeliveredAt?.let { TimestampSerializer.parseTimestamp(it) }
                        ))
                    }
                }
            }

            // 2. Persist to local database
            // --- SNIPPET PERSISTENCE ---
            // Before inserting, check if we already have decrypted snippets locally
            // to avoid overwriting them with "[Encrypted]" from the server.
            val chatsToInsert = chatEntities.map { newChat ->
                val existing = chatDao.getChatById(newChat.id)
                if (existing != null) {
                    val isRemoteEncrypted = newChat.lastMessage?.startsWith("[Encrypted]") == true
                    
                    if (isRemoteEncrypted) {
                        if (existing.lastDecryptedMessage != null && existing.lastTimestamp >= newChat.lastTimestamp) {
                            newChat.copy(
                                lastMessage = existing.lastDecryptedMessage,
                                lastDecryptedMessage = existing.lastDecryptedMessage,
                                snippetType = existing.snippetType,
                                snippetUri = existing.snippetUri
                            )
                        } else {
                            // Remote is encrypted and we don't have a decrypted version
                            newChat.copy(lastMessage = null, lastDecryptedMessage = null)
                        }
                    } else if (!isRemoteEncrypted) {
                        newChat.copy(lastDecryptedMessage = newChat.lastMessage)
                    } else {
                        newChat
                    }
                } else {
                    newChat
                }
            }

            chatDao.insertChats(chatsToInsert)
            chatDao.insertChatMembers(memberEntities)
            
        } catch (e: Exception) {
            Log.e(TAG, "syncChats failed", e)
        }
    }

    override suspend fun syncFullChatHistory() {
        // Sync more chats, e.g., first 50
        syncChats(limit = 50, offset = 0)
    }

    override suspend fun syncAllRecentContent() {
        // Initial sync of recent 15 chats
        syncChats(limit = 15, offset = 0)
    }
    override suspend fun syncAllProfiles() {
        try {
            val profiles = getSupabase().postgrest["profiles"].select().decodeList<Profile>()
            userDao.insertUsers(profiles.map { 
                UserEntity(
                    id = it.id,
                    name = it.fullName ?: it.username,
                    username = it.username,
                    phone = it.phone,
                    avatarUrl = it.avatarUrl,
                    isActive = true,
                    lastSeen = it.lastSeen,
                    isOnline = true
                )
            })
        } catch (_: Exception) {}
    }

    override suspend fun searchUsers(query: String): Result<List<Profile>> = runCatching {
        getSupabase().postgrest["profiles"].select {
            filter {
                or {
                    ilike("username", "%$query%")
                    ilike("full_name", "%$query%")
                    ilike("phone", "%$query%")
                    ilike("email", "%$query%")
                }
            }
        }.decodeList<Profile>().also { profiles -> profileDao.insertProfiles(profiles.map { it.toEntity() }) }
    }

    private fun Profile.toEntity() = ProfileEntity(
        id = id,
        username = username,
        fullName = fullName,
        email = email,
        phone = phone,
        avatarUrl = avatarUrl,
        coverUrl = coverUrl,
        bio = bio,
        country = country,
        countryCode = countryCode,
        joinDate = joinDate,
        isVerified = isVerified,
        lastSeen = lastSeen,
        avatarVisibility = privacy.avatarVisibility,
        coverVisibility = privacy.coverVisibility,
        phoneVisibility = privacy.phoneVisibility,
        emailVisibility = privacy.emailVisibility,
        bioVisibility = privacy.bioVisibility,
        lastSeenVisibility = privacy.lastSeenVisibility,
        onlineStatusVisibility = privacy.onlineStatusVisibility,
        notificationSettingsJson = Json.encodeToString(com.keeftalk.chat.data.prefs.NotificationPreferences.serializer(), notifications)
    )

    override suspend fun searchUserByPhone(phone: String): Result<Profile?> = runCatching {
        getSupabase().postgrest["profiles"].select { filter { eq("phone", phone) } }.decodeSingleOrNull<Profile>()
    }

    override suspend fun getOrCreateOneToOneChat(otherUserId: String): Result<String> = try {
        val chatId = getSupabase().postgrest.rpc("get_or_create_1to1_chat", buildJsonObject { put("other_user_id", otherUserId) }).decodeAs<String>()
        fetchAndCacheChatInfo(chatId)
        Result.success(chatId)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        val existing = messageDao.getMessageById(messageId)
        if (existing != null && getStatusPriority(status.name) <= getStatusPriority(existing.status)) return
        messageDao.updateMessageStatus(messageId, status.name)
        repositoryScope.launch {
            try {
                if (status == MessageStatus.FCM_RECEIVED || status == MessageStatus.DELIVERED || status == MessageStatus.SEEN) {
                    if (status == MessageStatus.SEEN && !userPrefsRepo.userPreferencesFlow.first().readReceiptsEnabled) return@launch
                    val supabase = getSupabase()
                    supabase.postgrest["messages"].update(buildJsonObject { put("status", status.name) }) { filter { eq("id", messageId) } }
                    messageDao.getMessageById(messageId)?.let { supabase.postgrest["chats"].update(buildJsonObject { put("last_message_status", status.name) }) { filter { eq("id", it.chatId) } } }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update message status remotely", e)
            }
        }
        if (status != MessageStatus.SEEN) pendingStatusUpdates.trySend(messageId to status)
        else repositoryScope.launch { if (userPrefsRepo.userPreferencesFlow.first().readReceiptsEnabled) pendingStatusUpdates.trySend(messageId to status) }
    }

    override suspend fun togglePinChat(chatId: String, isPinned: Boolean) { chatDao.updatePinned(chatId, isPinned) }
    override suspend fun toggleFavorite(chatId: String, isFavorite: Boolean) { chatDao.updateFavorite(chatId, isFavorite) }
    override suspend fun toggleMuteChat(chatId: String, isMuted: Boolean) { chatDao.updateMuted(chatId, isMuted) }
    override suspend fun archiveChat(chatId: String, isArchived: Boolean) { chatDao.updateArchived(chatId, isArchived) }
    override suspend fun blockUser(userId: String, isBlocked: Boolean) { userDao.updateBlocked(userId, isBlocked) }
    override suspend fun clearChat(chatId: String) {
        val currentUserId = getCurrentUserId() ?: return
        repositoryScope.launch {
            try {
                // 1. Find all messages in this chat with attachments
                // Note: Simplified logic, ideally we'd have a Query to get all FileIds for a ChatId
                val messages = messageDao.getLatestMessagesOnce(chatId, Int.MAX_VALUE)
                val referenceManager = AppModule.provideFileReferenceManager(context)
                
                messages.forEach { msg ->
                    val attachments = fileDao.getFilesForMessage(msg.id)
                    attachments.forEach { file ->
                        referenceManager.removeReference(file.id)
                    }
                }
                
                messageDao.deleteMessagesForChat(chatId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear chat", e)
            }
        }
    }
    override suspend fun deleteChat(chatId: String) { chatDao.deleteChat(chatId) }
    override suspend fun updateChatTheme(chatId: String, themeId: String?) {
        chatDao.updateThemeId(chatId, themeId)
        try { val userId = getCurrentUserId() ?: return; getSupabase().postgrest["chat_members"].update({ set("theme_id", themeId) }) { filter { eq("chat_id", chatId); eq("user_id", userId) } } } catch (e: Exception) {}
    }

    override suspend fun deleteMessage(messageId: String) { 
        try { 
            // 1. Get attachments to remove references
            val attachments = fileDao.getFilesForMessage(messageId)
            
            // 2. Delete remote and local message
            getSupabase().postgrest["messages"].delete { filter { eq("id", messageId) } }
            messageDao.deleteMessageById(messageId)
            messageDao.deleteReactionsForMessage(messageId)
            
            // 3. Decrement reference counts
            val referenceManager = AppModule.provideFileReferenceManager(context)
            attachments.forEach { file ->
                referenceManager.removeReference(file.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message", e)
        } 
    }
    override suspend fun setAutoDeleteTimer(chatId: String, timer: Long?) { chatDao.updateAutoDeleteTimer(chatId, timer) }
    override suspend fun toggleAutoTranslate(chatId: String, enabled: Boolean) { chatDao.updateAutoTranslate(chatId, enabled) }
    override suspend fun updateChatNotificationSettings(chatId: String, settings: ChatNotificationSettings) { chatDao.updateNotificationSettings(chatId, Json.encodeToString(settings)) }
    override suspend fun translateMessage(messageId: String) {
        val msg = messageDao.getMessageById(messageId) ?: return
        if (msg.type != "TEXT") return
        try {
            val tManager = getTranslationManager()
            val sourceLang = tManager.detectLanguage(msg.content)
            if (sourceLang != null) { 
                val translated = tManager.translate(msg.content, sourceLang)
                if (translated != null && translated != msg.content) {
                    messageDao.updateMessageTranslation(messageId, translated, sourceLang) 
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to translate message", e)
        }
    }

    override suspend fun preloadTranslationModel(langTag: String) { getTranslationManager().preloadModel(langTag) }
    override suspend fun updateMessageTranslation(messageId: String, translatedContent: String, sourceLanguage: String) { messageDao.updateMessageTranslation(messageId, translatedContent, sourceLanguage) }
    override suspend fun exportChat(chatId: String): String = ""
    override fun getTypingStatus(chatId: String): Flow<Set<String>> = typingUsers.map { it[chatId] ?: emptySet() }
    override fun getLastSeenMessageId(chatId: String, senderId: String): Flow<String?> = messageDao.getLastSeenMessageIdFlow(chatId, senderId)
    override fun getAllTypingStatuses(): Flow<Map<String, Set<String>>> = typingUsers
    override suspend fun setTypingStatus(chatId: String, isTyping: Boolean) {
        val currentUserId = getCurrentUserId() ?: return
        try { 
            if (userPrefsRepo.userPreferencesFlow.first().typingIndicatorsEnabled) {
                val channel = messagesChannel ?: getSupabase().realtime.channel("public:messages").apply { subscribe() }.also { messagesChannel = it }
                channel.broadcast<TypingBroadcast>("typing", TypingBroadcast(chatId, currentUserId, isTyping)) 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast typing status", e)
        }
    }

    override suspend fun startCall(chatId: String, type: String, callId: String): String {
        val currentUserId = getCurrentUserId() ?: throw Exception("Unauthorized")
        val chat = chatDao.getChatById(chatId) ?: throw Exception("Chat not found")
        try {
            getSupabase().postgrest["call_sessions"].insert(buildJsonObject { put("id", callId); put("chat_id", chatId); put("caller_id", currentUserId); put("receiver_id", chat.peerId ?: ""); put("type", type); put("state", "calling") })
            sendMessage(chatId, "Started a ${if (type.uppercase() == "VIDEO") "video call" else "voice call"}", MessageType.CALL_LOG)
        } catch (e: Exception) { throw e }
        return callId
    }

    override suspend fun acceptCall(callId: String) { updateCallState(callId, CallState.CONNECTING) }
    override suspend fun endCall(callId: String) {
        val session = getSupabase().postgrest["call_sessions"].select { filter { eq("id", callId) } }.decodeSingleOrNull<JsonObject>()
        if (session != null) {
            val state = session["state"]?.jsonPrimitive?.content; val chatId = session["chat_id"]?.jsonPrimitive?.content; val callType = session["type"]?.jsonPrimitive?.content ?: "voice"
            if (state == "calling" || state == "ringing") chatId?.let { sendMessage(it, "Missed ${if (callType.uppercase() == "VIDEO") "video call" else "voice call"}", MessageType.CALL_LOG) }
        }
        updateCallState(callId, CallState.ENDED)
    }

    override suspend fun updateCallState(callId: String, state: CallState) {
        val dbState = when (state) { CallState.IDLE -> "idle"; CallState.OUTGOING_RINGING -> "calling"; CallState.INCOMING_RINGING -> "calling"; CallState.CONNECTING -> "ringing"; CallState.ACTIVE_CALL -> "connected"; CallState.RECONNECTING -> "connected"; CallState.ENDED -> "ended" }
        try { getSupabase().postgrest["call_sessions"].update({ set("state", dbState); if (state == CallState.ENDED) set("ended_at", formatToIso(System.currentTimeMillis())) }) { filter { eq("id", callId) } } } catch (e: Exception) {}
    }

    override suspend fun sendCallSignaling(callId: String, type: String, payload: String) {
        val currentUserId = getCurrentUserId() ?: return
        val dbType = when(type.uppercase()) { "OFFER" -> "offer"; "ANSWER" -> "answer"; "ICE_CANDIDATE" -> "ice-candidate"; else -> type.lowercase() }
        val jsonPayload = try { Json.parseToJsonElement(payload) } catch (e: Exception) { JsonPrimitive(payload) }
        try { getSupabase().postgrest["call_signaling"].insert(buildJsonObject { put("call_id", callId); put("sender_id", currentUserId); put("type", dbType); put("payload", jsonPayload) }) } catch (e: Exception) {}
    }

    override fun getCallSignaling(callId: String): Flow<CallSignaling> = flow {
        val supabase = getSupabase()
        val freshChannel = supabase.realtime.channel("signaling_$callId")
        val signalingFlow = callbackFlow {
            fun handleSignaling(data: JsonObject) {
                val dbType = data["type"]?.jsonPrimitive?.content ?: ""
                val type = when(dbType) { "offer" -> "OFFER"; "answer" -> "ANSWER"; "ice-candidate" -> "ICE_CANDIDATE"; else -> dbType.uppercase() }
                val payloadString = when (val p = data["payload"]) { is JsonObject, is JsonArray -> p.toString(); is JsonPrimitive -> p.content; else -> p?.toString() ?: "" }
                trySend(CallSignaling(data["id"]?.jsonPrimitive?.content ?: "", callId, data["sender_id"]?.jsonPrimitive?.content ?: "", type, payloadString))
            }
            launch { try { supabase.postgrest["call_signaling"].select { filter { eq("call_id", callId) } }.decodeList<JsonObject>().forEach { handleSignaling(it) } } catch (e: Exception) {} }
            freshChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "call_signaling" }.filter { it.record["call_id"]?.jsonPrimitive?.content == callId }.onEach { handleSignaling(it.record) }.launchIn(this)
            launch { freshChannel.subscribe() }
            awaitClose { launch { try { freshChannel.unsubscribe(); supabase.realtime.removeChannel(freshChannel) } catch (_: Exception) {} } }
        }
        emitAll(signalingFlow)
    }

    override fun getCallSession(callId: String): Flow<CallSession?> = flow {
        val supabase = getSupabase()
        val freshChannel = supabase.realtime.channel("session_$callId")
        val currentUserId = getCurrentUserId()
        val sessionFlow = callbackFlow {
            suspend fun fetch() { try { supabase.postgrest["call_sessions"].select { filter { eq("id", callId) } }.decodeSingleOrNull<JsonObject>()?.let { val dbState = it["state"]!!.jsonPrimitive.content; val callerId = it["caller_id"]!!.jsonPrimitive.content; val state = if (dbState == "calling" && callerId != currentUserId) CallState.INCOMING_RINGING else dbState.toCallState(); trySend(CallSession(it["id"]!!.jsonPrimitive.content, it["chat_id"]!!.jsonPrimitive.content, it["caller_id"]!!.jsonPrimitive.content, it["receiver_id"]!!.jsonPrimitive.content, it["type"]!!.jsonPrimitive.content, state, 0L)) } } catch (e: Exception) {} }
            launch { fetch() }; freshChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") { table = "call_sessions" }.filter { it.record["id"]?.jsonPrimitive?.content == callId }.onEach { fetch() }.launchIn(this)
            launch { freshChannel.subscribe() }
            awaitClose { launch { try { freshChannel.unsubscribe(); supabase.realtime.removeChannel(freshChannel) } catch (_: Exception) {} } }
        }
        emitAll(sessionFlow)
    }

    override fun getIncomingCalls(): Flow<CallSession> = flow {
        val currentUserId = getCurrentUserId() ?: return@flow
        val supabase = getSupabase()
        val channelId = "incoming_calls_${currentUserId}_${java.util.UUID.randomUUID().toString().take(8)}"
        val freshChannel = supabase.realtime.channel(channelId)
        val callFlow = callbackFlow {
            suspend fun fetchActiveCalls() { try { supabase.postgrest["call_sessions"].select { filter { eq("receiver_id", currentUserId); or { eq("state", "calling"); eq("state", "ringing") } } }.decodeList<JsonObject>().forEach { data -> trySend(CallSession(data["id"]!!.jsonPrimitive.content, data["chat_id"]!!.jsonPrimitive.content, data["caller_id"]!!.jsonPrimitive.content, data["receiver_id"]!!.jsonPrimitive.content, data["type"]!!.jsonPrimitive.content, CallState.INCOMING_RINGING, 0L)) } } catch (e: Exception) {} }
            launch { fetchActiveCalls() }
            freshChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "call_sessions" }.filter { it.record["receiver_id"]?.jsonPrimitive?.content == currentUserId && (it.record["state"]?.jsonPrimitive?.content == "calling" || it.record["state"]?.jsonPrimitive?.content == "ringing") }.onEach { action ->
                val data = action.record; val callId = data["id"]!!.jsonPrimitive.content; val chatId = data["chat_id"]!!.jsonPrimitive.content; val type = data["type"]!!.jsonPrimitive.content
                sendMessage(chatId, "Incoming ${if (type.uppercase() == "VIDEO") "video call" else "voice call"}", MessageType.CALL_LOG)
                launch {
                    val caller = userDao.getUserById(data["caller_id"]!!.jsonPrimitive.content)
                    insertNotification(AppNotification(
                        id = UUID.randomUUID().toString(),
                        type = NotificationType.MESSAGE,
                        priority = NotificationPriority.NORMAL,
                        title = "Missed Call",
                        message = "Incoming ${if (type.uppercase() == "VIDEO") "video call" else "voice call"} from ${caller?.name ?: "Unknown"}",
                        timestamp = System.currentTimeMillis(),
                        data = NotificationData(chatId = chatId, userId = data["caller_id"]?.jsonPrimitive?.content)
                    ))
                }
                trySend(CallSession(callId, chatId, data["caller_id"]!!.jsonPrimitive.content, data["receiver_id"]!!.jsonPrimitive.content, type, CallState.INCOMING_RINGING, 0L))
            }.launchIn(this)
            freshChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") { table = "call_sessions" }.filter { it.record["receiver_id"]?.jsonPrimitive?.content == currentUserId && (it.record["state"]?.jsonPrimitive?.content == "calling" || it.record["state"]?.jsonPrimitive?.content == "ringing") }.onEach { action -> val data = action.record; trySend(CallSession(data["id"]!!.jsonPrimitive.content, data["chat_id"]!!.jsonPrimitive.content, data["caller_id"]!!.jsonPrimitive.content, data["receiver_id"]!!.jsonPrimitive.content, data["type"]!!.jsonPrimitive.content, CallState.INCOMING_RINGING, 0L)) }.launchIn(this)
            freshChannel.subscribe()
            awaitClose { launch { try { freshChannel.unsubscribe(); supabase.realtime.removeChannel(freshChannel) } catch (_: Exception) {} } }
        }
        emitAll(callFlow)
    }

    private fun String.toCallState(): CallState = when (this.lowercase()) { "calling" -> CallState.OUTGOING_RINGING; "ringing" -> CallState.CONNECTING; "connected" -> CallState.ACTIVE_CALL; "ended" -> CallState.ENDED; else -> CallState.IDLE }
    private val notificationSettingsCache = mutableMapOf<String, ChatNotificationSettings>()
    private val messageStatusCache = mutableMapOf<String, MessageStatus?>()
    private fun getCachedMessageStatus(status: String?): MessageStatus? { if (status == null) return null; return messageStatusCache.getOrPut(status.uppercase()) { try { MessageStatus.valueOf(status.uppercase()) } catch (e: Exception) { null } } }

    private fun ChatEntity.toDomainInternal(): Chat {
        val settings = notificationSettingsJson?.let { json -> 
            notificationSettingsCache.getOrPut(json) { 
                try { 
                    Json.decodeFromString<ChatNotificationSettings>(json) 
                } catch (e: Exception) { 
                    Log.e(TAG, "Failed to decode notification settings", e)
                    ChatNotificationSettings() 
                } 
            } 
        } ?: ChatNotificationSettings()
        
        val threadMetadata = com.keeftalk.chat.util.ThreadMetadata.fromBlob(metadata)
        
        // Use lastDecryptedMessage for UI preview
        val displayLastMessage = lastDecryptedMessage ?: if (lastMessage?.startsWith("[Encrypted]") == true) "New message" else lastMessage

        return Chat(id, name, avatarUrl, try { ChatType.valueOf(type) } catch (e: Exception) { ChatType.ONE_TO_ONE }, displayLastMessage, lastTimestamp, unreadCount, isPinned, isMuted, isArchived, isFavorite, autoDeleteTimer, containerId, peerId, getCachedMessageStatus(lastMessageStatus), lastMessageSenderId, autoTranslateEnabled, settings, themeId, snippetType, snippetUri, threadMetadata.latestAuthorName)
    }

    private fun formatToIso(timestamp: Long): String = TimestampSerializer.formatTimestamp(timestamp)

    private fun MessageEntity.toDomain(attachments: List<FileEntity> = emptyList()): Message {
        val rawType = this.type
        val effectiveType = try { MessageType.valueOf(rawType) } catch (e: Exception) { MessageType.TEXT }

        var content = this.content
        if (this.decryptionState != com.keeftalk.chat.domain.model.DecryptionState.SUCCESS) {
            content = "" // UI filters these out via DAO, but be safe for direct model usage
        }

        return Message(
            id = this.id,
            chatId = this.chatId,
            senderId = this.senderId,
            content = content,
            timestamp = this.timestamp,
            status = try { MessageStatus.valueOf(this.status) } catch (e: Exception) { MessageStatus.SENT },
            type = effectiveType,
            reactions = emptyList(),
            replyToId = this.replyToId,
            translatedContent = this.translatedContent,
            sourceLanguage = this.sourceLanguage,
            mediaLocked = this.mediaLocked,
            mediaLockUpdatedAt = this.mediaLockUpdatedAt,
            mediaLockUpdatedBy = this.mediaLockUpdatedBy,
            cryptoVersion = this.cryptoVersion,
            decryptionState = this.decryptionState,
            attachments = attachments.map { it.toDomain() }
        )
    }

    private fun com.keeftalk.chat.data.local.dao.MessageWithReactions.toDomainInternal(): Message {
        return message.toDomain(attachments).copy(
            reactions = reactions.map { MessageReaction(it.userId, it.emoji) },
            replyTo = replyTo?.toDomain()
        )
    }

    override suspend fun addReaction(messageId: String, emoji: String) {
        val userId = getCurrentUserId() ?: return
        messageDao.insertReaction(MessageReactionEntity(messageId, userId, emoji))
        try { 
            getSupabase().postgrest["message_reactions"].upsert(buildJsonObject { put("message_id", messageId); put("user_id", userId); put("emoji", emoji) }) 
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add reaction remotely", e)
        }
    }
    override suspend fun removeReaction(messageId: String) {
        val userId = getCurrentUserId() ?: return
        messageDao.deleteReaction(messageId, userId)
        try { 
            getSupabase().postgrest["message_reactions"].delete { filter { eq("message_id", messageId); eq("user_id", userId) } } 
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove reaction remotely", e)
        }
    }

    override suspend fun toggleMediaLock(messageId: String, locked: Boolean) {
        val currentUserId = getCurrentUserId() ?: return
        val now = System.currentTimeMillis()
        
        // Update local
        messageDao.updateMediaLockStatus(messageId, locked, now, currentUserId)
        
        // Update remote
        repositoryScope.launch {
            try {
                getSupabase().postgrest["messages"].update(buildJsonObject {
                    put("media_locked", locked)
                    put("media_lock_updated_at", now)
                    put("media_lock_updated_by", currentUserId)
                }) {
                    filter { eq("id", messageId) }
                }
            } catch (e: Exception) {
                Log.e("ChatRepositoryImpl", "Failed to toggle media lock remotely", e)
            }
        }
    }

    override suspend fun downloadMedia(message: Message, onProgress: ((Float) -> Unit)?): Result<DownloadResult> = withContext(Dispatchers.IO) {
        try {
            // 0. Check lock
            val currentUserId = getCurrentUserId()
            if (message.mediaLocked && message.senderId != currentUserId) {
                return@withContext Result.failure(Exception("File is locked by sender"))
            }

            // 1. Ensure we have the decrypted file in internal storage
            val localFileResult = ensureMediaLocal(message)
            if (localFileResult.isFailure) return@withContext Result.failure(localFileResult.exceptionOrNull() ?: Exception("Local prep failed"))
            
            val decryptedFile = localFileResult.getOrThrow()
            
            // 2. Prepare the destination in structured Keeftalk folder
            val subFolder = when(message.type) {
                MessageType.IMAGE -> "Picture"
                MessageType.VIDEO -> "Video"
                else -> "files"
            }
            
            val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Keeftalk/$subFolder")
            if (!downloadDir.exists()) downloadDir.mkdirs()
            
            val attachment = message.attachments.firstOrNull()
            val fileName = attachment?.fileName ?: decryptedFile.name
            val targetFile = File(downloadDir, fileName)
            
            if (targetFile.exists()) {
                // Return success immediately if already exists in target location
                return@withContext Result.success(DownloadResult(targetFile, true))
            }
            
            // 3. Copy the decrypted file to public storage
            decryptedFile.copyTo(targetFile, overwrite = true)
            
            Result.success(DownloadResult(targetFile, false))
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Result.failure(e)
        }
    }

    override suspend fun ensureMediaLocal(message: Message): Result<File> = withContext(Dispatchers.IO) {
        val attachment = message.attachments.firstOrNull() ?: return@withContext Result.failure(Exception("No attachments"))
        val fileRepo = AppModule.provideFileRepository(context)
        fileRepo.ensureMediaLocal(attachment)
    }

    override fun shutdown() {
        Log.i(TAG, "Shutting down ChatRepository")
        repositoryScope.cancel()
        incomingMessagesChannel.close()
        pendingStatusUpdates.close()
    }

    override suspend fun bumpChat(chatId: String) { chatDao.getChatById(chatId)?.let { chatDao.insertChat(it.copy(lastTimestamp = System.currentTimeMillis())) } }
    override suspend fun forwardMessage(messageId: String, targetChatIds: List<String>) {
        val msgEntity = messageDao.getMessageById(messageId) ?: return
        val attachments = fileDao.getFilesForMessage(messageId)
        
        // 1. Decrypt source message to get FEK
        val sourceEnvelope = EncryptedMessageEnvelope(
            type = msgEntity.envelopeType,
            ciphertext = java.util.Base64.getDecoder().decode(msgEntity.ciphertext!!),
            nonce = msgEntity.nonce
        )
        val decryptedSource = cryptoManager.decryptMessage(msgEntity.chatId, sourceEnvelope, msgEntity.cryptoVersion)
        val sourceJson = Json.parseToJsonElement(decryptedSource).jsonObject
        
        val wrappedFekJson = sourceJson["wrappedFek"]?.jsonPrimitive?.content
        val mediaIv = sourceJson["mediaIv"]?.jsonPrimitive?.content
        
        if (wrappedFekJson != null && mediaIv != null) {
            // Unwrapping FEK from source context
            val sourcePck = AppModule.provideConversationKeyManager(context).getOrLoadKey(msgEntity.chatId)!!
            val wrappedFekObj = Json.decodeFromString<EncryptedObject>(wrappedFekJson)
            val fek = StorageCryptoService.unwrapKey(wrappedFekObj, sourcePck)
            
            // 2. Re-wrap and send to each target
            targetChatIds.forEach { targetChatId ->
                repositoryScope.launch {
                    val targetPck = AppModule.provideConversationKeyManager(context).getOrLoadKey(targetChatId) 
                        ?: AppModule.provideConversationKeyManager(context).createKey(targetChatId)
                    
                    val newWrappedFek = StorageCryptoService.wrapKey(fek, targetPck)
                    val newContent = buildJsonObject {
                        put("text", sourceJson["text"]?.jsonPrimitive?.content ?: "")
                        put("wrappedFek", Json.encodeToString(EncryptedObject.serializer(), newWrappedFek))
                        put("mediaIv", mediaIv)
                    }.toString()
                    
                    sendMessage(targetChatId, newContent, MessageType.valueOf(msgEntity.type))
                }
            }
        } else {
            // Forwarding simple text
            targetChatIds.forEach { targetChatId ->
                sendMessage(targetChatId, msgEntity.content, MessageType.valueOf(msgEntity.type))
            }
        }
    }
    override suspend fun reportMessage(messageId: String, reason: String) { try { getSupabase().postgrest["message_reports"].insert(buildJsonObject { put("message_id", messageId); put("reason", reason); put("reporter_id", getCurrentUserId()) }) } catch (_: Exception) {} }
    override fun getNotifications(): Flow<List<AppNotification>> = getAppNotifications()
    override fun getAppNotifications(): Flow<List<AppNotification>> = notificationDao.getAllNotifications().map { entities -> entities.map { it.toDomain() } }
    override fun getUnreadNotificationCount(): Flow<Int> = notificationDao.getUnreadCount()
    override suspend fun insertNotification(notification: AppNotification) { notificationDao.insertNotification(NotificationEntity.fromDomain(notification)) }
    override suspend fun pushNotification(recipientId: String, notification: AppNotification) {}
    override suspend fun markNotificationAsRead(id: String) { notificationDao.markAsRead(id) }
    override suspend fun markAllNotificationsAsRead() { notificationDao.markAllAsRead() }
    override suspend fun deleteNotification(id: String) { notificationDao.deleteNotification(id) }
    override suspend fun clearAllNotifications() { notificationDao.clearAll() }
    override suspend fun syncNotifications() = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext
            val remote = getSupabase().postgrest["notifications"].select {
                filter {
                    eq("user_id", userId)
                }
                order("created_at", order = Order.DESCENDING)
                limit(20)
            }.decodeList<AppNotification>()
            notificationDao.insertNotifications(remote.map { NotificationEntity.fromDomain(it) })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync notifications", e)
        }
    }
    override suspend fun refreshChatMetadata(chatIds: List<String>) { chatIds.forEach { fetchAndCacheChatInfo(it) } }
    override suspend fun markAsRead(chatId: String) = withContext(databaseWriteDispatcher) {
        val currentUserId = getCurrentUserId() ?: return@withContext
        try {
            val latestPeerMessage = messageDao.getLatestPeerMessage(chatId, currentUserId)
            
            // 1. Mark ALL received messages as read locally for immediate UI update
            messageDao.markMessagesAsRead(chatId, currentUserId)
            
            // 2. Reset unread count for this chat
            chatDao.resetUnreadCount(chatId)
            
            // 3. Update the global read pointer if there's a peer message
            if (latestPeerMessage != null && userPrefsRepo.userPreferencesFlow.first().readReceiptsEnabled) {
                sendReadReceipt(chatId, latestPeerMessage.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "markAsRead failed for chat $chatId", e)
        }
    }

    override suspend fun sendReadReceipt(chatId: String, messageId: String) {
        val currentUserId = getCurrentUserId() ?: return
        val message = messageDao.getMessageById(messageId) ?: return
        
        // --- FORWARD-ONLY GUARD ---
        val currentMember = chatDao.getChatMember(chatId, currentUserId)
        if (currentMember != null && (currentMember.lastReadAt ?: 0L) >= message.timestamp) {
            return // Pointer is already at or beyond this message
        }
        
        // 1. Local update
        chatDao.updateReadPointer(chatId, currentUserId, messageId, message.timestamp)
        
        // 2. Queue for remote
        receiptSyncQueueDao.enqueueReceipt(ReceiptSyncQueueEntity(
            chatId = chatId,
            messageId = messageId,
            type = "READ",
            timestamp = message.timestamp
        ))
        
        // 3. Trigger flush
        repositoryScope.launch { flushReceiptSyncQueue() }
    }

    override suspend fun sendDeliveryReceipt(chatId: String, messageId: String) {
        val currentUserId = getCurrentUserId() ?: return
        val message = messageDao.getMessageById(messageId) ?: return

        // --- FORWARD-ONLY GUARD ---
        val currentMember = chatDao.getChatMember(chatId, currentUserId)
        if (currentMember != null && (currentMember.lastDeliveredAt ?: 0L) >= message.timestamp) {
            return
        }

        // 1. Local update
        chatDao.updateDeliveryPointer(chatId, currentUserId, messageId, message.timestamp)

        // 2. Queue for remote
        receiptSyncQueueDao.enqueueReceipt(ReceiptSyncQueueEntity(
            chatId = chatId,
            messageId = messageId,
            type = "DELIVERED",
            timestamp = message.timestamp
        ))

        // 3. Trigger flush
        repositoryScope.launch { flushReceiptSyncQueue() }
    }

    override fun getChatMembersFlow(chatId: String): Flow<List<ChatMemberEntity>> = 
        chatDao.getChatMembersFlow(chatId)

    private val receiptSyncMutex = kotlinx.coroutines.sync.Mutex()
    
    private suspend fun flushReceiptSyncQueue() {
        receiptSyncMutex.withLock {
            val pending = receiptSyncQueueDao.getPendingReceipts()
            if (pending.isEmpty()) return
            
            val supabase = getSupabase()
            val currentUserId = getCurrentUserId() ?: return
            
            pending.groupBy { it.chatId to it.type }.forEach { (key, receipts) ->
                val (chatId, type) = key
                val highest = receipts.maxByOrNull { it.timestamp } ?: return@forEach
                
                try {
                    val column = if (type == "READ") "last_read_message_id" else "last_delivered_message_id"
                    val timeColumn = if (type == "READ") "last_read_at" else "last_delivered_at"
                    
                    supabase.postgrest["chat_members"].update(buildJsonObject {
                        put(column, highest.messageId)
                        put(timeColumn, formatToIso(highest.timestamp))
                    }) {
                        filter {
                            eq("chat_id", chatId)
                            eq("user_id", currentUserId)
                        }
                    }
                    
                    receipts.forEach { receiptSyncQueueDao.markAsCompleted(it.id) }
                    receiptSyncQueueDao.removeObsoleteReceipts(chatId, type, highest.timestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to flush receipt: $type for chat $chatId", e)
                }
            }
            receiptSyncQueueDao.clearCompleted()
        }
    }

    override fun getSearchableProfiles(): Flow<List<Profile>> = profileDao.getAllProfiles().map { entities -> entities.map { it.toDomain() } }
    
    override suspend fun syncContacts() {
        val currentUserId = getCurrentUserId() ?: return
        val supabase = getSupabase()

        try {
            // 1. Pull from Supabase
            val remoteContacts = supabase.postgrest["contacts"]
                .select {
                    filter { eq("owner_id", currentUserId) }
                }
                .decodeList<ContactDto>()

            remoteContacts.forEach { dto ->
                val entity = com.keeftalk.chat.data.local.entities.UserEntity(
                    id = dto.id ?: return@forEach,
                    name = dto.name,
                    username = dto.username ?: "",
                    phone = dto.phone,
                    avatarUrl = dto.avatarUrl,
                    isActive = dto.isKeeftalkUser,
                    isContact = true,
                    secondaryPhone = dto.secondaryPhone,
                    secondaryPhoneLabel = dto.secondaryPhoneLabel,
                    tertiaryPhone = dto.tertiaryPhone,
                    tertiaryPhoneLabel = dto.tertiaryPhoneLabel,
                    email = dto.email,
                    birthday = dto.birthday,
                    callingCard = dto.callingCard,
                    cloudSyncStatus = 1 // Synced
                )
                userDao.insertUser(entity)
            }

            // 2. Push local unsynced contacts
            val unsynced = userDao.getUnsyncedContacts()
            unsynced.forEach { local ->
                val dto = ContactDto(
                    name = local.name,
                    username = local.username,
                    phone = local.phone,
                    email = local.email,
                    avatarUrl = local.avatarUrl,
                    secondaryPhone = local.secondaryPhone,
                    secondaryPhoneLabel = local.secondaryPhoneLabel,
                    tertiaryPhone = local.tertiaryPhone,
                    tertiaryPhoneLabel = local.tertiaryPhoneLabel,
                    birthday = local.birthday,
                    callingCard = local.callingCard,
                    isKeeftalkUser = local.isActive,
                    ownerId = currentUserId
                )
                
                try {
                    val response = supabase.postgrest["contacts"].insert(dto) {
                        select()
                    }.decodeSingle<ContactDto>()
                    
                    // Update local with remote ID and sync status
                    userDao.updateSyncStatus(local.id, 1)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to push contact: ${local.name}", e)
                }
            }

            // 3. Platform Discovery: Check which contacts are Keeftalk users
            val allLocalContacts = userDao.getContacts().first()
            val phoneToContactMap = allLocalContacts.filter { !it.phone.isNullOrEmpty() }
                .associateBy { it.phone!! }
            
            val allPhoneNumbers = phoneToContactMap.keys.toList()
            
            if (allPhoneNumbers.isNotEmpty()) {
                // Bulk discovery (up to 100 at a time to avoid URL length limits or query constraints)
                allPhoneNumbers.chunked(100).forEach { chunk ->
                    val registeredProfiles = supabase.postgrest["profiles"]
                        .select {
                            filter { 
                                isIn("phone", chunk)
                            }
                        }.decodeList<com.keeftalk.chat.domain.model.Profile>()
                    
                    registeredProfiles.forEach { profile ->
                        val contact = phoneToContactMap[profile.phone]
                        if (contact != null) {
                            userDao.insertUser(contact.copy(
                                isActive = true,
                                avatarUrl = profile.avatarUrl ?: contact.avatarUrl,
                                username = profile.username
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync contacts failed", e)
            throw e
        }
    }

    override suspend fun syncCallLogs() {
        val currentUserId = getCurrentUserId() ?: return
        val supabase = getSupabase()

        try {
            // 1. Push local unsynced logs to Supabase
            val unsynced = callLogDao.getUnsyncedCallLogs()
            unsynced.forEach { entity ->
                val dto = entity.toDto(currentUserId)
                try {
                    supabase.postgrest["call_logs"].insert(dto)
                    callLogDao.updateSyncStatus(entity.id, 1) // Mark as synced
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync call log ${entity.id}", e)
                }
            }

            // 2. Optionally pull from Supabase (For multi-device sync)
            val remoteLogs = supabase.postgrest["call_logs"]
                .select {
                    filter { eq("user_id", currentUserId) }
                    order("timestamp", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(100)
                }
                .decodeList<CallLogDto>()

            // Update local DB with remote logs
            val entities = remoteLogs.map { it.toEntity(1) }
            if (entities.isNotEmpty()) {
                callLogDao.insertCallLogs(entities)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Sync call logs failed", e)
        }
    }

    override suspend fun warmRecipientCache(userIds: List<String>) { 
        userIds.forEach { id -> 
            userDao.getUserById(id)?.let { 
                com.keeftalk.chat.util.LiveRecipientCache.put(it.toDomain()) 
            } 
        } 
    }

    override suspend fun saveLocalContact(
        name: String, 
        phone: String,
        secondaryPhone: String?,
        secondaryPhoneLabel: String?,
        tertiaryPhone: String?,
        tertiaryPhoneLabel: String?,
        email: String?,
        birthday: Long?,
        callingCard: String?,
        avatarUrl: String?
    ) {
        val id = "local_${java.util.UUID.randomUUID()}"
        val user = com.keeftalk.chat.data.local.entities.UserEntity(
            id = id,
            name = name,
            username = name.lowercase().replace(" ", "_") + "_local",
            phone = phone,
            secondaryPhone = secondaryPhone,
            secondaryPhoneLabel = secondaryPhoneLabel,
            tertiaryPhone = tertiaryPhone,
            tertiaryPhoneLabel = tertiaryPhoneLabel,
            email = email,
            birthday = birthday,
            callingCard = callingCard,
            avatarUrl = avatarUrl,
            isContact = true,
            isActive = false,
            cloudSyncStatus = 0 // Local only
        )
        userDao.insertUser(user)
    }

    override suspend fun shareEmailToChat(emailMessageId: String, chatId: String) {
        val emailRepo = com.keeftalk.chat.di.AppModule.provideEmailRepository(context)
        emailRepo.markEmailAsShared(emailMessageId)
        val email = emailRepo.getMessage(emailMessageId).firstOrNull()
        val title = email?.subject ?: "Shared an email"
        sendMessage(chatId, title, com.keeftalk.chat.domain.model.MessageType.SHARED_EMAIL, filePath = emailMessageId)
    }

    override suspend fun shareVaultFileToChat(vaultItemId: String, chatId: String) {
        val vaultRepo = com.keeftalk.chat.di.AppModule.provideVaultRepository(context)
        vaultRepo.markItemAsShared(vaultItemId)
        val entity = vaultRepo.getItems(null).first().find { it.id == vaultItemId }
        val title = entity?.title ?: "Shared a vault file"
        sendMessage(chatId, title, com.keeftalk.chat.domain.model.MessageType.SHARED_VAULT_FILE, filePath = vaultItemId)
    }

    override suspend fun shareAgendaToChat(calendarItemId: String, chatId: String) {
        val calendarRepo = com.keeftalk.chat.di.AppModule.provideCalendarRepository(context)
        val item = calendarRepo.getItemById(calendarItemId)
        
        if (item != null) {
            val members = getChatMembers(chatId).first()
            val currentUserId = userPrefsRepo.getUserIdFast() ?: ""
            members.forEach { member ->
                if (member.id != currentUserId) {
                    calendarRepo.addAttendee(calendarItemId, member.id, com.keeftalk.chat.domain.model.calendar.AttendeeRole.VIEWER)
                }
            }
        }
        
        val title = item?.title ?: "Shared an agenda item"
        sendMessage(chatId, title, com.keeftalk.chat.domain.model.MessageType.SHARED_AGENDA, filePath = calendarItemId)
    }

    override suspend fun shareNoteToChat(noteId: String, chatId: String, accessLevel: String, canInviteOthers: Boolean) {
        val members = getChatMembers(chatId).first()
        val currentUserId = userPrefsRepo.getUserIdFast() ?: ""
        val noteRepo = com.keeftalk.chat.di.AppModule.provideNoteRepository(context)

        members.forEach { member ->
            if (member.id != currentUserId) {
                noteRepo.addShare(noteId, member.id, accessLevel)
            }
        }

        val note = noteRepo.getNote(noteId).first()
        note?.let {
            if (it.canOthersAdd != canInviteOthers) {
                noteRepo.saveNote(it.copy(canOthersAdd = canInviteOthers))
            }
        }

        val title = note?.title ?: "Shared a note"
        sendMessage(chatId, title, com.keeftalk.chat.domain.model.MessageType.SHARED_NOTE, filePath = noteId)
    }

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }
}
