package com.keeftalk.chat.util

import android.content.Context
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.domain.model.UserParentalControls
import kotlinx.coroutines.flow.first

class ParentalControlManager(
    private val context: Context,
    private val prefs: UserPreferencesRepository
) {
    suspend fun getControls(): UserParentalControls {
        return prefs.fullSettingsFlow.first().parentalControls
    }

    suspend fun canMessage(targetUserId: String, isUnknown: Boolean): Boolean {
        val pc = getControls()
        if (!pc.isEnabled) return true
        
        if (isUnknown && !pc.canMessageUnknown) return false
        
        if (pc.allowedContactLevel == "WHITELIST") {
            return pc.communicationWhitelist.contains(targetUserId)
        }
        
        return true
    }

    suspend fun canCall(targetUserId: String, isUnknown: Boolean): Boolean {
        val pc = getControls()
        if (!pc.isEnabled) return true
        
        if (isUnknown && !pc.canCallUnknown) return false
        
        if (pc.allowedContactLevel == "WHITELIST") {
            return pc.communicationWhitelist.contains(targetUserId)
        }
        
        return true
    }

    suspend fun isMediaDownloadRestricted(sizeBytes: Long): Boolean {
        val pc = getControls()
        if (!pc.isEnabled) return false
        
        if (pc.restrictMediaDownloads) {
            val maxBytes = pc.maxDownloadSizeMb * 1024L * 1024L
            if (sizeBytes > maxBytes) return true
        }
        
        return false
    }
}
