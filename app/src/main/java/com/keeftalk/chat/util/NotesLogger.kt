package com.keeftalk.chat.util

import android.util.Log

object NotesLogger {
    private const val TAG = "KeeftalkNotes"

    fun v(component: String, message: String, noteId: String? = null, userId: String? = null) {
        log(Log.VERBOSE, component, message, noteId, userId)
    }

    fun d(component: String, message: String, noteId: String? = null, userId: String? = null) {
        log(Log.DEBUG, component, message, noteId, userId)
    }

    fun i(component: String, message: String, noteId: String? = null, userId: String? = null) {
        log(Log.INFO, component, message, noteId, userId)
    }

    fun w(component: String, message: String, noteId: String? = null, userId: String? = null) {
        log(Log.WARN, component, message, noteId, userId)
    }

    fun e(component: String, message: String, noteId: String? = null, userId: String? = null, throwable: Throwable? = null) {
        val fullMessage = buildString {
            append("[NOTES][$component] ")
            if (noteId != null) append("[Note: $noteId] ")
            if (userId != null) append("[User: $userId] ")
            append(message)
            if (throwable != null) {
                append("\nStacktrace: ")
                append(Log.getStackTraceString(throwable))
            }
        }
        Log.e(TAG, fullMessage)
    }

    private fun log(priority: Int, component: String, message: String, noteId: String? = null, userId: String? = null) {
        val threadName = Thread.currentThread().name
        val fullMessage = buildString {
            append("[NOTES][$component] ")
            append("[$threadName] ")
            if (noteId != null) append("[Note: $noteId] ")
            if (userId != null) append("[User: $userId] ")
            append(message)
        }
        Log.println(priority, TAG, fullMessage)
    }
}
