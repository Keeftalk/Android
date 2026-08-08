package com.keeftalk.chat.util

import android.util.Log
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Suppress("unused")
object PerformanceMetrics {
    private const val TAG = "KeeftalkPerformance"
    
    private val databaseQueryCount = AtomicInteger(0)
    private val recompositionCounts = mutableMapOf<String, AtomicInteger>()
    private val startTimeNanos = AtomicLong(0)
    
    private var chatOpenStartNanos = 0L
    private var chatScreenCreatedNanos = 0L
    private var chatViewModelCreatedNanos = 0L
    private var chatRoomQueryStartNanos = 0L
    private var chatRoomQueryEndNanos = 0L
    private var chatPagingDataEmittedNanos = 0L
    private var chatFirstMessageComposedNanos = 0L
    private var chatFirstFrameNanos = 0L

    fun onAppStarted() {
        startTimeNanos.set(System.nanoTime())
    }

    fun onChatOpenStarted() {
        chatOpenStartNanos = System.nanoTime()
        chatFirstFrameNanos = 0L
        Log.d("CHAT_PERF", "ENTER_CHAT")
    }

    fun onChatScreenCreated() {
        chatScreenCreatedNanos = System.nanoTime()
        Log.d("CHAT_PERF", "SCREEN_CREATED (tap → screen = ${(chatScreenCreatedNanos - chatOpenStartNanos) / 1_000_000}ms)")
    }

    fun onChatViewModelCreated() {
        chatViewModelCreatedNanos = System.nanoTime()
        Log.d("CHAT_PERF", "VIEWMODEL_CREATED (screen → ViewModel = ${(chatViewModelCreatedNanos - chatScreenCreatedNanos) / 1_000_000}ms)")
    }

    fun onChatRoomQueryStart() {
        chatRoomQueryStartNanos = System.nanoTime()
        Log.d("CHAT_PERF", "ROOM_QUERY_START (ViewModel → Room query = ${(chatRoomQueryStartNanos - chatViewModelCreatedNanos) / 1_000_000}ms)")
    }

    fun onChatRoomQueryEnd(count: Int) {
        chatRoomQueryEndNanos = System.nanoTime()
        Log.d("CHAT_PERF", "ROOM_QUERY_END count=$count (Room query = ${(chatRoomQueryEndNanos - chatRoomQueryStartNanos) / 1_000_000}ms)")
    }

    fun onChatPagingDataEmitted(count: Int) {
        chatPagingDataEmittedNanos = System.nanoTime()
        Log.d("CHAT_PERF", "PAGING_DATA_EMITTED count=$count (Room → PagingData = ${(chatPagingDataEmittedNanos - chatRoomQueryEndNanos) / 1_000_000}ms)")
    }

    fun onChatFirstMessageComposed() {
        if (chatFirstMessageComposedNanos == 0L) {
            chatFirstMessageComposedNanos = System.nanoTime()
            Log.d("CHAT_PERF", "FIRST_MESSAGE_COMPOSED (PagingData → UI = ${(chatFirstMessageComposedNanos - chatPagingDataEmittedNanos) / 1_000_000}ms)")
        }
    }

    fun onChatFirstFrameDrawn() {
        if (chatFirstFrameNanos == 0L) {
            chatFirstFrameNanos = System.nanoTime()
            val total = (chatFirstFrameNanos - chatOpenStartNanos) / 1_000_000
            val uiDelay = (chatFirstFrameNanos - chatFirstMessageComposedNanos) / 1_000_000
            Log.d("CHAT_PERF", "FIRST_FRAME (first message → first rendered frame = ${uiDelay}ms)")
            Log.i(TAG, "Time To Open (TTO): ${total}ms")
        }
    }

    fun onPaginationTriggered() {
        Log.d("CHAT_PERF", "PAGINATION_TRIGGERED")
    }

    fun onOlderQueryStart() {
        Log.d("CHAT_PERF", "OLDER_QUERY_START")
    }

    fun onOlderQueryEnd(count: Int) {
        Log.d("CHAT_PERF", "OLDER_QUERY_END count=$count")
    }

    fun onOlderMessagesReady(count: Int) {
        Log.d("CHAT_PERF", "OLDER_MESSAGES_READY count=$count")
    }

    fun onPrependComplete() {
        Log.d("CHAT_PERF", "PREPEND_COMPLETE")
    }

    fun logDatabaseQuery(query: String) {
        databaseQueryCount.incrementAndGet()
        // Log.v(TAG, "DB Query: $query")
    }

    fun logRecomposition(name: String) {
        recompositionCounts.getOrPut(name) { AtomicInteger(0) }.incrementAndGet()
    }

    fun getReport(): String {
        val totalTimeMs = (System.nanoTime() - startTimeNanos.get()) / 1_000_000
        val sb = StringBuilder()
        sb.append("\n============= PERFORMANCE REPORT =============\n")
        sb.append("Time since process start: ${totalTimeMs}ms\n")
        sb.append("Total Database Queries: ${databaseQueryCount.get()}\n")
        sb.append("Recompositions:\n")
        recompositionCounts.forEach { (name, count) ->
            sb.append("  - $name: ${count.get()}\n")
        }
        sb.append("==============================================\n")
        return sb.toString()
    }
}
