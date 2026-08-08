package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.theme.ChatThemes

@Composable
fun PollDialog(onDismiss: () -> Unit, onPollCreated: (String, List<String>) -> Unit) {
    var q by remember { mutableStateOf("") }
    var o1 by remember { mutableStateOf("") }
    var o2 by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Create Poll") },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                OutlinedTextField(q, { q = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(o1, { o1 = it }, label = { Text("Option 1") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(o2, { o2 = it }, label = { Text("Option 2") }, modifier = Modifier.fillMaxWidth()) 
            } 
        },
        confirmButton = { 
            TextButton(
                onClick = { if (q.isNotBlank() && o1.isNotBlank() && o2.isNotBlank()) onPollCreated(q, listOf(o1, o2)) }, 
                enabled = q.isNotBlank() && o1.isNotBlank() && o2.isNotBlank()
            ) { Text("Create") } 
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ForwardDialog(onDismiss: () -> Unit, onForward: (List<String>) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forward Message") },
        text = { Text("Select chats to forward to.") },
        confirmButton = { TextButton(onClick = { onForward(emptyList()) }) { Text("Forward") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ThemePickerDialog(
    currentThemeId: String?,
    onDismiss: () -> Unit,
    onThemeSelect: (String?) -> Unit
) {
    val themes = ChatThemes.all
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                themes.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelect(if (theme.id == "default") null else theme.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentThemeId ?: "default") == theme.id,
                            onClick = { onThemeSelect(if (theme.id == "default") null else theme.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val emoji = when(theme.id) {
                            "rosa" -> "🌸 "
                            "alpha" -> "⚫ "
                            else -> "✓ "
                        }
                        Text(text = "$emoji${theme.name}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AutoDeleteDialog(currentTimer: Long?, onDismiss: () -> Unit, onConfirm: (Long?) -> Unit) {
    val options = listOf("Off" to null, "24 Hours" to 86400000L, "7 Days" to 604800000L, "30 Days" to 2592000000L)
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Auto-Delete Messages") }, 
        text = { 
            Column { 
                options.forEach { (l, v) -> 
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(v) }
                            .padding(vertical = 12.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) { 
                        RadioButton(currentTimer == v, { onConfirm(v) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(l) 
                    } 
                } 
            } 
        }, 
        confirmButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}
