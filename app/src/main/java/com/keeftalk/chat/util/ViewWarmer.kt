package com.keeftalk.chat.util

import android.content.Context
import android.view.View

/**
 * Managed View Warmer for pre-inflating views.
 * With the move to Compose inside RecyclerView, we primarily rely on standard recycling,
 * but this can be extended to pre-warm ComposeViews if needed.
 */
object ViewWarmer {

    @Suppress("UNUSED_PARAMETER")
    fun init(context: Context) {
        // Compose pre-warming is handled differently
    }

    @Suppress("UNUSED_PARAMETER")
    fun getView(layoutId: Int): View? {
        return null
    }

    @Suppress("unused")
    fun invalidate() {
    }
}
