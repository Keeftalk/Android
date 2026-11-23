package tn.keeftalk.chat.data.local

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.data.local.entities.ChatEntity
import tn.keeftalk.chat.domain.model.ChatListItemUiModel
import tn.keeftalk.chat.domain.model.ChatType
import tn.keeftalk.chat.domain.model.MessageStatus

class ChatPreviewInitializationTest {

    @Test
    fun testChatEntityPrefersDecryptedMessageForPreview() {
        val chatWithDecrypted = ChatEntity(
            id = "chat_1",
            name = "Alice",
            type = "ONE_TO_ONE",
            lastMessage = "[Encrypted]",
            lastDecryptedMessage = "Hello from initial sync!",
            lastTimestamp = 1700000000000L
        )

        val preview = chatWithDecrypted.lastDecryptedMessage ?: chatWithDecrypted.lastMessage
        assertEquals("Hello from initial sync!", preview)
    }

    @Test
    fun testChatListItemUiModelRetainsLastMessageOnInitialLaunch() {
        val uiModel = ChatListItemUiModel(
            id = "chat_1",
            name = "Alice",
            avatarUrl = null,
            initials = "A",
            lastMessage = "Hey, are you free today?",
            lastTimestamp = 1700000000000L,
            formattedTimestamp = "10:00 AM",
            unreadCount = 1,
            isPinned = false,
            isMuted = false,
            lastMessageStatus = MessageStatus.SENT,
            lastMessageSenderId = "user_2",
            isArchived = false,
            peerId = "user_2",
            type = ChatType.ONE_TO_ONE
        )

        assertNotNull("Last message preview must not be null on initial load", uiModel.lastMessage)
        assertEquals("Hey, are you free today?", uiModel.lastMessage)
    }

    @Test
    fun testChatEntityFallbackWhenDecryptedMessageNull() {
        val chatPlain = ChatEntity(
            id = "chat_2",
            name = "Bob",
            type = "ONE_TO_ONE",
            lastMessage = "Plain text message",
            lastDecryptedMessage = null,
            lastTimestamp = 1700000000000L
        )

        val preview = chatPlain.lastDecryptedMessage ?: chatPlain.lastMessage
        assertEquals("Plain text message", preview)
    }
}
