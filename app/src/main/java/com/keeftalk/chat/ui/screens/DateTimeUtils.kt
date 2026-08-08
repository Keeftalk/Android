package com.keeftalk.chat.ui.screens

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
private val monthDayFormat = SimpleDateFormat("MMMM d", Locale.getDefault())
private val fullDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
private val recentCallFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dayAbbrFormat = SimpleDateFormat("EEE", Locale.getDefault())
private val shortDateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
private val joinDateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

fun formatJoinDate(timestamp: Long): String {
    if (timestamp <= 0) return "Unknown"
    return joinDateFormat.format(Date(timestamp))
}

fun formatTimestamp(timestamp: Long): String {
    return timeFormat.format(Date(timestamp))
}

fun formatDateSeparator(timestamp: Long): String {
    val now = Calendar.getInstance()
    val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        isSameDay(now, messageDate) -> "Today"
        isYesterday(now, messageDate) -> "Yesterday"
        isSameWeek(now, messageDate) -> dayFormat.format(messageDate.time)
        isSameYear(now, messageDate) -> monthDayFormat.format(messageDate.time)
        else -> fullDateFormat.format(messageDate.time)
    }
}

fun formatRecentCallDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val callDate = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        isSameDay(now, callDate) -> recentCallFormat.format(callDate.time)
        isYesterday(now, callDate) -> "Yesterday"
        isSameWeek(now, callDate) -> dayAbbrFormat.format(callDate.time)
        else -> shortDateFormat.format(callDate.time)
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(now: Calendar, date: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return isSameDay(yesterday, date)
}

private fun isSameWeek(now: Calendar, date: Calendar): Boolean {
    return now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            now.get(Calendar.WEEK_OF_YEAR) == date.get(Calendar.WEEK_OF_YEAR)
}

private fun isSameYear(now: Calendar, date: Calendar): Boolean {
    return now.get(Calendar.YEAR) == date.get(Calendar.YEAR)
}

fun formatLastSeen(timestamp: Long): String {
    if (timestamp <= 0) return "Offline"
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 1000 / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Active now"
        minutes < 60 -> "Last seen $minutes minutes ago"
        hours < 24 -> "Last seen $hours hours ago"
        else -> "Last seen $days days ago"
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        days == 1L -> "Yesterday"
        else -> "$days days ago"
    }
}
