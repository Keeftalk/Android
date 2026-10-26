package tn.keeftalk.chat.data.remote

import tn.keeftalk.chat.domain.model.*
import kotlinx.serialization.KSerializer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

open class FlexibleDtoSerializer<T : Any>(serializer: KSerializer<T>) : JsonTransformingSerializer<T>(serializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> element
            is JsonPrimitive -> {
                if (element.isString) {
                    try {
                        Json.parseToJsonElement(element.content)
                    } catch (_: Exception) {
                        JsonObject(emptyMap())
                    }
                } else {
                    JsonObject(emptyMap())
                }
            }
            else -> JsonObject(emptyMap())
        }
    }
}

object UserChatSettingsFlexibleSerializer : FlexibleDtoSerializer<UserChatSettingsDto>(UserChatSettingsDto.serializer())
object UserCallSettingsFlexibleSerializer : FlexibleDtoSerializer<UserCallSettingsDto>(UserCallSettingsDto.serializer())
object UserNoteSettingsFlexibleSerializer : FlexibleDtoSerializer<UserNoteSettingsDto>(UserNoteSettingsDto.serializer())
object UserVaultSettingsFlexibleSerializer : FlexibleDtoSerializer<UserVaultSettingsDto>(UserVaultSettingsDto.serializer())
object UserCalendarSettingsFlexibleSerializer : FlexibleDtoSerializer<UserCalendarSettingsDto>(UserCalendarSettingsDto.serializer())
object UserEmailSettingsFlexibleSerializer : FlexibleDtoSerializer<UserEmailSettingsDto>(UserEmailSettingsDto.serializer())
object UserNotificationSettingsFlexibleSerializer : FlexibleDtoSerializer<UserNotificationSettingsDto>(UserNotificationSettingsDto.serializer())
object UserPrivacySettingsFlexibleSerializer : FlexibleDtoSerializer<UserPrivacySettingsDto>(UserPrivacySettingsDto.serializer())
object UserParentalControlsFlexibleSerializer : FlexibleDtoSerializer<UserParentalControlsDto>(UserParentalControlsDto.serializer())
object UserBrowserSettingsFlexibleSerializer : FlexibleDtoSerializer<UserBrowserSettingsDto>(UserBrowserSettingsDto.serializer())
object UserHabitSettingsFlexibleSerializer : FlexibleDtoSerializer<UserHabitSettingsDto>(UserHabitSettingsDto.serializer())
object UserFeedSettingsFlexibleSerializer : FlexibleDtoSerializer<UserFeedSettingsDto>(UserFeedSettingsDto.serializer())
object BookSettingsFlexibleSerializer : FlexibleDtoSerializer<BookSettingsDto>(BookSettingsDto.serializer())
object UserMarketSettingsFlexibleSerializer : FlexibleDtoSerializer<UserMarketSettingsDto>(UserMarketSettingsDto.serializer())
object UserSmsSettingsFlexibleSerializer : FlexibleDtoSerializer<UserSmsSettingsDto>(UserSmsSettingsDto.serializer())
object UserTodoSettingsFlexibleSerializer : FlexibleDtoSerializer<UserTodoSettingsDto>(UserTodoSettingsDto.serializer())
object UserMapSettingsFlexibleSerializer : FlexibleDtoSerializer<UserMapSettingsDto>(UserMapSettingsDto.serializer())

@Serializable
data class UserSettingsDto(
    @SerialName("user_id") val userId: String,
    @Serializable(with = UserChatSettingsFlexibleSerializer::class) @SerialName("chat_settings") val chatSettings: UserChatSettingsDto? = null,
    @Serializable(with = UserCallSettingsFlexibleSerializer::class) @SerialName("call_settings") val callSettings: UserCallSettingsDto? = null,
    @Serializable(with = UserNoteSettingsFlexibleSerializer::class) @SerialName("note_settings") val noteSettings: UserNoteSettingsDto? = null,
    @Serializable(with = UserVaultSettingsFlexibleSerializer::class) @SerialName("vault_settings") val vaultSettings: UserVaultSettingsDto? = null,
    @Serializable(with = UserCalendarSettingsFlexibleSerializer::class) @SerialName("calendar_settings") val calendarSettings: UserCalendarSettingsDto? = null,
    @Serializable(with = UserEmailSettingsFlexibleSerializer::class) @SerialName("email_settings") val emailSettings: UserEmailSettingsDto? = null,
    @Serializable(with = UserNotificationSettingsFlexibleSerializer::class) @SerialName("notification_settings") val notificationSettings: UserNotificationSettingsDto? = null,
    @Serializable(with = UserPrivacySettingsFlexibleSerializer::class) @SerialName("privacy_settings") val privacySettings: UserPrivacySettingsDto? = null,
    @Serializable(with = UserParentalControlsFlexibleSerializer::class) @SerialName("parental_controls") val parentalControls: UserParentalControlsDto? = null,
    @Serializable(with = UserBrowserSettingsFlexibleSerializer::class) @SerialName("browser_settings") val browserSettings: UserBrowserSettingsDto? = null,
    @Serializable(with = UserHabitSettingsFlexibleSerializer::class) @SerialName("habit_settings") val habitSettings: UserHabitSettingsDto? = null,
    @Serializable(with = UserFeedSettingsFlexibleSerializer::class) @SerialName("feed_settings") val feedSettings: UserFeedSettingsDto? = null,
    @Serializable(with = BookSettingsFlexibleSerializer::class) @SerialName("book_settings") val bookSettings: BookSettingsDto? = null,
    @Serializable(with = UserMarketSettingsFlexibleSerializer::class) @SerialName("market_settings") val marketSettings: UserMarketSettingsDto? = null,
    @Serializable(with = UserSmsSettingsFlexibleSerializer::class) @SerialName("sms_settings") val smsSettings: UserSmsSettingsDto? = null,
    @Serializable(with = UserTodoSettingsFlexibleSerializer::class) @SerialName("todo_settings") val todoSettings: UserTodoSettingsDto? = null,
    @Serializable(with = UserMapSettingsFlexibleSerializer::class) @SerialName("map_settings") val mapSettings: UserMapSettingsDto? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
 {
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
        parentalControls = parentalControls?.toDomain() ?: UserParentalControls(),
        browserSettings = browserSettings?.toDomain() ?: UserBrowserSettings(),
        habitSettings = habitSettings?.toDomain() ?: UserHabitSettings(),
        feedSettings = feedSettings?.toDomain() ?: UserFeedSettings(),
        bookSettings = bookSettings?.toDomain() ?: BookSettings(),
        marketSettings = marketSettings?.toDomain() ?: UserMarketSettings(),
        smsSettings = smsSettings?.toDomain() ?: UserSmsSettings(),
        todoSettings = todoSettings?.toDomain() ?: UserTodoSettings(),
        mapSettings = mapSettings?.toDomain() ?: UserMapSettings()
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
            parentalControls = UserParentalControlsDto.fromDomain(settings.parentalControls),
            browserSettings = UserBrowserSettingsDto.fromDomain(settings.browserSettings),
            habitSettings = UserHabitSettingsDto.fromDomain(settings.habitSettings),
            feedSettings = UserFeedSettingsDto.fromDomain(settings.feedSettings),
            bookSettings = BookSettingsDto.fromDomain(settings.bookSettings),
            marketSettings = UserMarketSettingsDto.fromDomain(settings.marketSettings),
            smsSettings = UserSmsSettingsDto.fromDomain(settings.smsSettings),
            todoSettings = UserTodoSettingsDto.fromDomain(settings.todoSettings),
            mapSettings = UserMapSettingsDto.fromDomain(settings.mapSettings)
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
data class FolderBackupConfigDto(
    val id: String,
    val name: String,
    val path: String,
    val enabled: Boolean = true,
    val isPreset: Boolean = false,
    val fileTypes: List<String> = listOf("PHOTO", "VIDEO", "DOCUMENT", "OTHER")
) {
    fun toDomain() = FolderBackupConfig(id, name, path, enabled, isPreset, fileTypes)

    companion object {
        fun fromDomain(d: FolderBackupConfig) = FolderBackupConfigDto(
            d.id, d.name, d.path, d.enabled, d.isPreset, d.fileTypes
        )
    }
}

@Serializable
data class UserVaultSettingsDto(
    val autoLockVault: Boolean = false,
    val autoLockTimeoutMinutes: Int = 1,
    val requireBiometricAuth: Boolean = false,
    val requireAppAuth: Boolean = false,
    val hideVaultPreviews: Boolean = true,
    val screenshotProtection: Boolean = false,
    val autoImportBehavior: String = "ASK",
    val generateThumbnails: Boolean = true,
    val mediaQuality: String = "ORIGINAL",
    val localCacheEnabled: Boolean = true,
    val autoCleanupCache: Boolean = true,
    val downloadBehavior: String = "WIFI_ONLY",
    val automaticBackupEnabled: Boolean = false,
    val backupAllPhotosAndVideos: Boolean = false,
    val backupWifiOnly: Boolean = true,
    val backupAllowMobileData: Boolean = false,
    val backupOnlyWhileCharging: Boolean = false,
    val backupFolders: List<FolderBackupConfigDto> = emptyList(),
    val shareToChatBehavior: String = "ENCRYPTED_LINK",
    val zeroCopySharingEnabled: Boolean = true,
    val sharedFileAccessTimeoutHours: Int = 24
) {
    fun toDomain() = UserVaultSettings(
        autoLockVault = autoLockVault,
        autoLockTimeoutMinutes = autoLockTimeoutMinutes,
        requireBiometricAuth = requireBiometricAuth,
        requireAppAuth = requireAppAuth,
        hideVaultPreviews = hideVaultPreviews,
        screenshotProtection = screenshotProtection,
        autoImportBehavior = autoImportBehavior,
        generateThumbnails = generateThumbnails,
        mediaQuality = mediaQuality,
        localCacheEnabled = localCacheEnabled,
        autoCleanupCache = autoCleanupCache,
        downloadBehavior = downloadBehavior,
        automaticBackupEnabled = automaticBackupEnabled,
        backupAllPhotosAndVideos = backupAllPhotosAndVideos,
        backupWifiOnly = backupWifiOnly,
        backupAllowMobileData = backupAllowMobileData,
        backupOnlyWhileCharging = backupOnlyWhileCharging,
        backupFolders = if (backupFolders.isEmpty()) defaultPresetFolders() else backupFolders.map { it.toDomain() },
        shareToChatBehavior = shareToChatBehavior,
        zeroCopySharingEnabled = zeroCopySharingEnabled,
        sharedFileAccessTimeoutHours = sharedFileAccessTimeoutHours
    )

    companion object {
        fun fromDomain(d: UserVaultSettings) = UserVaultSettingsDto(
            autoLockVault = d.autoLockVault,
            autoLockTimeoutMinutes = d.autoLockTimeoutMinutes,
            requireBiometricAuth = d.requireBiometricAuth,
            requireAppAuth = d.requireAppAuth,
            hideVaultPreviews = d.hideVaultPreviews,
            screenshotProtection = d.screenshotProtection,
            autoImportBehavior = d.autoImportBehavior,
            generateThumbnails = d.generateThumbnails,
            mediaQuality = d.mediaQuality,
            localCacheEnabled = d.localCacheEnabled,
            autoCleanupCache = d.autoCleanupCache,
            downloadBehavior = d.downloadBehavior,
            automaticBackupEnabled = d.automaticBackupEnabled,
            backupAllPhotosAndVideos = d.backupAllPhotosAndVideos,
            backupWifiOnly = d.backupWifiOnly,
            backupAllowMobileData = d.backupAllowMobileData,
            backupOnlyWhileCharging = d.backupOnlyWhileCharging,
            backupFolders = d.backupFolders.map { FolderBackupConfigDto.fromDomain(it) },
            shareToChatBehavior = d.shareToChatBehavior,
            zeroCopySharingEnabled = d.zeroCopySharingEnabled,
            sharedFileAccessTimeoutHours = d.sharedFileAccessTimeoutHours
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
    val allEnabled: Boolean = true,
    val inAppEnabled: Boolean = true,
    val systemEnabled: Boolean = true,
    val fcmEnabled: Boolean = true,
    val sound: String = "DEFAULT",
    val vibration: String = "NORMAL",
    val showPreview: Boolean = true,
    val hideSensitiveContent: Boolean = true,
    val messagesEnabled: Boolean = true,
    val groupsEnabled: Boolean = true,
    val callsEnabled: Boolean = true,
    val missedCallsEnabled: Boolean = true,
    val mentionsEnabled: Boolean = true,
    val repliesEnabled: Boolean = true,
    val profileViewsEnabled: Boolean = true,
    val activityLogsEnabled: Boolean = true,
    val systemNudgesEnabled: Boolean = true,
    val chatsEnabled: Boolean = true,
    val smsMmsEnabled: Boolean = true,
    val notesEnabled: Boolean = true,
    val vaultEnabled: Boolean = true,
    val secretsEnabled: Boolean = true,
    val calendarEnabled: Boolean = true,
    val walletEnabled: Boolean = true,
    val financesEnabled: Boolean = true,
    val emailEnabled: Boolean = true,
    val feedEnabled: Boolean = true,
    val booksEnabled: Boolean = true,
    val marketEnabled: Boolean = true,
    val habitsEnabled: Boolean = true,
    val browserEnabled: Boolean = true
) {
    fun toDomain() = UserNotificationSettings(
        allEnabled = allEnabled,
        inAppEnabled = inAppEnabled,
        systemEnabled = systemEnabled,
        fcmEnabled = fcmEnabled,
        sound = sound,
        vibration = vibration,
        showPreview = showPreview,
        hideSensitiveContent = hideSensitiveContent,
        messagesEnabled = messagesEnabled,
        groupsEnabled = groupsEnabled,
        callsEnabled = callsEnabled,
        missedCallsEnabled = missedCallsEnabled,
        mentionsEnabled = mentionsEnabled,
        repliesEnabled = repliesEnabled,
        profileViewsEnabled = profileViewsEnabled,
        activityLogsEnabled = activityLogsEnabled,
        systemNudgesEnabled = systemNudgesEnabled,
        chatsEnabled = chatsEnabled,
        smsMmsEnabled = smsMmsEnabled,
        notesEnabled = notesEnabled,
        vaultEnabled = vaultEnabled,
        secretsEnabled = secretsEnabled,
        calendarEnabled = calendarEnabled,
        walletEnabled = walletEnabled,
        financesEnabled = financesEnabled,
        emailEnabled = emailEnabled,
        feedEnabled = feedEnabled,
        booksEnabled = booksEnabled,
        marketEnabled = marketEnabled,
        habitsEnabled = habitsEnabled,
        browserEnabled = browserEnabled
    )

    companion object {
        fun fromDomain(d: UserNotificationSettings) = UserNotificationSettingsDto(
            allEnabled = d.allEnabled,
            inAppEnabled = d.inAppEnabled,
            systemEnabled = d.systemEnabled,
            fcmEnabled = d.fcmEnabled,
            sound = d.sound,
            vibration = d.vibration,
            showPreview = d.showPreview,
            hideSensitiveContent = d.hideSensitiveContent,
            messagesEnabled = d.messagesEnabled,
            groupsEnabled = d.groupsEnabled,
            callsEnabled = d.callsEnabled,
            missedCallsEnabled = d.missedCallsEnabled,
            mentionsEnabled = d.mentionsEnabled,
            repliesEnabled = d.repliesEnabled,
            profileViewsEnabled = d.profileViewsEnabled,
            activityLogsEnabled = d.activityLogsEnabled,
            systemNudgesEnabled = d.systemNudgesEnabled,
            chatsEnabled = d.chatsEnabled,
            smsMmsEnabled = d.smsMmsEnabled,
            notesEnabled = d.notesEnabled,
            vaultEnabled = d.vaultEnabled,
            secretsEnabled = d.secretsEnabled,
            calendarEnabled = d.calendarEnabled,
            walletEnabled = d.walletEnabled,
            financesEnabled = d.financesEnabled,
            emailEnabled = d.emailEnabled,
            feedEnabled = d.feedEnabled,
            booksEnabled = d.booksEnabled,
            marketEnabled = d.marketEnabled,
            habitsEnabled = d.habitsEnabled,
            browserEnabled = d.browserEnabled
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

@Serializable
data class UserBrowserSettingsDto(
    val unit: String = "C",
    val h24: Boolean = true,
    val fx: Boolean = true,
    val sound: Boolean = false,
    val haptic: Boolean = true,
    val searchEngine: String = "Google",
    val homepage: String = "https://www.google.com",
    val trk: Boolean = true,
    val b3p: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val blockPopups: Boolean = true,
    val doNotTrack: Boolean = true,
    val clearDataOnExit: Boolean = false,
    val saveHistory: Boolean = true,
    val ql: Boolean = true,
    val tabs: Boolean = true,
    val reads: Boolean = true,
    val desktopMode: Boolean = false,
    val openLinksInApp: Boolean = true,
    val downloadPath: String = "Downloads",
    val cacheEnabled: Boolean = true
) {
    fun toDomain() = UserBrowserSettings(
        unit = unit,
        h24 = h24,
        fx = fx,
        sound = sound,
        haptic = haptic,
        searchEngine = searchEngine,
        homepage = homepage,
        trk = trk,
        b3p = b3p,
        javaScriptEnabled = javaScriptEnabled,
        blockPopups = blockPopups,
        doNotTrack = doNotTrack,
        clearDataOnExit = clearDataOnExit,
        saveHistory = saveHistory,
        ql = ql,
        tabs = tabs,
        reads = reads,
        desktopMode = desktopMode,
        openLinksInApp = openLinksInApp,
        downloadPath = downloadPath,
        cacheEnabled = cacheEnabled
    )

    companion object {
        fun fromDomain(d: UserBrowserSettings) = UserBrowserSettingsDto(
            unit = d.unit,
            h24 = d.h24,
            fx = d.fx,
            sound = d.sound,
            haptic = d.haptic,
            searchEngine = d.searchEngine,
            homepage = d.homepage,
            trk = d.trk,
            b3p = d.b3p,
            javaScriptEnabled = d.javaScriptEnabled,
            blockPopups = d.blockPopups,
            doNotTrack = d.doNotTrack,
            clearDataOnExit = d.clearDataOnExit,
            saveHistory = d.saveHistory,
            ql = d.ql,
            tabs = d.tabs,
            reads = d.reads,
            desktopMode = d.desktopMode,
            openLinksInApp = d.openLinksInApp,
            downloadPath = d.downloadPath,
            cacheEnabled = d.cacheEnabled
        )
    }
}

@Serializable
data class UserFeedSettingsDto(
    val autoRefreshEnabled: Boolean = true,
    val defaultLayoutMode: String = "MOSAIC",
    val showWeatherCard: Boolean = true,
    val showPrayerCard: Boolean = true,
    val showVerseCard: Boolean = true,
    val syncFeedsAcrossDevices: Boolean = true
) {
    fun toDomain() = UserFeedSettings(autoRefreshEnabled, defaultLayoutMode, showWeatherCard, showPrayerCard, showVerseCard, syncFeedsAcrossDevices)
    companion object {
        fun fromDomain(d: UserFeedSettings) = UserFeedSettingsDto(d.autoRefreshEnabled, d.defaultLayoutMode, d.showWeatherCard, d.showPrayerCard, d.showVerseCard, d.syncFeedsAcrossDevices)
    }
}

@Serializable
data class BookSettingsDto(
    val showReadMarker: Boolean = true,
    val buttonSizePx: Int = 38,
    val greenOpacityPercent: Int = 50,
    val readMarkerColorHex: String = "#2ECC71",
    val theme: String = "original",
    val pageMarginsEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val autoNextPageOnRead: Boolean = true,
    val volumeKeysTurnPage: Boolean = false,
    val syncAcrossDevices: Boolean = true,
    val encryptFiles: Boolean = true,
    val autoAddPageReference: Boolean = true,
    val trackReadingTime: Boolean = true,
    val dailyGoalMinutes: Int = 30,
    val yearlyGoalBooks: Int = 24,
    val pdfDpiOption: String = "custom",
    val pdfCustomDpi: Int = 400,
    val hasSeenLibraryTour: Boolean = false,
    val hasSeenQuotesCard: Boolean = false,
    val hasSeenGoalsCard: Boolean = false,
    val hasSeenAchievementsCard: Boolean = false,
    val showTimerBadge: Boolean = false,
    val timerBadgeOpacityPercent: Int = 100,
    val readingDirection: String = "AUTO",
    val hasSeenRtlPrompt: Boolean = false
) {
    fun toDomain() = BookSettings(
        showReadMarker = showReadMarker, buttonSizePx = buttonSizePx, greenOpacityPercent = greenOpacityPercent,
        readMarkerColorHex = readMarkerColorHex, theme = theme, pageMarginsEnabled = pageMarginsEnabled,
        keepScreenOn = keepScreenOn, autoNextPageOnRead = autoNextPageOnRead, volumeKeysTurnPage = volumeKeysTurnPage,
        syncAcrossDevices = syncAcrossDevices, encryptFiles = encryptFiles, autoAddPageReference = autoAddPageReference,
        trackReadingTime = trackReadingTime, dailyGoalMinutes = dailyGoalMinutes, yearlyGoalBooks = yearlyGoalBooks,
        pdfDpiOption = pdfDpiOption, pdfCustomDpi = pdfCustomDpi, hasSeenLibraryTour = hasSeenLibraryTour,
        hasSeenQuotesCard = hasSeenQuotesCard, hasSeenGoalsCard = hasSeenGoalsCard, hasSeenAchievementsCard = hasSeenAchievementsCard,
        showTimerBadge = showTimerBadge, timerBadgeOpacityPercent = timerBadgeOpacityPercent,
        readingDirection = readingDirection, hasSeenRtlPrompt = hasSeenRtlPrompt
    )
    companion object {
        fun fromDomain(d: BookSettings) = BookSettingsDto(
            showReadMarker = d.showReadMarker, buttonSizePx = d.buttonSizePx, greenOpacityPercent = d.greenOpacityPercent,
            readMarkerColorHex = d.readMarkerColorHex, theme = d.theme, pageMarginsEnabled = d.pageMarginsEnabled,
            keepScreenOn = d.keepScreenOn, autoNextPageOnRead = d.autoNextPageOnRead, volumeKeysTurnPage = d.volumeKeysTurnPage,
            syncAcrossDevices = d.syncAcrossDevices, encryptFiles = d.encryptFiles, autoAddPageReference = d.autoAddPageReference,
            trackReadingTime = d.trackReadingTime, dailyGoalMinutes = d.dailyGoalMinutes, yearlyGoalBooks = d.yearlyGoalBooks,
            pdfDpiOption = d.pdfDpiOption, pdfCustomDpi = d.pdfCustomDpi, hasSeenLibraryTour = d.hasSeenLibraryTour,
            hasSeenQuotesCard = d.hasSeenQuotesCard, hasSeenGoalsCard = d.hasSeenGoalsCard, hasSeenAchievementsCard = d.hasSeenAchievementsCard,
            showTimerBadge = d.showTimerBadge, timerBadgeOpacityPercent = d.timerBadgeOpacityPercent,
            readingDirection = d.readingDirection, hasSeenRtlPrompt = d.hasSeenRtlPrompt
        )
    }
}

@Serializable
data class UserMarketSettingsDto(
    val defaultCurrency: String = "USD",
    val autoRefreshIntervalSeconds: Int = 30,
    val priceAlertsEnabled: Boolean = true,
    val showTrendCharts: Boolean = true,
    val syncWatchlistsAcrossDevices: Boolean = true
) {
    fun toDomain() = UserMarketSettings(defaultCurrency, autoRefreshIntervalSeconds, priceAlertsEnabled, showTrendCharts, syncWatchlistsAcrossDevices)
    companion object {
        fun fromDomain(d: UserMarketSettings) = UserMarketSettingsDto(d.defaultCurrency, d.autoRefreshIntervalSeconds, d.priceAlertsEnabled, d.showTrendCharts, d.syncWatchlistsAcrossDevices)
    }
}

@Serializable
data class UserSmsSettingsDto(
    val smsBridgeEnabled: Boolean = false,
    val deliveryReportsEnabled: Boolean = true,
    val mmsAutoDownload: Boolean = true,
    val retentionDays: Int = 90,
    val encryptSmsThreads: Boolean = true
) {
    fun toDomain() = UserSmsSettings(smsBridgeEnabled, deliveryReportsEnabled, mmsAutoDownload, retentionDays, encryptSmsThreads)
    companion object {
        fun fromDomain(d: UserSmsSettings) = UserSmsSettingsDto(d.smsBridgeEnabled, d.deliveryReportsEnabled, d.mmsAutoDownload, d.retentionDays, d.encryptSmsThreads)
    }
}

@Serializable
data class UserTodoSettingsDto(
    val defaultPriority: String = "MEDIUM",
    val showCompletedTasks: Boolean = true,
    val dueRemindersEnabled: Boolean = true,
    val weekStartDay: String = "MONDAY",
    val syncTodosAcrossDevices: Boolean = true
) {
    fun toDomain() = UserTodoSettings(defaultPriority, showCompletedTasks, dueRemindersEnabled, weekStartDay, syncTodosAcrossDevices)
    companion object {
        fun fromDomain(d: UserTodoSettings) = UserTodoSettingsDto(d.defaultPriority, d.showCompletedTasks, d.dueRemindersEnabled, d.weekStartDay, d.syncTodosAcrossDevices)
    }
}

@Serializable
data class UserMapSettingsDto(
    val defaultMapType: String = "NORMAL",
    val highAccuracyGps: Boolean = true,
    val offlineCacheEnabled: Boolean = true,
    val distanceUnit: String = "KM",
    val syncLocationsAcrossDevices: Boolean = true
) {
    fun toDomain() = UserMapSettings(defaultMapType, highAccuracyGps, offlineCacheEnabled, distanceUnit, syncLocationsAcrossDevices)
    companion object {
        fun fromDomain(d: UserMapSettings) = UserMapSettingsDto(d.defaultMapType, d.highAccuracyGps, d.offlineCacheEnabled, d.distanceUnit, d.syncLocationsAcrossDevices)
    }
}
