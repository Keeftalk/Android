package com.keeftalk.chat.util

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.keeftalk.chat.R
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Pre-inflates views in the background to eliminate layout inflation latency
 * on the critical UI thread path.
 */
object CachedInflater {
    private const val TAG = "CachedInflater"
    private val viewPool = ConcurrentLinkedQueue<View>()
    private const val TARGET_POOL_SIZE = 15

    /**
     * Triggers background inflation of conversation list items.
     * Should be called in Tier 2 (Background Startup).
     */
    fun preInflate(context: Context) {
        try {
            // Use themed context to resolve attributes
            val themedContext = ContextThemeWrapper(context, R.style.Theme_Keeftalk)
            val inflater = LayoutInflater.from(themedContext)
            
            repeat(TARGET_POOL_SIZE) {
                // Pre-inflation still helps warm up the layout cache and XML parser.
                val view = inflater.inflate(R.layout.layout_chat_item, null, false)
                viewPool.offer(view)
            }
            PerformanceProfiler.logEvent("CachedInflater: Pre-inflated ${viewPool.size} items", category = PerformanceProfiler.Category.UI)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to pre-inflate views", e)
        }
    }

    /**
     * Pulls a pre-inflated view from the pool or inflates a new one if empty.
     */
    fun inflate(inflater: LayoutInflater, parent: ViewGroup): View {
        return viewPool.poll() ?: inflater.inflate(R.layout.layout_chat_item, parent, false)
    }
}
