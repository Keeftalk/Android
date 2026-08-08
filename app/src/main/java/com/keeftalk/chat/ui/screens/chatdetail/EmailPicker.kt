package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import com.keeftalk.chat.feature.email.model.EmailMessage
import com.keeftalk.chat.ui.components.KeeftalkFeatureTopBar

@Composable
fun EmailPicker(
    emails: LazyPagingItems<EmailMessage>,
    onBack: () -> Unit,
    onEmailSelected: (EmailMessage) -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            KeeftalkFeatureTopBar(
                title = "Choose an Email",
                onBack = onBack
            )
        },
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(
                count = emails.itemCount,
                key = emails.itemKey { it.id },
                contentType = emails.itemContentType { "email" }
            ) { index ->
                val email = emails[index]
                if (email != null) {
                    EmailPickerItem(email = email, onClick = { onEmailSelected(email) })
                }
            }
        }
    }
}

@Composable
fun EmailPickerItem(email: EmailMessage, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Email, null, tint = Color(0xFF2196F3))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(email.subject.ifBlank { "(No Subject)" }, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("From: ${email.senderName}", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }
}
