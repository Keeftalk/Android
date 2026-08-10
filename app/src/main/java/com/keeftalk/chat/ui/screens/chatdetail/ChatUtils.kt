package com.keeftalk.chat.ui.screens.chatdetail

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.recyclerview.widget.RecyclerView
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

private val zoneId = ZoneId.systemDefault()

fun updateBubblePositions(
    recyclerView: RecyclerView,
    bubblePositions: MutableMap<String, Pair<Offset, IntSize>>,
) {
    for (i in 0 until recyclerView.childCount) {
        val child = recyclerView.getChildAt(i)
        val tag = child.tag as? String ?: continue
        
        // Handle complex binding tags: msg_UUID_status_...
        val messageId = if (tag.startsWith("msg_")) {
            tag.split("_").getOrNull(1) ?: tag
        } else {
            tag
        }
        
        val x = child.x
        val y = child.y
        bubblePositions[messageId] = Offset(x, y) to IntSize(child.width, child.height)
    }
}

fun formatDurationInternal(m: Long) = String.format(Locale.getDefault(), "%02d:%02d", (m / 60000) % 60, (m / 1000) % 60)

fun formatAutoDeleteInternal(t: Long) = when (t) { 
    86400000L -> "24h"
    604800000L -> "7d"
    2592000000L -> "30d"
    else -> "${t / 3600000}h" 
}

fun formatTimestampInternal(t: Long) = DateFormat.format("HH:mm", t).toString()

fun formatLastSeenInternal(t: Long) = "Last seen ${DateUtils.getRelativeTimeSpanString(t)}"

fun formatDateSeparatorInternal(t: Long): String {
    val date = Instant.ofEpochMilli(t).atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.isAfter(today.minusDays(7)) -> DateFormat.format("EEEE", t).toString()
        date.year == today.year -> DateFormat.format("d MMMM", t).toString()
        else -> DateFormat.format("d MMMM yyyy", t).toString()
    }
}

fun isYesterday(t: Long): Boolean {
    val date = Instant.ofEpochMilli(t).atZone(zoneId).toLocalDate()
    val yesterday = LocalDate.now(zoneId).minusDays(1)
    return date == yesterday
}

fun isWithinSameWeek(t: Long): Boolean {
    val date = Instant.ofEpochMilli(t).atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    return date.isAfter(today.minusDays(7))
}

fun isWithinSameYear(t: Long): Boolean {
    val date = Instant.ofEpochMilli(t).atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    return date.year == today.year
}

fun isDifferentDay(t1: Long, t2: Long): Boolean {
    val d1 = Instant.ofEpochMilli(t1).atZone(zoneId).toLocalDate()
    val d2 = Instant.ofEpochMilli(t2).atZone(zoneId).toLocalDate()
    return d1 != d2
}

fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}
