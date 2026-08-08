package com.keeftalk.chat.ui.discovery

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.ui.theme.KeeftalkDimensions
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.components.KeeftalkTopBar
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.util.AvatarUtils
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    onBack: () -> Unit,
    onChatStarted: (String) -> Unit,
    onProfileClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val icons = LocalAppIcons.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp
            ) {
                Column {
                    KeeftalkTopBar(
                        title = "Discovery",
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(icons.back, contentDescription = "Back", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                            }
                        }
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Find users, groups, or channels...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )

                        LaunchedEffect(Unit) {
                            // Ensure focus is requested only after layout is stable
                            kotlinx.coroutines.delay(400)
                            try {
                                focusRequester.requestFocus()
                            } catch (e: Exception) {
                                Log.w("DiscoveryScreen", "Failed to request focus (likely uninitialized/detached)")
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "DiscoveryState"
            ) { state ->
                Column(modifier = Modifier.fillMaxSize()) {
                    if (state is DiscoveryUiState.Results && state.isSearching) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    }
                    
                    when (state) {
                        is DiscoveryUiState.Idle -> DiscoveryIdleView()
                        is DiscoveryUiState.Loading -> DiscoveryLoadingView()
                        is DiscoveryUiState.Results -> {
                            if (state.users.isEmpty() && !state.isSearching) {
                                DiscoveryNoResultsView()
                            } else {
                                DiscoveryResultsView(state.users, onProfileClick)
                            }
                        }
                        is DiscoveryUiState.Error -> DiscoveryErrorView(state.message)
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveryNoResultsView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No users found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Try searching with a different name or username",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun DiscoveryIdleView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Explore Keeftalk",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Search for users by username or full name",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DiscoveryLoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
fun DiscoveryResultsView(users: List<Profile>, onProfileClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(users, key = { it.id }) { user ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn()
            ) {
                DiscoveryUserItem(user = user, onClick = { onProfileClick(user.id) })
            }
        }
    }
}

@Composable
fun DiscoveryUserItem(user: Profile, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .graphicsLayer { shadowElevation = 2.dp.toPx() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeeftalkAvatar(
                avatarUrl = user.avatarUrl,
                initials = AvatarUtils.getInitials(user.fullName ?: user.username),
                seed = user.id,
                size = 60.dp,
                modifier = Modifier
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName ?: user.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                LocalAppIcons.current.chevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun DiscoveryErrorView(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            LocalAppIcons.current.info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Please check your internet connection and try again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
