package tn.keeftalk.chat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import tn.keeftalk.chat.data.local.converters.KeeftalkConverters
import tn.keeftalk.chat.data.local.dao.*
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.feature.secrets.data.local.dao.SecretsDao
import tn.keeftalk.chat.feature.secrets.data.local.entities.*

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
        CalendarHabitEntity::class,
        CalendarHabitLogEntity::class,
        HabitEntity::class,
        ScheduleVersionEntity::class,
        CheckInEntity::class,
        GraceUseEntity::class,
        MilestoneUnlockEntity::class,
        CalendarItemAttendeeEntity::class,
        CalendarReminderEntity::class,
        CalendarCategoryEntity::class,
        FamilyMemberEntity::class,
        FamilyPermissionEntity::class,
        CalendarInvitationEntity::class,
        CalendarSyncQueueEntity::class,
        tn.keeftalk.chat.security.crypto.ConversationKeyEntity::class,
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
        TimeTrackingEntity::class,
        NoteAttachmentEntity::class,
        AgendaAttachmentEntity::class,
        RelationshipCacheEntity::class,
        FeedSourceEntity::class,
        FeedArticleEntity::class,
        WeatherCacheEntity::class,
        VaultBackupItemEntity::class,
        BookEntity::class,
        BookPageProgressEntity::class,
        BookHighlightEntity::class,
        BookBookmarkEntity::class,
        BookReadingSessionEntity::class,
        BookSyncQueueEntity::class,
        FinanceTransactionEntity::class,
        FinanceBudgetEntity::class,
        FinanceGoalEntity::class,
        FinanceAssetEntity::class,
        FinanceDebtEntity::class,
        FinanceRecurringEntity::class,
        FinanceAccountEntity::class,
        FinanceSharedPersonEntity::class,
        FinanceSharedLogEntity::class,
        FinanceChallengeEntity::class,
        FinanceDocEntity::class,
        FinanceSettingsEntity::class,
        FinanceSalaryEntity::class,
        FinanceWorkLogEntity::class,
        ConfirmedPageEntity::class,
        ConfirmedRangeEntity::class,
        ReadingDayEntity::class,
        UnlockedAchievementEntity::class,
        XpEventEntity::class,
        XpMetaEntity::class,
        ReadingSessionEntity::class,
        SecretCredentialEntity::class,
        SecretPasswordHistoryEntity::class,
        SecretTotpAccountEntity::class,
        SecretBackupCodeEntity::class,
        SecretVaultItemEntity::class,
        SecretSettingsEntity::class,
        TodoTaskEntity::class,
        TodoListEntity::class,
        SavedPlaceEntity::class,
        BrowserHistoryEntity::class,
        MarketListingEntity::class,
        MarketChatEntity::class,
        MarketSellerEntity::class,
        MarketSavedEntity::class,
        DocEntity::class,
        DocBookmarkEntity::class,
        DocHighlightEntity::class,
        DocSyncQueueEntity::class
    ],
    version = 120,
    exportSchema = true
)
@TypeConverters(KeeftalkConverters::class)
abstract class KeeftalkDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun profileDao(): ProfileDao
    abstract fun marketDao(): tn.keeftalk.chat.data.local.dao.MarketDao
    abstract fun noteDao(): NoteDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun notificationDao(): NotificationDao
    abstract fun vaultDao(): VaultDao
    abstract fun vaultSyncQueueDao(): VaultSyncQueueDao
    abstract fun bookDao(): BookDao
    abstract fun docDao(): DocDao
    abstract fun xpDao(): XpDao
    abstract fun vaultBackupDao(): VaultBackupDao
    abstract fun calendarSyncQueueDao(): CalendarSyncQueueDao
    abstract fun calendarDao(): CalendarDao
    abstract fun chatListCacheDao(): ChatListCacheDao
    abstract fun conversationKeyDao(): tn.keeftalk.chat.security.crypto.ConversationKeyDao
    abstract fun nativeHabitDao(): NativeHabitDao
    abstract fun receiptSyncQueueDao(): ReceiptSyncQueueDao
    abstract fun smsDao(): SmsDao
    abstract fun mailDao(): MailDao
    abstract fun callLogDao(): CallLogDao
    abstract fun fileDao(): FileDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun feedDao(): FeedDao
    abstract fun financesDao(): FinancesDao
    abstract fun secretsDao(): SecretsDao
    abstract fun timeTrackingDao(): TimeTrackingDao
    abstract fun todoDao(): TodoDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun browserHistoryDao(): BrowserHistoryDao

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

        val MIGRATION_66_67 = object : Migration(66, 67) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Bridge migration v66 to v67
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

        val MIGRATION_74_82 = object : Migration(74, 82) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safely bridge historical versions 74 to 82
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS conversation_keys (
                            conversationId TEXT NOT NULL,
                            encryptedKey TEXT NOT NULL,
                            keyVersion INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL,
                            PRIMARY KEY(conversationId)
                        )
                    """.trimIndent())
                } catch (_: Exception) {}
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

        val MIGRATION_96_100 = object : Migration(96, 100) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Bridge migration v96 to v100
            }
        }

        val MIGRATION_100_101 = object : Migration(100, 101) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS habits (id TEXT NOT NULL, userId TEXT NOT NULL, name TEXT NOT NULL, icon TEXT NOT NULL, category TEXT NOT NULL, baseDays INTEGER NOT NULL, createdDateEpochDay INTEGER NOT NULL, reminderTimeMinutes INTEGER, slot TEXT NOT NULL, habitType TEXT NOT NULL, updatedAt INTEGER NOT NULL, deleted INTEGER NOT NULL, syncState TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS habit_schedules (id TEXT NOT NULL, habitId TEXT NOT NULL, effectiveFromEpochDay INTEGER NOT NULL, daysMaskString TEXT NOT NULL, updatedAt INTEGER NOT NULL, deleted INTEGER NOT NULL, syncState TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS habit_checkins (id TEXT NOT NULL, habitId TEXT NOT NULL, epochDay INTEGER NOT NULL, completed INTEGER NOT NULL, updatedAt INTEGER NOT NULL, deleted INTEGER NOT NULL, syncState TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS habit_grace_uses (id TEXT NOT NULL, habitId TEXT NOT NULL, forgivenEpochDay INTEGER NOT NULL, usedEpochDay INTEGER NOT NULL, usedMonthKey TEXT NOT NULL, updatedAt INTEGER NOT NULL, deleted INTEGER NOT NULL, syncState TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS habit_milestones (key TEXT NOT NULL, unlockedAtEpochDay INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(key))")
            }
        }

        val MIGRATION_101_102 = object : Migration(101, 102) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Bridge migration v101 to v102
            }
        }

        val MIGRATION_102_103 = object : Migration(102, 103) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN isReadingHabit INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN autoCountReading INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE habits ADD COLUMN linkedBookIdsString TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_103_104 = object : Migration(103, 104) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Bridge migration v103 to v104
            }
        }

        val MIGRATION_104_105 = object : Migration(104, 105) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN content_hash TEXT")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS confirmed_pages (
                        content_hash TEXT NOT NULL,
                        page_key TEXT NOT NULL,
                        credit REAL NOT NULL,
                        dwell_ms INTEGER NOT NULL,
                        confirmed_at INTEGER NOT NULL,
                        PRIMARY KEY(content_hash, page_key)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS confirmed_ranges (
                        content_hash TEXT NOT NULL,
                        start_char INTEGER NOT NULL,
                        end_char INTEGER NOT NULL,
                        confirmed_at INTEGER NOT NULL,
                        PRIMARY KEY(content_hash, start_char, end_char)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reading_days (
                        date TEXT NOT NULL,
                        credited_pages REAL NOT NULL,
                        active_seconds INTEGER NOT NULL,
                        flagged INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(date)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS unlocked_achievements (
                        id TEXT NOT NULL,
                        unlocked_at INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS xp_events (
                        event_id TEXT NOT NULL,
                        content_hash TEXT NOT NULL,
                        page_key_or_range TEXT NOT NULL,
                        dwell_ms INTEGER NOT NULL,
                        anchored_at_ms INTEGER NOT NULL,
                        anchored INTEGER NOT NULL DEFAULT 0,
                        credit REAL NOT NULL,
                        flags INTEGER NOT NULL DEFAULT 0,
                        synced INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(event_id)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS xp_meta (
                        id INTEGER NOT NULL DEFAULT 1,
                        legacy_reading_xp INTEGER NOT NULL,
                        account_tz TEXT NOT NULL,
                        last_global_rank INTEGER NOT NULL DEFAULT 0,
                        last_total_readers INTEGER NOT NULL DEFAULT 0,
                        last_percentile INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())

                var legacyXp = 0L
                try {
                    val cursor = db.query("SELECT hours_read, total_pages, percent_progress FROM books WHERE is_deleted = 0")
                    var totalPagesRead = 0L
                    var totalMinutesRead = 0L
                    while (cursor.moveToNext()) {
                        val hoursRead = cursor.getFloat(0)
                        val totalPages = cursor.getInt(1)
                        val percentProgress = cursor.getInt(2)
                        totalPagesRead += (totalPages * percentProgress / 100).toLong()
                        totalMinutesRead += (hoursRead * 60).toLong()
                    }
                    cursor.close()
                    legacyXp = ((totalMinutesRead * 60L) + totalPagesRead)
                } catch (_: Exception) {}

                val systemTz = try {
                    java.time.ZoneId.systemDefault().id
                } catch (_: Exception) {
                    "UTC"
                }

                db.execSQL("INSERT OR REPLACE INTO xp_meta (id, legacy_reading_xp, account_tz) VALUES (1, $legacyXp, '$systemTz')")

                try {
                    val bookCursor = db.query("SELECT total_pages, percent_progress, hours_read FROM books WHERE is_deleted = 0")
                    var totalPagesRead = 0
                    var totalMinutesRead = 0
                    var finishedCount = 0
                    var librarySize = 0
                    while (bookCursor.moveToNext()) {
                        librarySize++
                        val totalPages = bookCursor.getInt(0)
                        val percentProgress = bookCursor.getInt(1)
                        val hoursRead = bookCursor.getFloat(2)
                        totalPagesRead += (totalPages * percentProgress / 100)
                        totalMinutesRead += (hoursRead * 60).toInt()
                        if (percentProgress >= 100) finishedCount++
                    }
                    bookCursor.close()

                    var bookmarksCount = 0
                    val bmCursor = db.query("SELECT COUNT(*) FROM book_bookmarks")
                    if (bmCursor.moveToFirst()) bookmarksCount = bmCursor.getInt(0)
                    bmCursor.close()

                    var highlightsCount = 0
                    val hlCursor = db.query("SELECT COUNT(*) FROM book_highlights")
                    if (hlCursor.moveToFirst()) highlightsCount = hlCursor.getInt(0)
                    hlCursor.close()

                    val now = System.currentTimeMillis()
                    val grandfatheredIds = mutableListOf<String>()

                    if (totalPagesRead >= 100) grandfatheredIds.add("pages_100")
                    if (totalPagesRead >= 1000) grandfatheredIds.add("pages_1k")
                    if (totalPagesRead >= 10000) grandfatheredIds.add("pages_10k")
                    if (totalPagesRead >= 100000) grandfatheredIds.add("pages_100k")
                    if (totalPagesRead >= 1000000) grandfatheredIds.add("pages_1m")

                    if (bookmarksCount >= 1) grandfatheredIds.add("bm_1")
                    if (bookmarksCount >= 25) grandfatheredIds.add("bm_25")
                    if (bookmarksCount >= 100) grandfatheredIds.add("bm_100")
                    if (bookmarksCount >= 500) grandfatheredIds.add("bm_500")

                    if (highlightsCount >= 1) grandfatheredIds.add("q_1")
                    if (highlightsCount >= 10) grandfatheredIds.add("q_10")
                    if (highlightsCount >= 50) grandfatheredIds.add("q_50")
                    if (highlightsCount >= 100) grandfatheredIds.add("q_100")
                    if (highlightsCount >= 500) grandfatheredIds.add("q_500")

                    if (totalMinutesRead >= 10) grandfatheredIds.add("t_10m")
                    if (totalMinutesRead >= 30) grandfatheredIds.add("t_30m")
                    if (totalMinutesRead >= 90) grandfatheredIds.add("t_90m")
                    if (totalMinutesRead >= 60) grandfatheredIds.add("t_1h")
                    if (totalMinutesRead >= 180) grandfatheredIds.add("t_3h")
                    if (totalMinutesRead >= 300) grandfatheredIds.add("t_5h")
                    if (totalMinutesRead >= 480) grandfatheredIds.add("t_8h")
                    if (totalMinutesRead >= 600) grandfatheredIds.add("t_10h")

                    if (finishedCount >= 1) grandfatheredIds.add("f_1")
                    if (finishedCount >= 5) grandfatheredIds.add("f_5")
                    if (finishedCount >= 10) grandfatheredIds.add("f_10")
                    if (finishedCount >= 25) grandfatheredIds.add("f_25")
                    if (finishedCount >= 50) grandfatheredIds.add("f_50")
                    if (finishedCount >= 100) grandfatheredIds.add("f_100")

                    if (librarySize >= 1) grandfatheredIds.add("lib_1")
                    if (librarySize >= 10) grandfatheredIds.add("lib_10")
                    if (librarySize >= 50) grandfatheredIds.add("lib_50")
                    if (librarySize >= 100) grandfatheredIds.add("lib_100")
                    if (librarySize >= 500) grandfatheredIds.add("lib_500")
                    if (librarySize >= 1000) grandfatheredIds.add("lib_1000")

                    if (grandfatheredIds.size >= 12) grandfatheredIds.add("ach_12")
                    if (grandfatheredIds.size >= 50) grandfatheredIds.add("ach_50")
                    if (grandfatheredIds.size >= 100) grandfatheredIds.add("ach_100")

                    for (id in grandfatheredIds) {
                        db.execSQL("INSERT OR IGNORE INTO unlocked_achievements (id, unlocked_at) VALUES ('$id', $now)")
                    }
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_105_106 = object : Migration(105, 106) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reading_sessions (
                        id TEXT NOT NULL,
                        content_hash TEXT NOT NULL,
                        book_id TEXT NOT NULL,
                        book_title TEXT NOT NULL,
                        book_author TEXT NOT NULL,
                        cover_url TEXT,
                        custom_cover_json TEXT,
                        start_at INTEGER NOT NULL,
                        end_at INTEGER NOT NULL,
                        active_sec INTEGER NOT NULL,
                        pages_confirmed INTEGER NOT NULL,
                        credited_pages REAL NOT NULL,
                        xp_base INTEGER NOT NULL,
                        xp_bonus INTEGER NOT NULL,
                        xp_achievements INTEGER NOT NULL,
                        total_xp_gained INTEGER NOT NULL,
                        flagged INTEGER NOT NULL DEFAULT 0,
                        is_record INTEGER NOT NULL DEFAULT 0,
                        shown INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_106_107 = object : Migration(106, 107) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE finance_transactions ADD COLUMN sourceAccountId TEXT")
                    db.execSQL("ALTER TABLE finance_transactions ADD COLUMN destinationAccountId TEXT")
                    db.execSQL("ALTER TABLE finance_transactions ADD COLUMN relatedDebtId TEXT")
                    db.execSQL("ALTER TABLE finance_transactions ADD COLUMN merchantOrPerson TEXT")
                    db.execSQL("ALTER TABLE finance_transactions ADD COLUMN currency TEXT NOT NULL DEFAULT 'TND'")
                    db.execSQL("ALTER TABLE finance_transactions ADD COLUMN destinationAmount REAL")
                    db.execSQL("ALTER TABLE finance_transactions ADD COLUMN exchangeRate REAL")
                    db.execSQL("ALTER TABLE finance_transactions ADD COLUMN timestamp INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}

                try {
                    db.execSQL("ALTER TABLE finance_accounts ADD COLUMN linkedBankAccountId TEXT")
                    db.execSQL("ALTER TABLE finance_accounts ADD COLUMN creditLimit REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE finance_accounts ADD COLUMN colorHex TEXT")
                    db.execSQL("ALTER TABLE finance_accounts ADD COLUMN iconName TEXT")
                    db.execSQL("ALTER TABLE finance_accounts ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE finance_accounts ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}

                try {
                    db.execSQL("ALTER TABLE finance_debts ADD COLUMN personOrOrg TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE finance_debts ADD COLUMN direction TEXT NOT NULL DEFAULT 'i_owe'")
                    db.execSQL("ALTER TABLE finance_debts ADD COLUMN dateCreated TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE finance_debts ADD COLUMN dueDate TEXT")
                    db.execSQL("ALTER TABLE finance_debts ADD COLUMN paymentSchedule TEXT")
                    db.execSQL("ALTER TABLE finance_debts ADD COLUMN note TEXT")
                    db.execSQL("ALTER TABLE finance_debts ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 1")
                } catch (_: Exception) {}

                try {
                    db.execSQL("ALTER TABLE finance_settings ADD COLUMN lastUsedAccountId TEXT")
                    db.execSQL("ALTER TABLE finance_settings ADD COLUMN lastUsedCategory TEXT")
                } catch (_: Exception) {}

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS finance_salaries (
                        id TEXT NOT NULL,
                        employer TEXT NOT NULL,
                        amount REAL NOT NULL,
                        payPeriod TEXT NOT NULL DEFAULT 'monthly',
                        currency TEXT NOT NULL DEFAULT 'TND',
                        payDay INTEGER NOT NULL DEFAULT 25,
                        startDate TEXT NOT NULL DEFAULT '',
                        workingSchedule TEXT NOT NULL DEFAULT 'Mon-Fri (8h/day)',
                        hoursPerDay REAL NOT NULL DEFAULT 8.0,
                        daysPerWeek INTEGER NOT NULL DEFAULT 5,
                        hourlyRate REAL NOT NULL DEFAULT 0.0,
                        targetAccountId TEXT,
                        expectedRaisePct REAL NOT NULL DEFAULT 0.0,
                        expectedRaiseDate TEXT,
                        active INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS finance_work_logs (
                        date TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'working',
                        customStatusName TEXT,
                        workHours REAL NOT NULL DEFAULT 8.0,
                        overtimeHours REAL NOT NULL DEFAULT 0.0,
                        isPaid INTEGER NOT NULL DEFAULT 1,
                        note TEXT,
                        PRIMARY KEY(date)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_107_108 = object : Migration(107, 108) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op migration from 107 to 108
            }
        }

        val MIGRATION_108_109 = object : Migration(108, 109) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS secret_credentials (
                        id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        service_name TEXT NOT NULL,
                        website TEXT NOT NULL DEFAULT '',
                        username TEXT NOT NULL DEFAULT '',
                        encrypted_password TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT 'Personal',
                        password_strength INTEGER NOT NULL DEFAULT 0,
                        has_two_factor INTEGER NOT NULL DEFAULT 0,
                        last_changed_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        is_deleted INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_secret_credentials_user_id ON secret_credentials(user_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_secret_credentials_category ON secret_credentials(category)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS secret_password_history (
                        id TEXT NOT NULL,
                        credential_id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        encrypted_password TEXT NOT NULL,
                        set_at INTEGER NOT NULL,
                        replaced_at INTEGER NOT NULL,
                        PRIMARY KEY(id),
                        FOREIGN KEY(credential_id) REFERENCES secret_credentials(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_secret_password_history_credential_id ON secret_password_history(credential_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_secret_password_history_user_id ON secret_password_history(user_id)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS secret_totp_accounts (
                        id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        service_name TEXT NOT NULL,
                        username TEXT NOT NULL DEFAULT '',
                        encrypted_secret_key TEXT NOT NULL,
                        issuer TEXT NOT NULL DEFAULT '',
                        type TEXT NOT NULL DEFAULT 'TOTP',
                        algorithm TEXT NOT NULL DEFAULT 'SHA1',
                        digits INTEGER NOT NULL DEFAULT 6,
                        period INTEGER NOT NULL DEFAULT 30,
                        counter INTEGER NOT NULL DEFAULT 0,
                        pin TEXT NOT NULL DEFAULT '',
                        color_hex TEXT NOT NULL DEFAULT '#7D5CFF',
                        created_at INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_secret_totp_accounts_user_id ON secret_totp_accounts(user_id)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS secret_backup_codes (
                        id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        encrypted_code TEXT NOT NULL,
                        is_used INTEGER NOT NULL DEFAULT 0,
                        used_at INTEGER,
                        created_at INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_secret_backup_codes_user_id ON secret_backup_codes(user_id)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS secret_vault_items (
                        id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        item_type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        encrypted_content TEXT NOT NULL,
                        subtitle TEXT NOT NULL DEFAULT '',
                        is_locked INTEGER NOT NULL DEFAULT 1,
                        created_at INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_secret_vault_items_user_id ON secret_vault_items(user_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_secret_vault_items_item_type ON secret_vault_items(item_type)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS secret_settings (
                        user_id TEXT NOT NULL,
                        auto_lock_minutes INTEGER NOT NULL DEFAULT 5,
                        block_screenshots INTEGER NOT NULL DEFAULT 1,
                        clear_clipboard_seconds INTEGER NOT NULL DEFAULT 30,
                        wipe_after_failed_tries INTEGER NOT NULL DEFAULT 1,
                        encrypted_backup_enabled INTEGER NOT NULL DEFAULT 0,
                        biometric_required INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(user_id)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_109_110 = object : Migration(109, 110) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE secret_totp_accounts ADD COLUMN type TEXT NOT NULL DEFAULT 'TOTP'")
                    db.execSQL("ALTER TABLE secret_totp_accounts ADD COLUMN counter INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE secret_totp_accounts ADD COLUMN pin TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_110_111 = object : Migration(110, 111) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS time_tracking_logs (
                            epochDay INTEGER NOT NULL,
                            featureKey TEXT NOT NULL,
                            durationSeconds INTEGER NOT NULL DEFAULT 0,
                            updatedAt INTEGER NOT NULL,
                            PRIMARY KEY(epochDay, featureKey)
                        )
                    """.trimIndent())
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_111_112 = object : Migration(111, 112) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE time_tracking_logs ADD COLUMN pendingSyncSeconds INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_112_113 = object : Migration(112, 113) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS todo_tasks (
                            id TEXT NOT NULL,
                            userId TEXT NOT NULL,
                            title TEXT NOT NULL,
                            notes TEXT NOT NULL DEFAULT '',
                            dueDate TEXT NOT NULL DEFAULT '',
                            priority INTEGER NOT NULL DEFAULT 1,
                            listId TEXT NOT NULL DEFAULT 'personal',
                            completed INTEGER NOT NULL DEFAULT 0,
                            createdAt INTEGER NOT NULL,
                            completedAt INTEGER NOT NULL DEFAULT 0,
                            reminder TEXT NOT NULL DEFAULT '',
                            reminderFired INTEGER NOT NULL DEFAULT 0,
                            updatedAt INTEGER NOT NULL,
                            deleted INTEGER NOT NULL DEFAULT 0,
                            syncState TEXT NOT NULL DEFAULT 'PENDING',
                            PRIMARY KEY(id)
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_todo_tasks_user_id ON todo_tasks(userId)")
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS todo_lists (
                            id TEXT NOT NULL,
                            userId TEXT NOT NULL,
                            name TEXT NOT NULL,
                            colorHex TEXT NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            deleted INTEGER NOT NULL DEFAULT 0,
                            syncState TEXT NOT NULL DEFAULT 'PENDING',
                            PRIMARY KEY(id)
                        )
                    """.trimIndent())
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_113_114 = object : Migration(113, 114) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Bridge migration v113 to v114
            }
        }

        val MIGRATION_114_115 = object : Migration(114, 115) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS saved_places (
                            id TEXT NOT NULL,
                            latitude REAL NOT NULL,
                            longitude REAL NOT NULL,
                            name TEXT,
                            address TEXT,
                            place_id TEXT,
                            provider TEXT,
                            created_at INTEGER NOT NULL,
                            PRIMARY KEY(id)
                        )
                    """.trimIndent())
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_115_116 = object : Migration(115, 116) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS browser_history (
                            id TEXT NOT NULL,
                            url TEXT NOT NULL,
                            title TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            cloudSyncStatus INTEGER NOT NULL DEFAULT 0,
                            PRIMARY KEY(id)
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_browser_history_timestamp ON browser_history(timestamp DESC)")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_116_117 = object : Migration(116, 117) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS market_listings (
                            id TEXT NOT NULL,
                            title TEXT NOT NULL,
                            price REAL NOT NULL,
                            cat TEXT NOT NULL,
                            sub TEXT NOT NULL,
                            cd TEXT NOT NULL,
                            desc TEXT NOT NULL,
                            seller TEXT NOT NULL,
                            as_identity TEXT NOT NULL,
                            shape TEXT NOT NULL DEFAULT 'v',
                            ts INTEGER NOT NULL,
                            exp INTEGER NOT NULL,
                            ext INTEGER NOT NULL DEFAULT 0,
                            media_json TEXT NOT NULL DEFAULT '[]',
                            is_sold INTEGER NOT NULL DEFAULT 0,
                            pos_lat REAL,
                            pos_lng REAL,
                            created_at INTEGER NOT NULL,
                            PRIMARY KEY(id)
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_market_listings_cat ON market_listings(cat)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_market_listings_seller ON market_listings(seller)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_market_listings_is_sold ON market_listings(is_sold)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS market_chats (
                            listing_id TEXT NOT NULL,
                            as_identity TEXT NOT NULL,
                            start INTEGER NOT NULL,
                            status TEXT,
                            messages_json TEXT NOT NULL DEFAULT '[]',
                            review_state_json TEXT,
                            updated_at INTEGER NOT NULL,
                            PRIMARY KEY(listing_id)
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_market_chats_listing_id ON market_chats(listing_id)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS market_sellers (
                            id TEXT NOT NULL,
                            alias TEXT NOT NULL,
                            real TEXT NOT NULL,
                            since INTEGER NOT NULL,
                            ver INTEGER NOT NULL DEFAULT 0,
                            reviews_json TEXT NOT NULL DEFAULT '[]',
                            PRIMARY KEY(id)
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS market_saved (
                            listing_id TEXT NOT NULL,
                            saved_at INTEGER NOT NULL,
                            PRIMARY KEY(listing_id)
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_market_saved_listing_id ON market_saved(listing_id)")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_117_118 = object : Migration(117, 118) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE profiles ADD COLUMN total_xp INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_118_119 = object : Migration(118, 119) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE books ADD COLUMN finished_at INTEGER")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_119_120 = object : Migration(119, 120) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS docs (
                            id TEXT NOT NULL,
                            title TEXT NOT NULL,
                            author_or_source TEXT NOT NULL DEFAULT '',
                            total_pages_or_sections INTEGER NOT NULL DEFAULT 1,
                            format TEXT NOT NULL DEFAULT 'PDF',
                            file_path TEXT NOT NULL DEFAULT '',
                            file_size INTEGER NOT NULL DEFAULT 0,
                            mime_type TEXT NOT NULL DEFAULT '',
                            cover_url TEXT,
                            custom_cover_json TEXT,
                            percent_progress INTEGER NOT NULL DEFAULT 0,
                            last_read_page INTEGER NOT NULL DEFAULT 1,
                            last_read_position_json TEXT,
                            is_favorite INTEGER NOT NULL DEFAULT 0,
                            is_archived INTEGER NOT NULL DEFAULT 0,
                            is_deleted INTEGER NOT NULL DEFAULT 0,
                            content_hash TEXT,
                            created_at INTEGER NOT NULL,
                            updated_at INTEGER NOT NULL,
                            PRIMARY KEY(id)
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_docs_is_archived ON docs(is_archived)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_docs_is_deleted ON docs(is_deleted)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS doc_bookmarks (
                            doc_id TEXT NOT NULL,
                            page_or_position INTEGER NOT NULL,
                            chapter_title TEXT NOT NULL DEFAULT '',
                            excerpt TEXT NOT NULL DEFAULT '',
                            created_at INTEGER NOT NULL,
                            PRIMARY KEY(doc_id, page_or_position)
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_doc_bookmarks_doc_id ON doc_bookmarks(doc_id)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS doc_highlights (
                            id TEXT NOT NULL,
                            doc_id TEXT NOT NULL,
                            page_or_position INTEGER NOT NULL,
                            start_offset INTEGER NOT NULL DEFAULT 0,
                            end_offset INTEGER NOT NULL DEFAULT 0,
                            selected_text TEXT NOT NULL,
                            color_hex TEXT NOT NULL DEFAULT '#2ECC71',
                            note_text TEXT NOT NULL DEFAULT '',
                            created_at INTEGER NOT NULL,
                            PRIMARY KEY(id)
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_doc_highlights_doc_id ON doc_highlights(doc_id)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS doc_sync_queue (
                            id TEXT NOT NULL,
                            action TEXT NOT NULL,
                            entity_type TEXT NOT NULL,
                            payload_json TEXT NOT NULL,
                            status TEXT NOT NULL DEFAULT 'PENDING',
                            created_at INTEGER NOT NULL,
                            PRIMARY KEY(id)
                        )
                    """.trimIndent())
                } catch (_: Exception) {}
            }
        }
    }
}
