package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.UserChatSettings
import kotlinx.coroutines.flow.Flow

interface ChatSettingsRepository {
    val chatSettings: Flow<UserChatSettings>
    
    suspend fun updateSettings(settings: UserChatSettings): Result<Unit>
    suspend fun updateSetting(key: String, value: Any): Result<Unit>
    suspend fun resetToDefault(): Result<Unit>
    
    fun startSettingsObservation()
    
    // Storage methods
    suspend fun getCacheSize(): Map<String, Long>
    suspend fun clearCache(types: List<String>): Result<Unit>
    suspend fun clearSearchHistory(): Result<Unit>
    suspend fun clearRecentEmojis(): Result<Unit>

    fun shutdown()
}
