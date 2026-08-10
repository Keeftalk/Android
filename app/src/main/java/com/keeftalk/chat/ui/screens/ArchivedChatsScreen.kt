package com.keeftalk.chat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.keeftalk.chat.ui.components.KeeftalkTopBar
import com.keeftalk.chat.ui.theme.KeeftalkDimensions
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedChatsScreen(
    viewModel: ChatListViewModel,
    myId: String,
    onBack: () -> Unit,
    onChatClick: (String) -> Unit,
) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val archivedChats by viewModel.archivedChats.collectAsState()
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            KeeftalkTopBar(
                title = "Archived Chats",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(icons.back, contentDescription = "Back", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        ChatListContent(
            chatsPager = viewModel.archivedChatsPager,
            typingStatuses = viewModel.typingStatuses,
            displayChats = archivedChats,
            currentUserId = myId,
            onChatClick = onChatClick,
            onMenuAction = {},
            onChatAction = { chat, action ->
                when (action) {
                    ChatAction.ARCHIVE -> viewModel.archiveChat(chatId = chat.id, archive = false)
                    ChatAction.DELETE -> viewModel.deleteChat(chat.id)
                    else -> {}
                }
            },
            onLoadMore = { viewModel.loadMoreChats() },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            title = "Archived Chats",
            isRefreshing = isSyncing,
            onRefresh = { viewModel.refresh() },
            showTopBar = true,
        )
    }
}
