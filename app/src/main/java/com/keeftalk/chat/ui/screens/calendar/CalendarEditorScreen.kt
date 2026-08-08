package com.keeftalk.chat.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.domain.model.calendar.*
import com.keeftalk.chat.ui.components.FixedRichTextEditor
import com.mohamedrejeb.richeditor.model.RichTextState
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEditorScreen(
    type: CalendarItemType,
    onBack: () -> Unit,
    onSave: (CalendarItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    val descriptionState = remember { RichTextState() }
    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var endTime by remember { mutableStateOf(System.currentTimeMillis() + 3600000) }
    var isAllDay by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(CalendarPriority.MEDIUM) }
    var color by remember { mutableStateOf("#6C63FF") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New ${type.name.lowercase().replaceFirstChar { it.uppercase() }}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val item = CalendarItem(
                                id = UUID.randomUUID().toString(),
                                type = type,
                                title = title,
                                description = descriptionState.toHtml(),
                                color = color,
                                icon = null,
                                location = null,
                                startTime = startTime,
                                endTime = endTime,
                                isAllDay = isAllDay,
                                timezone = TimeZone.getDefault().id,
                                recurrenceRule = null,
                                priority = priority,
                                status = CalendarStatus.PENDING,
                                isPrivate = false,
                                ownerId = "",
                                categoryId = null,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis(),
                                meetingType = null,
                                meetingLink = null,
                                progress = 0,
                                pomodoroCount = 0,
                                deadline = null,
                                parentItemId = null
                            )
                            onSave(item)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Priority: ")
                CalendarPriority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.name) },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Description", style = MaterialTheme.typography.titleMedium)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                FixedRichTextEditor(
                    state = descriptionState,
                    modifier = Modifier.padding(8.dp),
                    placeholder = { Text("Add more details...") }
                )
            }
        }
    }
}
