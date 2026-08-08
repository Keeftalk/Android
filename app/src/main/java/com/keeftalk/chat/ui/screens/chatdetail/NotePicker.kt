package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.domain.model.Note
import com.keeftalk.chat.ui.components.KeeftalkFeatureTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePicker(
    notes: List<Note>,
    onBack: () -> Unit,
    onNoteSelected: (Note, String, Boolean) -> Unit
) {
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    BackHandler {
        if (selectedNote != null) selectedNote = null
        else onBack()
    }
    var accessLevel by remember { mutableStateOf("read") }
    var canInviteOthers by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            KeeftalkFeatureTopBar(
                title = if (selectedNote == null) "Choose a Note" else "Set Permissions",
                onBack = {
                    if (selectedNote != null) selectedNote = null
                    else onBack()
                }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (selectedNote == null) {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(notes) { note ->
                    NotePickerItem(note = note, onClick = { selectedNote = note })
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    "Permissions for: ${selectedNote!!.title}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                PermissionOption(
                    title = "View only (Read)",
                    selected = accessLevel == "read",
                    onClick = { accessLevel = "read" }
                )
                PermissionOption(
                    title = "Read & Write (Edit)",
                    selected = accessLevel == "write",
                    onClick = { accessLevel = "write" }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Can add others", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Allow recipient to share this note with more people", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    Switch(
                        checked = canInviteOthers,
                        onCheckedChange = { canInviteOthers = it }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { onNoteSelected(selectedNote!!, accessLevel, canInviteOthers) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Share Note", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NotePickerItem(note: Note, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, null, tint = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(note.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(note.content.take(50), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun PermissionOption(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
