package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    
    suspend fun updateSettings(settings: UserSettings): Result<Unit>
    suspend fun updateChatSettings(update: (com.keeftalk.chat.domain.model.UserChatSettings) -> com.keeftalk.chat.domain.model.UserChatSettings): Result<Unit>
    suspend fun updateCallSettings(update: (com.keeftalk.chat.domain.model.UserCallSettings) -> com.keeftalk.chat.domain.model.UserCallSettings): Result<Unit>
    suspend fun updateNoteSettings(update: (com.keeftalk.chat.domain.model.UserNoteSettings) -> com.keeftalk.chat.domain.model.UserNoteSettings): Result<Unit>
    suspend fun updateVaultSettings(update: (com.keeftalk.chat.domain.model.UserVaultSettings) -> com.keeftalk.chat.domain.model.UserVaultSettings): Result<Unit>
    suspend fun updateCalendarSettings(update: (com.keeftalk.chat.domain.model.UserCalendarSettings) -> com.keeftalk.chat.domain.model.UserCalendarSettings): Result<Unit>
    suspend fun updateEmailSettings(update: (com.keeftalk.chat.domain.model.UserEmailSettings) -> com.keeftalk.chat.domain.model.UserEmailSettings): Result<Unit>
    suspend fun updateNotificationSettings(update: (com.keeftalk.chat.domain.model.UserNotificationSettings) -> com.keeftalk.chat.domain.model.UserNotificationSettings): Result<Unit>
    suspend fun updatePrivacySettings(update: (com.keeftalk.chat.domain.model.UserPrivacySettings) -> com.keeftalk.chat.domain.model.UserPrivacySettings): Result<Unit>
    suspend fun updateParentalControls(update: (com.keeftalk.chat.domain.model.UserParentalControls) -> com.keeftalk.chat.domain.model.UserParentalControls): Result<Unit>
    
    // Tools & Management
    suspend fun getCacheSize(): Map<String, Long>
    suspend fun clearCache(types: List<String>): Result<Unit>
    suspend fun clearSearchHistory(): Result<Unit>
    suspend fun clearRecentEmojis(): Result<Unit>
    suspend fun resetToDefault(): Result<Unit>

    fun startSettingsSync()
    fun stopSettingsSync()
    fun shutdown()
}
