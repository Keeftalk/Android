package tn.keeftalk.chat.domain.xp

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

data class AnchorState(
    val serverMs: Long,
    val elapsedRealtimeAtAnchor: Long,
    val bootCount: Int,
    val isAnchored: Boolean
)

object TrustedClock {
    private var currentAnchor: AnchorState? = null

    fun getBootCount(context: Context): Int {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        } catch (_: Exception) {
            -1
        }
    }

    fun setAnchor(context: Context, serverMs: Long) {
        val bootCount = getBootCount(context)
        val elapsed = SystemClock.elapsedRealtime()
        currentAnchor = AnchorState(
            serverMs = serverMs,
            elapsedRealtimeAtAnchor = elapsed,
            bootCount = bootCount,
            isAnchored = true
        )
    }

    fun getAnchoredNowMs(context: Context): Pair<Long, Boolean> {
        val anchor = currentAnchor
        val currentBoot = getBootCount(context)
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowSystem = System.currentTimeMillis()

        if (anchor != null && anchor.isAnchored && anchor.bootCount == currentBoot && currentBoot != -1) {
            val delta = nowElapsed - anchor.elapsedRealtimeAtAnchor
            if (delta >= 0) {
                return Pair(anchor.serverMs + delta, true)
            }
        }
        return Pair(nowSystem, false)
    }
}
