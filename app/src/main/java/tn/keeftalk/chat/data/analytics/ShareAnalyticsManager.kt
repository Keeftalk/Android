package tn.keeftalk.chat.data.analytics

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

enum class ShareAction {
    INITIATED,
    COMPLETED,
    CANCELLED
}

enum class ShareCategory {
    CHAT,
    FEED,
    VAULT,
    NOTE,
    EXTERNAL
}

/**
 * Privacy-preserving aggregate share analytics.
 * Captures zero personal content, no message text, no user IDs, no recipient identifiers.
 * Respects Keeftalk privacy commitments.
 */
object ShareAnalyticsManager {
    private const val TAG = "ShareAnalyticsManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val totalInitiated = AtomicInteger(0)
    private val totalCompleted = AtomicInteger(0)
    private val totalCancelled = AtomicInteger(0)

    fun trackShareEvent(
        action: ShareAction,
        category: ShareCategory
    ) {
        scope.launch {
            when (action) {
                ShareAction.INITIATED -> totalInitiated.incrementAndGet()
                ShareAction.COMPLETED -> totalCompleted.incrementAndGet()
                ShareAction.CANCELLED -> totalCancelled.incrementAndGet()
            }
            Log.d(TAG, "Share Event recorded [Privacy Preserved]: action=$action, category=$category")
        }
    }

    fun getAggregateStats(): String {
        return "Shares(Initiated=${totalInitiated.get()}, Completed=${totalCompleted.get()}, Cancelled=${totalCancelled.get()})"
    }
}
