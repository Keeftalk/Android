package com.keeftalk.chat.data.remote

import com.keeftalk.chat.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class UserSettingsDto(
    @SerialName("user_id") val userId: String,
    @SerialName("chat_settings") val chatSettings: UserChatSettingsDto? = null,
    @SerialName("call_settings") val callSettings: UserCallSettingsDto? = null,
    @SerialName("note_settings") val noteSettings: UserNoteSettingsDto? = null,
    @SerialName("vault_settings") val vaultSettings: UserVaultSettingsDto? = null,
    @SerialName("calendar_settings") val calendarSettings: UserCalendarSettingsDto? = null,
    @SerialName("email_settings") val emailSettings: UserEmailSettingsDto? = null,
    @SerialName("notification_settings") val notificationSettings: UserNotificationSettingsDto? = null,
    @SerialName("privacy_settings") val privacySettings: UserPrivacySettingsDto? = null,
    @SerialName("parental_controls") val parentalControls: UserParentalControlsDto? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toDomain() = UserSettings(
        userId = userId,
        chatSettings = chatSettings?.toDomain() ?: UserChatSettings(userId = userId),
        callSettings = callSettings?.toDomain() ?: UserCallSettings(),
        noteSettings = noteSettings?.toDomain() ?: UserNoteSettings(),
        vaultSettings = vaultSettings?.toDomain() ?: UserVaultSettings(),
        calendarSettings = calendarSettings?.toDomain() ?: UserCalendarSettings(),
        emailSettings = emailSettings?.toDomain() ?: UserEmailSettings(),
        notificationSettings = notificationSettings?.toDomain() ?: UserNotificationSettings(),
        privacySettings = privacySettings?.toDomain() ?: UserPrivacySettings(userId = userId),
        parentalControls = parentalControls?.toDomain() ?: UserParentalControls()
        // updatedAt handling if needed
    )

    companion object {
        fun fromDomain(settings: UserSettings) = UserSettingsDto(
            userId = settings.userId,
            chatSettings = UserChatSettingsDto.fromDomain(settings.chatSettings),
            callSettings = UserCallSettingsDto.fromDomain(settings.callSettings),
            noteSettings = UserNoteSettingsDto.fromDomain(settings.noteSettings),
            vaultSettings = UserVaultSettingsDto.fromDomain(settings.vaultSettings),
            calendarSettings = UserCalendarSettingsDto.fromDomain(settings.calendarSettings),
            emailSettings = UserEmailSettingsDto.fromDomain(settings.emailSettings),
            notificationSettings = UserNotificationSettingsDto.fromDomain(settings.notificationSettings),
            privacySettings = UserPrivacySettingsDto.fromDomain(settings.privacySettings),
            parentalControls = UserParentalControlsDto.fromDomain(settings.parentalControls)
        )
    }
}

@Serializable
data class UserCallSettingsDto(
    val incomingCallBehavior: String,
    val ringtone: String,
    val vibration: String,
    val speakerOnStart: Boolean,
    val bluetoothAutoConnect: Boolean,
    val microphoneEnabledByDefault: Boolean,
    val cameraEnabledByDefault: Boolean,
    val videoQuality: String,
    val whoCanCallMe: String,
    val unknownCallerBehavior: String,
    val callHistoryRetentionDays: Int,
    val wifiOnlyCalling: Boolean,
    val dataSavingMode: Boolean
) {
    fun toDomain() = UserCallSettings(
        incomingCallBehavior = incomingCallBehavior,
        ringtone = ringtone,
        vibration = vibration,
        speakerOnStart = speakerOnStart,
        bluetoothAutoConnect = bluetoothAutoConnect,
        microphoneEnabledByDefault = microphoneEnabledByDefault,
        cameraEnabledByDefault = cameraEnabledByDefault,
        videoQuality = videoQuality,
        whoCanCallMe = PrivacyVisibility.fromString(whoCanCallMe),
        unknownCallerBehavior = unknownCallerBehavior,
        callHistoryRetentionDays = callHistoryRetentionDays,
        wifiOnlyCalling = wifiOnlyCalling,
        dataSavingMode = dataSavingMode
    )

    companion object {
        fun fromDomain(d: UserCallSettings) = UserCallSettingsDto(
            d.incomingCallBehavior, d.ringtone, d.vibration, d.speakerOnStart, d.bluetoothAutoConnect,
            d.microphoneEnabledByDefault, d.cameraEnabledByDefault, d.videoQuality, d.whoCanCallMe.name,
            d.unknownCallerBehavior, d.callHistoryRetentionDays, d.wifiOnlyCalling, d.dataSavingMode
        )
    }
}

@Serializable
data class UserNoteSettingsDto(
    val defaultVisibility: String,
    val defaultEditorMode: String,
    val autoSaveEnabled: Boolean,
    val autoSaveIntervalSeconds: Int,
    val defaultSorting: String,
    val defaultView: String,
    val archiveBehavior: String,
    val trashRetentionDays: Int,
    val syncNotesAcrossDevices: Boolean,
    val offlineNotesEnabled: Boolean,
    val syncAttachments: Boolean,
    val conflictResolution: String,
    val lockNotesByDefault: Boolean,
    val requireAuthToOpen: Boolean,
    val encryptionType: String,
    val autoLockTimeoutMinutes: Int,
    val attachmentLimitMb: Int,
    val clearCacheOnExit: Boolean
) {
    fun toDomain() = UserNoteSettings(
        defaultVisibility, defaultEditorMode, autoSaveEnabled, autoSaveIntervalSeconds,
        defaultSorting, defaultView, archiveBehavior, trashRetentionDays,
        syncNotesAcrossDevices, offlineNotesEnabled, syncAttachments, conflictResolution,
        lockNotesByDefault, requireAuthToOpen, encryptionType, autoLockTimeoutMinutes,
        attachmentLimitMb, clearCacheOnExit
    )

    companion object {
        fun fromDomain(d: UserNoteSettings) = UserNoteSettingsDto(
            d.defaultVisibility, d.defaultEditorMode, d.autoSaveEnabled, d.autoSaveIntervalSeconds,
            d.defaultSorting, d.defaultView, d.archiveBehavior, d.trashRetentionDays,
            d.syncNotesAcrossDevices, d.offlineNotesEnabled, d.syncAttachments, d.conflictResolution,
            d.lockNotesByDefault, d.requireAuthToOpen, d.encryptionType, d.autoLockTimeoutMinutes,
            d.attachmentLimitMb, d.clearCacheOnExit
        )
    }
}

@Serializable
data class UserVaultSettingsDto(
    val autoLockVault: Boolean,
    val autoLockTimeoutMinutes: Int,
    val requireBiometricAuth: Boolean,
    val requireAppAuth: Boolean,
    val hideVaultPreviews: Boolean,
    val screenshotProtection: Boolean,
    val autoImportBehavior: String,
    val generateThumbnails: Boolean,
    val mediaQuality: String,
    val localCacheEnabled: Boolean,
    val autoCleanupCache: Boolean,
    val downloadBehavior: String,
    val shareToChatBehavior: String,
    val zeroCopySharingEnabled: Boolean,
    val sharedFileAccessTimeoutHours: Int
) {
    fun toDomain() = UserVaultSettings(
        autoLockVault, autoLockTimeoutMinutes, requireBiometricAuth, requireAppAuth,
        hideVaultPreviews, screenshotProtection, autoImportBehavior, generateThumbnails,
        mediaQuality, localCacheEnabled, autoCleanupCache, downloadBehavior,
        shareToChatBehavior, zeroCopySharingEnabled, sharedFileAccessTimeoutHours
    )

    companion object {
        fun fromDomain(d: UserVaultSettings) = UserVaultSettingsDto(
            d.autoLockVault, d.autoLockTimeoutMinutes, d.requireBiometricAuth, d.requireAppAuth,
            d.hideVaultPreviews, d.screenshotProtection, d.autoImportBehavior, d.generateThumbnails,
            d.mediaQuality, d.localCacheEnabled, d.autoCleanupCache, d.downloadBehavior,
            d.shareToChatBehavior, d.zeroCopySharingEnabled, d.sharedFileAccessTimeoutHours
        )
    }
}

@Serializable
data class UserCalendarSettingsDto(
    val defaultCalendarId: String,
    val defaultEventDurationMinutes: Int,
    val defaultReminderMinutes: Int,
    val weekStartDay: Int,
    val defaultView: String,
    val eventNotificationsEnabled: Boolean,
    val reminderNotificationsEnabled: Boolean,
    val allDayEventReminderTime: String,
    val syncFrequencyMinutes: Int,
    val syncDeviceCalendar: Boolean,
    val syncGoogleCalendar: Boolean,
    val offlineModeEnabled: Boolean,
    val defaultEventPrivacy: String,
    val showBirthdays: Boolean,
    val showHolidays: Boolean
) {
    fun toDomain() = UserCalendarSettings(
        defaultCalendarId, defaultEventDurationMinutes, defaultReminderMinutes, weekStartDay,
        defaultView, eventNotificationsEnabled, reminderNotificationsEnabled, allDayEventReminderTime,
        syncFrequencyMinutes, syncDeviceCalendar, syncGoogleCalendar, offlineModeEnabled,
        defaultEventPrivacy, showBirthdays, showHolidays
    )

    companion object {
        fun fromDomain(d: UserCalendarSettings) = UserCalendarSettingsDto(
            d.defaultCalendarId, d.defaultEventDurationMinutes, d.defaultReminderMinutes, d.weekStartDay,
            d.defaultView, d.eventNotificationsEnabled, d.reminderNotificationsEnabled, d.allDayEventReminderTime,
            d.syncFrequencyMinutes, d.syncDeviceCalendar, d.syncGoogleCalendar, d.offlineModeEnabled,
            d.defaultEventPrivacy, d.showBirthdays, d.showHolidays
        )
    }
}

@Serializable
data class UserEmailSettingsDto(
    val defaultInbox: String,
    val conversationViewEnabled: Boolean,
    val markAsReadDelaySeconds: Int,
    val archiveAfterAction: Boolean,
    val swipeLeftAction: String,
    val swipeRightAction: String,
    val defaultReplyBehavior: String,
    val notificationsEnabled: Boolean,
    val notifyForImportantOnly: Boolean,
    val showNotificationPreview: Boolean,
    val sound: String,
    val syncFrequencyMinutes: Int,
    val syncDaysToKeep: Int,
    val downloadAttachmentsWifiOnly: Boolean,
    val offlineMailEnabled: Boolean,
    val requireAuthToOpen: Boolean,
    val blockExternalImages: Boolean,
    val confirmBeforeSending: Boolean,
    val useSecureLinkHandling: Boolean
) {
    fun toDomain() = UserEmailSettings(
        defaultInbox, conversationViewEnabled, markAsReadDelaySeconds, archiveAfterAction,
        swipeLeftAction, swipeRightAction, defaultReplyBehavior, notificationsEnabled,
        notifyForImportantOnly, showNotificationPreview, sound, syncFrequencyMinutes,
        syncDaysToKeep, downloadAttachmentsWifiOnly, offlineMailEnabled, requireAuthToOpen,
        blockExternalImages, confirmBeforeSending, useSecureLinkHandling
    )

    companion object {
        fun fromDomain(d: UserEmailSettings) = UserEmailSettingsDto(
            d.defaultInbox, d.conversationViewEnabled, d.markAsReadDelaySeconds, d.archiveAfterAction,
            d.swipeLeftAction, d.swipeRightAction, d.defaultReplyBehavior, d.notificationsEnabled,
            d.notifyForImportantOnly, d.showNotificationPreview, d.sound, d.syncFrequencyMinutes,
            d.syncDaysToKeep, d.downloadAttachmentsWifiOnly, d.offlineMailEnabled, d.requireAuthToOpen,
            d.blockExternalImages, d.confirmBeforeSending, d.useSecureLinkHandling
        )
    }
}

@Serializable
data class UserNotificationSettingsDto(
    val allEnabled: Boolean,
    val messagesEnabled: Boolean,
    val groupsEnabled: Boolean,
    val callsEnabled: Boolean,
    val missedCallsEnabled: Boolean,
    val mentionsEnabled: Boolean,
    val repliesEnabled: Boolean,
    val sound: String,
    val vibration: String,
    val showPreview: Boolean
) {
    fun toDomain() = UserNotificationSettings(
        allEnabled, messagesEnabled, groupsEnabled, callsEnabled, missedCallsEnabled,
        mentionsEnabled, repliesEnabled, sound, vibration, showPreview
    )

    companion object {
        fun fromDomain(d: UserNotificationSettings) = UserNotificationSettingsDto(
            d.allEnabled, d.messagesEnabled, d.groupsEnabled, d.callsEnabled, d.missedCallsEnabled,
            d.mentionsEnabled, d.repliesEnabled, d.sound, d.vibration, d.showPreview
        )
    }
}

@Serializable
data class UserParentalControlsDto(
    val isEnabled: Boolean,
    val supervisorId: String?,
    val allowedContactLevel: String,
    val canMessageUnknown: Boolean,
    val canCallUnknown: Boolean,
    val canJoinGroups: Boolean,
    val communicationWhitelist: List<String>,
    val mediaFilterLevel: String,
    val blockExternalLinks: Boolean,
    val restrictMediaDownloads: Boolean,
    val maxDownloadSizeMb: Int,
    val dailyTimeLimitMinutes: Int,
    val quietHoursEnabled: Boolean,
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val restrictedDays: List<Int>,
    val hideProfileFromSearch: Boolean,
    val disableContactDiscovery: Boolean,
    val shareLocationWithSupervisor: Boolean,
    val notifySupervisorOnSecurityAlerts: Boolean,
    val notifySupervisorOnNewContacts: Boolean
) {
    fun toDomain() = UserParentalControls(
        isEnabled, supervisorId, allowedContactLevel, canMessageUnknown, canCallUnknown,
        canJoinGroups, communicationWhitelist, mediaFilterLevel, blockExternalLinks,
        restrictMediaDownloads, maxDownloadSizeMb, dailyTimeLimitMinutes, quietHoursEnabled,
        quietHoursStart, quietHoursEnd, restrictedDays, hideProfileFromSearch,
        disableContactDiscovery, shareLocationWithSupervisor, notifySupervisorOnSecurityAlerts,
        notifySupervisorOnNewContacts
    )

    companion object {
        fun fromDomain(d: UserParentalControls) = UserParentalControlsDto(
            d.isEnabled, d.supervisorId, d.allowedContactLevel, d.canMessageUnknown, d.canCallUnknown,
            d.canJoinGroups, d.communicationWhitelist, d.mediaFilterLevel, d.blockExternalLinks,
            d.restrictMediaDownloads, d.maxDownloadSizeMb, d.dailyTimeLimitMinutes, d.quietHoursEnabled,
            d.quietHoursStart, d.quietHoursEnd, d.restrictedDays, d.hideProfileFromSearch,
            d.disableContactDiscovery, d.shareLocationWithSupervisor, d.notifySupervisorOnSecurityAlerts,
            d.notifySupervisorOnNewContacts
        )
    }
}
