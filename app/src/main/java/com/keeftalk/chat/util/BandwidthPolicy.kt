package com.keeftalk.chat.util

import com.keeftalk.chat.domain.model.SubscriptionPlan

object BandwidthPolicy {
    private const val MB_IN_BYTES = 1024L * 1024L

    // Upload Limits (Bytes per Second)
    private const val UPLOAD_FREE = (2.5 * MB_IN_BYTES).toLong()    // 2,621,440 Bps
    private const val UPLOAD_PLUS = 25 * MB_IN_BYTES              // 26,214,400 Bps
    
    // Download Limits (Bytes per Second)
    private const val DOWNLOAD_FREE = 5 * MB_IN_BYTES             // 5,242,880 Bps
    private const val DOWNLOAD_PLUS = 50 * MB_IN_BYTES            // 52,428,800 Bps

    fun getUploadLimit(plan: SubscriptionPlan): Long? {
        return when (plan) {
            SubscriptionPlan.FREE -> UPLOAD_FREE
            SubscriptionPlan.PLUS_MONTHLY, SubscriptionPlan.PLUS_YEARLY -> UPLOAD_PLUS
            SubscriptionPlan.PRO_MONTHLY, SubscriptionPlan.FAMILY_MONTHLY -> null // Unlimited
        }
    }

    fun getDownloadLimit(plan: SubscriptionPlan): Long? {
        return when (plan) {
            SubscriptionPlan.FREE -> DOWNLOAD_FREE
            SubscriptionPlan.PLUS_MONTHLY, SubscriptionPlan.PLUS_YEARLY -> DOWNLOAD_PLUS
            SubscriptionPlan.PRO_MONTHLY, SubscriptionPlan.FAMILY_MONTHLY -> null // Unlimited
        }
    }

    fun formatLimit(limitBps: Long?): String {
        if (limitBps == null) return "Unlimited"
        val mbps = limitBps.toDouble() / MB_IN_BYTES
        return if (mbps % 1.0 == 0.0) {
            "${mbps.toInt()} MB/s"
        } else {
            "$mbps MB/s"
        }
    }
}
