package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.UserPrivacySettings
import kotlinx.coroutines.flow.Flow

interface PrivacyRepository {
    val privacySettings: Flow<UserPrivacySettings>
    suspend fun updateSettings(settings: UserPrivacySettings)
    suspend fun updateSetting(key: String, value: Any)
    suspend fun getBlockedUsers(): Flow<List<String>>
    suspend fun blockUser(userId: String)
    suspend fun unblockUser(userId: String)
    suspend fun recordProfileView(viewedUserId: String)
    suspend fun getProfileViews(limit: Int = 20, offset: Int = 0): Flow<List<ProfileView>>
    suspend fun deleteAccount()
    suspend fun downloadMyData(): Result<String>
    fun shutdown()
}

data class ProfileView(
    val viewerId: String,
    val viewedUserId: String,
    val viewedAt: Long,
    val viewerName: String?,
    val viewerAvatarUrl: String?
)
