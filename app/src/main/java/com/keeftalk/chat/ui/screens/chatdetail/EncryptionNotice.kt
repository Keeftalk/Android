package com.keeftalk.chat.ui.screens.chatdetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.theme.LocalChatTheme

@Composable
fun EncryptionNotice(
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatTheme = LocalChatTheme.current
    
    // Minimal and subtle background based on theme
    val backgroundColor = when (chatTheme.id) {
        "rosa" -> Color.White.copy(alpha = 0.1f)
        "alpha" -> Color(0xFFC9A84C).copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    val textColor = when (chatTheme.id) {
        "rosa" -> Color.White.copy(alpha = 0.8f)
        "alpha" -> Color(0xFFE8E0D0).copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            val text = buildAnnotatedString {
                append("Messages and calls are secured with end-to-end encryption. Only people in this chat can read, listen to, or share them. ")
                pushStringAnnotation(tag = "info", annotation = "info")
                withStyle(style = SpanStyle(
                    color = if (chatTheme.id == "alpha") Color(0xFFC9A84C) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )) {
                    append("?")
                }
                pop()
            }

            Text(
                text = text,
                modifier = Modifier
                    .padding(12.dp)
                    .clickable { onInfoClick() },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    color = textColor
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptionExplanationBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val icons = LocalAppIcons.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icons.lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your messages are private",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Messages and calls in Keeftalk are protected with end-to-end encryption. This means that only the participants in this conversation can access the content.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Keeftalk cannot read your encrypted messages or listen to your encrypted calls.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://keeftalk.com/security"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "See more",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = icons.chevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close")
            }
        }
    }
}
