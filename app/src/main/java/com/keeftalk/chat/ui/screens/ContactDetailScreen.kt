package com.keeftalk.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.components.KeeftalkTopBar
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.theme.KeeftalkDimensions
import com.keeftalk.chat.util.AvatarUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    user: User,
    onBack: () -> Unit,
    onChatClick: () -> Unit,
    onCallClick: () -> Unit,
    onSmsClick: () -> Unit
) {
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            KeeftalkTopBar(
                title = "Contact Info",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(icons.back, contentDescription = "Back", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KeeftalkAvatar(
                    avatarUrl = user.avatarUrl,
                    initials = user.initials,
                    seed = user.id,
                    size = 120.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                user.phone?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ContactActionItem(icon = icons.chat, label = "Chat", onClick = onChatClick)
                ContactActionItem(icon = icons.phone, label = "Call", onClick = onCallClick)
                ContactActionItem(icon = icons.messageSquare, label = "SMS", onClick = onSmsClick)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Info List
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                InfoRow(icon = Icons.Default.Person, label = "Username", value = "@${user.username}")
                
                user.phone?.let {
                    InfoRow(icon = Icons.Default.Phone, label = "Primary Phone", value = it)
                }
                
                user.secondaryPhone?.let {
                    InfoRow(icon = Icons.Default.Phone, label = user.secondaryPhoneLabel ?: "Secondary Phone", value = it)
                }

                user.tertiaryPhone?.let {
                    InfoRow(icon = Icons.Default.Phone, label = user.tertiaryPhoneLabel ?: "Tertiary Phone", value = it)
                }

                user.email?.let {
                    InfoRow(icon = Icons.Default.Email, label = "Email", value = it)
                }

                user.birthday?.let {
                    val date = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                    InfoRow(icon = Icons.Default.Cake, label = "Birthday", value = date)
                }

                user.callingCard?.let {
                    InfoRow(icon = Icons.Default.Description, label = "Calling Card / Notes", value = it)
                }
            }
        }
    }
}

@Composable
fun ContactActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
