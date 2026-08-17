package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserNoteSettings(
    // Behavior
    val defaultVisibility: String = "PRIVATE", // PRIVATE, SHARED, PUBLIC
    val defaultEditorMode: String = "RICH_TEXT", // PLAIN_TEXT, RICH_TEXT, MARKDOWN
    val autoSaveEnabled: Boolean = true,
    val autoSaveIntervalSeconds: Int = 30,
    val defaultSorting: String = "UPDATED_AT", // CREATED_AT, UPDATED_AT, TITLE
    val defaultView: String = "GRID", // LIST, GRID
    val archiveBehavior: String = "MOVE_TO_ARCHIVE",
    val trashRetentionDays: Int = 30,
    
    // Sync
    val syncNotesAcrossDevices: Boolean = true,
    val offlineNotesEnabled: Boolean = true,
    val syncAttachments: Boolean = true,
    val conflictResolution: String = "NEWER_WINS", // NEWER_WINS, MANUAL, MERGE
    
    // Security
    val lockNotesByDefault: Boolean = false,
    val requireAuthToOpen: Boolean = false,
    val encryptionType: String = "E2EE", // E2EE, STANDARD
    val autoLockTimeoutMinutes: Int = 5,
    
    // Storage
    val attachmentLimitMb: Int = 25,
    val clearCacheOnExit: Boolean = false
)
