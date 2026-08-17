package com.keeftalk.chat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.keeftalk.chat.data.local.converters.KeeftalkConverters
import com.keeftalk.chat.data.local.dao.*
import com.keeftalk.chat.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        ChatMemberEntity::class,
        ProfileEntity::class,
        MessageReactionEntity::class,
        NoteEntity::class,
        NoteShareEntity::class,
        NotificationEntity::class,
        SyncQueueEntity::class,
        VaultItemEntity::class,
        VaultFolderEntity::class,
        VaultTagEntity::class,
        VaultSyncQueueEntity::class,
        CalendarItemEntity::class,
        ChatListCacheEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        CalendarItemAttendeeEntity::class,
        CalendarReminderEntity::class,
        CalendarCategoryEntity::class,
        FamilyMemberEntity::class,
        FamilyPermissionEntity::class,
        CalendarInvitationEntity::class,
        CalendarSyncQueueEntity::class,
        com.keeftalk.chat.security.crypto.ConversationKeyEntity::class,
        ReceiptSyncQueueEntity::class,
        SmsMessageEntity::class,
        SmsThreadEntity::class,
        MailAccountEntity::class,
        MailFolderEntity::class,
        MailThreadEntity::class,
        MailMessageEntity::class,
        MailAttachmentEntity::class,
        MailLabelEntity::class,
        MailMessageLabelCrossRef::class,
        MailSyncStateEntity::class,
        CallLogEntity::class,
        FileEntity::class,
        MessageAttachmentEntity::class,
        NoteAttachmentEntity::class,
        AgendaAttachmentEntity::class,
        RelationshipCacheEntity::class,
        FeedSourceEntity::class,
        FeedArticleEntity::class,
        WeatherCacheEntity::class,
    ],
    version = 99,
    exportSchema = false
)
@TypeConverters(KeeftalkConverters::class)
abstract class KeeftalkDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun profileDao(): ProfileDao
    abstract fun noteDao(): NoteDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun notificationDao(): NotificationDao
    abstract fun vaultDao(): VaultDao
    abstract fun vaultSyncQueueDao(): VaultSyncQueueDao
    abstract fun calendarSyncQueueDao(): CalendarSyncQueueDao
    abstract fun calendarDao(): CalendarDao
    abstract fun chatListCacheDao(): ChatListCacheDao
    abstract fun conversationKeyDao(): com.keeftalk.chat.security.crypto.ConversationKeyDao
    abstract fun receiptSyncQueueDao(): ReceiptSyncQueueDao
    abstract fun smsDao(): SmsDao
    abstract fun mailDao(): MailDao
    abstract fun callLogDao(): CallLogDao
    abstract fun fileDao(): FileDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun feedDao(): FeedDao

    companion object {
        val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                // Trigger to update chat snippet and timestamp after a message is inserted
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS tr_update_chat_on_message_insert
                    AFTER INSERT ON messages
                    BEGIN
                        UPDATE chats 
                        SET lastMessage = CASE 
                                WHEN NEW.type = 'IMAGE' THEN 'Sent a picture'
                                WHEN NEW.type = 'VIDEO' THEN 'Sent a video'
                                WHEN NEW.type = 'VOICE' THEN 'Sent a voice message'
                                WHEN NEW.type = 'FILE' THEN 'Sent a document'
                                WHEN NEW.type = 'PDF' THEN 'Sent a PDF'
                                ELSE NEW.content 
                            END,
                            lastTimestamp = NEW.timestamp,
                            snippetType = NEW.type,
                            lastMessageSenderId = NEW.senderId,
                            lastMessageStatus = NEW.status,
                            unreadCount = unreadCount + (CASE WHEN NEW.status != 'SEEN' THEN 1 ELSE 0 END)
                        WHERE id = NEW.chatId;
                    END;
                    """.trimIndent()
                )
                
                // Trigger to update chat snippet and timestamp after a message is deleted
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS tr_update_chat_on_message_delete
                    AFTER DELETE ON messages
                    BEGIN
                        UPDATE chats 
                        SET lastMessage = (SELECT content FROM messages WHERE chatId = OLD.chatId ORDER BY timestamp DESC LIMIT 1),
                            lastTimestamp = (SELECT timestamp FROM messages WHERE chatId = OLD.chatId ORDER BY timestamp DESC LIMIT 1),
                            snippetType = (SELECT type FROM messages WHERE chatId = OLD.chatId ORDER BY timestamp DESC LIMIT 1),
                            unreadCount = (CASE WHEN unreadCount > 0 THEN unreadCount - 1 ELSE 0 END)
                        WHERE id = OLD.chatId;
                    END;
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_62_63 = object : Migration(62, 63) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chats ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_63_64 = object : Migration(63, 64) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sms_messages (
                        id INTEGER NOT NULL, 
                        threadId INTEGER NOT NULL, 
                        address TEXT NOT NULL, 
                        body TEXT NOT NULL, 
                        timestamp INTEGER NOT NULL, 
                        read INTEGER NOT NULL, 
                        type INTEGER NOT NULL, 
                        status INTEGER NOT NULL, 
                        isMms INTEGER NOT NULL, 
                        attachmentsJson TEXT, 
                        deliveryStatus INTEGER NOT NULL, 
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_messages_threadId_timestamp ON sms_messages (threadId, timestamp)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sms_threads (
                        threadId INTEGER NOT NULL, 
                        address TEXT NOT NULL, 
                        snippet TEXT NOT NULL, 
                        timestamp INTEGER NOT NULL, 
                        unreadCount INTEGER NOT NULL, 
                        recipientId TEXT, 
                        isArchived INTEGER NOT NULL, 
                        isMuted INTEGER NOT NULL, 
                        isPinned INTEGER NOT NULL, 
                        draft TEXT, 
                        PRIMARY KEY(threadId)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_64_65 = object : Migration(64, 65) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN phone TEXT")
            }
        }

        val MIGRATION_65_66 = object : Migration(65, 66) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN secondaryPhone TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN secondaryPhoneLabel TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN tertiaryPhone TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN tertiaryPhoneLabel TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN email TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN birthday INTEGER")
                db.execSQL("ALTER TABLE users ADD COLUMN callingCard TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN cloudSyncStatus INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_67_68 = object : Migration(67, 68) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS mail_accounts (
                        id TEXT NOT NULL, emailAddress TEXT NOT NULL, provider TEXT NOT NULL, 
                        displayName TEXT NOT NULL, unreadCount INTEGER NOT NULL, 
                        storageUsed INTEGER NOT NULL, storageTotal INTEGER NOT NULL, 
                        refreshToken TEXT, accessToken TEXT, tokenExpiration INTEGER NOT NULL, 
                        lastSyncTimestamp INTEGER NOT NULL, isEnabled INTEGER NOT NULL, 
                        accountColor INTEGER, PRIMARY KEY(id)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS mail_folders (
                        id TEXT NOT NULL, accountId TEXT NOT NULL, name TEXT NOT NULL, 
                        type TEXT NOT NULL, unreadCount INTEGER NOT NULL, 
                        totalCount INTEGER NOT NULL, parentId TEXT, remoteId TEXT, 
                        PRIMARY KEY(id), 
                        FOREIGN KEY(accountId) REFERENCES mail_accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_folders_accountId ON mail_folders (accountId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS mail_messages (
                        id TEXT NOT NULL, accountId TEXT NOT NULL, folderId TEXT NOT NULL, 
                        threadId TEXT NOT NULL, remoteId TEXT NOT NULL, senderName TEXT NOT NULL, 
                        senderEmail TEXT NOT NULL, subject TEXT NOT NULL, snippet TEXT NOT NULL, 
                        content TEXT NOT NULL, htmlContent TEXT, timestamp INTEGER NOT NULL, 
                        isImportant INTEGER NOT NULL, isStarred INTEGER NOT NULL, 
                        hasAttachments INTEGER NOT NULL, isUnread INTEGER NOT NULL, 
                        isDraft INTEGER NOT NULL, isDeleted INTEGER NOT NULL, 
                        PRIMARY KEY(id), 
                        FOREIGN KEY(accountId) REFERENCES mail_accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(folderId) REFERENCES mail_folders(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_messages_accountId ON mail_messages (accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_messages_folderId ON mail_messages (folderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_messages_timestamp ON mail_messages (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_messages_threadId ON mail_messages (threadId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS mail_attachments (
                        id TEXT NOT NULL, messageId TEXT NOT NULL, fileName TEXT NOT NULL, 
                        mimeType TEXT NOT NULL, size INTEGER NOT NULL, localUri TEXT, 
                        remoteUrl TEXT, contentId TEXT, isInline INTEGER NOT NULL, 
                        PRIMARY KEY(id), 
                        FOREIGN KEY(messageId) REFERENCES mail_messages(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_attachments_messageId ON mail_attachments (messageId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS mail_labels (
                        id TEXT NOT NULL, accountId TEXT NOT NULL, name TEXT NOT NULL, 
                        color INTEGER, remoteId TEXT, PRIMARY KEY(id), 
                        FOREIGN KEY(accountId) REFERENCES mail_accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_labels_accountId ON mail_labels (accountId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS mail_message_labels (
                        messageId TEXT NOT NULL, labelId TEXT NOT NULL, 
                        PRIMARY KEY(messageId, labelId), 
                        FOREIGN KEY(messageId) REFERENCES mail_messages(id) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(labelId) REFERENCES mail_labels(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_message_labels_labelId ON mail_message_labels (labelId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS mail_sync_state (
                        accountId TEXT NOT NULL, lastSyncHistoryId TEXT, 
                        deltaToken TEXT, lastUid INTEGER, PRIMARY KEY(accountId), 
                        FOREIGN KEY(accountId) REFERENCES mail_accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_sync_state_accountId ON mail_sync_state (accountId)")
            }
        }

        val MIGRATION_68_69 = object : Migration(68, 69) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mail_accounts ADD COLUMN imapHost TEXT")
                db.execSQL("ALTER TABLE mail_accounts ADD COLUMN imapPort INTEGER")
                db.execSQL("ALTER TABLE mail_accounts ADD COLUMN smtpHost TEXT")
                db.execSQL("ALTER TABLE mail_accounts ADD COLUMN smtpPort INTEGER")
                db.execSQL("ALTER TABLE mail_accounts ADD COLUMN securityType TEXT DEFAULT 'SSL_TLS'")
                db.execSQL("ALTER TABLE mail_accounts ADD COLUMN encryptedPassword TEXT")
            }
        }

        val MIGRATION_69_70 = object : Migration(69, 70) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_chatId_decryptionState_timestamp ON messages (chatId, decryptionState, timestamp)")
            }
        }

        val MIGRATION_70_71 = object : Migration(70, 71) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Update mail_messages
                db.execSQL("ALTER TABLE mail_messages ADD COLUMN recipientsJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE mail_messages ADD COLUMN replyTo TEXT")
                db.execSQL("ALTER TABLE mail_messages ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE mail_messages ADD COLUMN syncState TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_messages_remoteId ON mail_messages (remoteId)")

                // 2. Create mail_threads
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS mail_threads (
                        id TEXT NOT NULL, 
                        accountId TEXT NOT NULL, 
                        subject TEXT NOT NULL, 
                        snippet TEXT NOT NULL, 
                        lastMessageTimestamp INTEGER NOT NULL, 
                        unreadCount INTEGER NOT NULL, 
                        messageCount INTEGER NOT NULL, 
                        isStarred INTEGER NOT NULL DEFAULT 0, 
                        isImportant INTEGER NOT NULL DEFAULT 0, 
                        participantNames TEXT NOT NULL, 
                        PRIMARY KEY(id), 
                        FOREIGN KEY(accountId) REFERENCES mail_accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_threads_accountId ON mail_threads (accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_threads_lastMessageTimestamp ON mail_threads (lastMessageTimestamp)")

                // 3. Recreate mail_sync_state with folderId in PK
                db.execSQL("DROP TABLE IF EXISTS mail_sync_state")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS mail_sync_state (
                        accountId TEXT NOT NULL, 
                        folderId TEXT NOT NULL, 
                        lastSyncHistoryId TEXT, 
                        deltaToken TEXT, 
                        lastUid INTEGER, 
                        uidValidity INTEGER, 
                        PRIMARY KEY(accountId, folderId), 
                        FOREIGN KEY(accountId) REFERENCES mail_accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mail_sync_state_accountId ON mail_sync_state (accountId)")
            }
        }

        val MIGRATION_71_72 = object : Migration(71, 72) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mail_accounts ADD COLUMN profilePicUrl TEXT")
                db.execSQL("ALTER TABLE mail_messages ADD COLUMN senderProfilePicUrl TEXT")
            }
        }

        val MIGRATION_72_73 = object : Migration(72, 73) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mail_messages ADD COLUMN isShared INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_73_74 = object : Migration(73, 74) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS call_logs (
                        id TEXT NOT NULL, 
                        peerId TEXT, 
                        number TEXT NOT NULL, 
                        name TEXT, 
                        timestamp INTEGER NOT NULL, 
                        duration INTEGER NOT NULL, 
                        type TEXT NOT NULL, 
                        isKeeftalk INTEGER NOT NULL, 
                        callType TEXT, 
                        avatarUrl TEXT, 
                        cloudSyncStatus INTEGER NOT NULL DEFAULT 0, 
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_82_83 = object : Migration(82, 83) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add user_id column to conversation_keys
                db.execSQL("ALTER TABLE conversation_keys ADD COLUMN user_id TEXT NOT NULL DEFAULT 'legacy'")
                
                // 2. Drop the old unique index on conversationId
                db.execSQL("DROP INDEX IF EXISTS index_conversation_keys_conversationId")
                
                // 3. Create a new unique composite index on (conversationId, user_id)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_conversation_keys_conversationId_user_id ON conversation_keys (conversationId, user_id)")

                // 4. Add user_id column to vault_folders (Fix for crash)
                db.execSQL("ALTER TABLE vault_folders ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")

                // 5. Add missing indices and column to vault_items
                try {
                    db.execSQL("ALTER TABLE vault_items ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Column might already exist if added manually or in a skipped migration
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_items_file_id ON vault_items (file_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_items_folder_id ON vault_items (folder_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_items_user_id ON vault_items (user_id)")
            }
        }

        val MIGRATION_83_84 = object : Migration(83, 84) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE files ADD COLUMN thumbnail_remote_path TEXT")
                db.execSQL("ALTER TABLE files ADD COLUMN thumbnail_local_path TEXT")
                db.execSQL("ALTER TABLE files ADD COLUMN thumbnail_size INTEGER")
                db.execSQL("ALTER TABLE files ADD COLUMN thumbnail_width INTEGER")
                db.execSQL("ALTER TABLE files ADD COLUMN thumbnail_height INTEGER")
                db.execSQL("ALTER TABLE files ADD COLUMN thumbnail_hmac TEXT")
            }
        }

        val MIGRATION_84_85 = object : Migration(84, 85) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix vault_items missing user_id and indices
                try {
                    db.execSQL("ALTER TABLE vault_items ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Column might already exist
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_items_file_id ON vault_items (file_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_items_folder_id ON vault_items (folder_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_items_user_id ON vault_items (user_id)")
            }
        }

        val MIGRATION_85_86 = object : Migration(85, 86) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix vault_items missing user_id and indices
                try {
                    db.execSQL("ALTER TABLE vault_items ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Column might already exist
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_items_file_id ON vault_items (file_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_items_folder_id ON vault_items (folder_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_items_user_id ON vault_items (user_id)")
            }
        }

        val MIGRATION_86_87 = object : Migration(86, 87) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix message_attachment missing indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_message_attachment_message_id ON message_attachment (message_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_message_attachment_file_id ON message_attachment (file_id)")
                
                // Fix note_attachment missing indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_attachment_note_id ON note_attachment (note_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_attachment_file_id ON note_attachment (file_id)")
                
                // Fix agenda_attachment missing indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_agenda_attachment_agenda_event_id ON agenda_attachment (agenda_event_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_agenda_attachment_file_id ON agenda_attachment (file_id)")
            }
        }

        val MIGRATION_87_88 = object : Migration(87, 88) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS relationship_cache (
                        targetUserId TEXT NOT NULL, 
                        distance INTEGER, 
                        mutualConnectionCount INTEGER NOT NULL, 
                        strengthLabel TEXT, 
                        densityLabel TEXT, 
                        nudgesReceived INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL, 
                        PRIMARY KEY(targetUserId)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_88_89 = object : Migration(88, 89) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE relationship_cache ADD COLUMN nudgesReceived INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_89_90 = object : Migration(89, 90) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notifications ADD COLUMN nudgeCount INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_90_91 = object : Migration(90, 91) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN viewsCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_91_92 = object : Migration(91, 92) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS feed_sources (
                        id TEXT NOT NULL, 
                        url TEXT NOT NULL, 
                        title TEXT NOT NULL, 
                        type TEXT NOT NULL, 
                        iconUrl TEXT, 
                        lastUpdated INTEGER NOT NULL, 
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS feed_articles (
                        id TEXT NOT NULL, 
                        sourceId TEXT NOT NULL, 
                        title TEXT NOT NULL, 
                        description TEXT, 
                        link TEXT NOT NULL, 
                        pubDate INTEGER NOT NULL, 
                        thumbnailUrl TEXT, 
                        content TEXT, 
                        PRIMARY KEY(id), 
                        FOREIGN KEY(sourceId) REFERENCES feed_sources(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_feed_articles_sourceId ON feed_articles (sourceId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS weather_cache (
                        id TEXT NOT NULL, 
                        locationName TEXT NOT NULL, 
                        temperature REAL NOT NULL, 
                        condition TEXT NOT NULL, 
                        iconCode TEXT NOT NULL, 
                        humidity INTEGER NOT NULL, 
                        windSpeed REAL NOT NULL, 
                        apparentTemperature REAL NOT NULL DEFAULT 0.0,
                        uvIndex REAL NOT NULL DEFAULT 0.0,
                        visibility REAL NOT NULL DEFAULT 0.0,
                        pressure REAL NOT NULL DEFAULT 0.0,
                        sunrise TEXT NOT NULL DEFAULT '',
                        sunset TEXT NOT NULL DEFAULT '',
                        isDay INTEGER NOT NULL DEFAULT 1,
                        cloudCover INTEGER NOT NULL DEFAULT 0,
                        precipitation REAL NOT NULL DEFAULT 0.0,
                        timestamp INTEGER NOT NULL, 
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_92_93 = object : Migration(92, 93) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE weather_cache ADD COLUMN apparentTemperature REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE weather_cache ADD COLUMN uvIndex REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE weather_cache ADD COLUMN visibility REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE weather_cache ADD COLUMN pressure REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE weather_cache ADD COLUMN sunrise TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE weather_cache ADD COLUMN sunset TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE weather_cache ADD COLUMN isDay INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE weather_cache ADD COLUMN cloudCover INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_93_94 = object : Migration(93, 94) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE weather_cache ADD COLUMN precipitation REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_94_95 = object : Migration(94, 95) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE feed_articles ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE feed_articles ADD COLUMN readingTimeMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_95_96 = object : Migration(95, 96) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE feed_articles ADD COLUMN author TEXT")
                db.execSQL("ALTER TABLE feed_articles ADD COLUMN isExtracted INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
