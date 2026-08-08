package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.keeftalk.chat.domain.model.calendar.CalendarItem
import com.keeftalk.chat.ui.components.KeeftalkFeatureTopBar

@Composable
fun AgendaPicker(
    items: List<CalendarItem>,
    onBack: () -> Unit,
    onItemSelected: (CalendarItem) -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            KeeftalkFeatureTopBar(
                title = "Choose Agenda Item",
                onBack = onBack
            )
        },
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(items) { item ->
                AgendaPickerItem(item = item, onClick = { onItemSelected(item) })
            }
        }
    }
}

@Composable
fun AgendaPickerItem(item: CalendarItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, null, tint = Color(0xFFE91E63))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(item.type.name, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }
}
