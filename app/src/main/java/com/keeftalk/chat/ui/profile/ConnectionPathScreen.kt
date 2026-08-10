package com.keeftalk.chat.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.util.AvatarUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionPathScreen(
    viewModel: ConnectionPathViewModel,
    onBack: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val paths by viewModel.paths.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How you're connected", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            } else if (paths.isEmpty()) {
                Text("No paths found within 4 degrees.", color = Color.White.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    paths.forEachIndexed { index, path ->
                        PathVisualization(
                            path = path, 
                            pathIndex = index + 1,
                            onProfileClick = onProfileClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PathVisualization(path: List<Any>, pathIndex: Int, onProfileClick: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Path #$pathIndex",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Start: You
        NodeItem(name = "You", isMe = true)
        
        path.forEach { node ->
            Connector()
            when (node) {
                is Profile -> NodeItem(
                    name = node.fullName ?: node.username,
                    avatarUrl = node.avatarUrl,
                    seed = node.id,
                    onClick = { onProfileClick(node.id) }
                )
                "masked" -> NodeItem(
                    name = "Private Connection",
                    isMasked = true
                )
                is String -> NodeItem(
                    name = "User $node",
                    seed = node,
                    onClick = { onProfileClick(node) }
                )
            }
        }
    }
}

@Composable
fun NodeItem(
    name: String,
    avatarUrl: String? = null,
    seed: String? = null,
    isMe: Boolean = false,
    isMasked: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.widthIn(min = 240.dp).then(clickableModifier),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMasked) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PrivacyTip, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            } else {
                KeeftalkAvatar(
                    avatarUrl = avatarUrl,
                    initials = if (isMe) "ME" else AvatarUtils.getInitials(name),
                    seed = seed ?: name,
                    size = 40.dp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isMe) MaterialTheme.colorScheme.primary else Color.White
            )
        }
    }
}

@Composable
fun Connector() {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(32.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.1f)
                    )
                )
            )
    )
}
