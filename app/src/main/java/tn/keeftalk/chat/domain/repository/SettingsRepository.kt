package tn.keeftalk.chat.domain.repository

import tn.keeftalk.chat.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    
    suspend fun updateSettings(settings: UserSettings): Result<Unit>
    suspend fun updateChatSettings(update: (tn.keeftalk.chat.domain.model.UserChatSettings) -> tn.keeftalk.chat.domain.model.UserChatSettings): Result<Unit>
    suspend fun updateCallSettings(update: (tn.keeftalk.chat.domain.model.UserCallSettings) -> tn.keeftalk.chat.domain.model.UserCallSettings): Result<Unit>
    suspend fun updateNoteSettings(update: (tn.keeftalk.chat.domain.model.UserNoteSettings) -> tn.keeftalk.chat.domain.model.UserNoteSettings): Result<Unit>
    suspend fun updateVaultSettings(update: (tn.keeftalk.chat.domain.model.UserVaultSettings) -> tn.keeftalk.chat.domain.model.UserVaultSettings): Result<Unit>
    suspend fun updateCalendarSettings(update: (tn.keeftalk.chat.domain.model.UserCalendarSettings) -> tn.keeftalk.chat.domain.model.UserCalendarSettings): Result<Unit>
    suspend fun updateEmailSettings(update: (tn.keeftalk.chat.domain.model.UserEmailSettings) -> tn.keeftalk.chat.domain.model.UserEmailSettings): Result<Unit>
    suspend fun updateNotificationSettings(update: (tn.keeftalk.chat.domain.model.UserNotificationSettings) -> tn.keeftalk.chat.domain.model.UserNotificationSettings): Result<Unit>
    suspend fun updatePrivacySettings(update: (tn.keeftalk.chat.domain.model.UserPrivacySettings) -> tn.keeftalk.chat.domain.model.UserPrivacySettings): Result<Unit>
    suspend fun updateParentalControls(update: (tn.keeftalk.chat.domain.model.UserParentalControls) -> tn.keeftalk.chat.domain.model.UserParentalControls): Result<Unit>
    suspend fun updateBrowserSettings(update: (tn.keeftalk.chat.domain.model.UserBrowserSettings) -> tn.keeftalk.chat.domain.model.UserBrowserSettings): Result<Unit>
    suspend fun updateHabitSettings(update: (tn.keeftalk.chat.domain.model.UserHabitSettings) -> tn.keeftalk.chat.domain.model.UserHabitSettings): Result<Unit>
    
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
