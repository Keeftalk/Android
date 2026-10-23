package tn.keeftalk.chat.util.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build

object NotificationChannelManager {

    // Standard Channel IDs for all 15 Features & System
    const val CHANNEL_CHATS = "channel_chats"
    const val CHANNEL_CALLS = "channel_calls_v4"
    const val CHANNEL_SMS = "channel_sms"
    const val CHANNEL_NOTES = "channel_notes"
    const val CHANNEL_VAULT = "channel_vault"
    const val CHANNEL_SECRETS = "channel_secrets"
    const val CHANNEL_CALENDAR = "channel_calendar"
    const val CHANNEL_WALLET = "channel_wallet"
    const val CHANNEL_FINANCES = "channel_finances"
    const val CHANNEL_EMAIL = "channel_email"
    const val CHANNEL_FEED = "channel_feed"
    const val CHANNEL_BOOKS = "channel_books"
    const val CHANNEL_MARKET = "channel_market"
    const val CHANNEL_HABITS = "channel_habits"
    const val CHANNEL_BROWSER = "channel_browser"
    const val CHANNEL_SYSTEM = "channel_system"

    /**
     * Initializes all standard Notification Channels for Keeftalk on Android O+ (API 26+).
     */
    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_CHATS,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for private and group chat messages"
            },

            NotificationChannel(
                CHANNEL_CALLS,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ringtone and heads-up alerts for incoming calls"
                val customRingtonePath = try {
                    tn.keeftalk.chat.di.AppModule.provideExtensionHost(context).registry.soundPackRegistry.resolveSoundPath("incoming_call")
                } catch (_: Exception) {
                    null
                }
                val ringtoneUri = if (!customRingtonePath.isNullOrBlank() && java.io.File(customRingtonePath).exists()) {
                    android.net.Uri.fromFile(java.io.File(customRingtonePath))
                } else {
                    android.provider.Settings.System.DEFAULT_RINGTONE_URI
                }
                setSound(
                    ringtoneUri,
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                )
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },

            NotificationChannel(
                CHANNEL_SMS,
                "SMS & MMS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for text messages and MMS"
            },

            NotificationChannel(
                CHANNEL_NOTES,
                "Notes & Shares",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders and note sharing notifications"
            },

            NotificationChannel(
                CHANNEL_VAULT,
                "Vault & Backups",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for Vault backups and file transfers"
            },

            NotificationChannel(
                CHANNEL_SECRETS,
                "Secrets & Security",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for secret expirations and security events"
            },

            NotificationChannel(
                CHANNEL_CALENDAR,
                "Calendar & Events",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled event reminders and invitations"
            },

            NotificationChannel(
                CHANNEL_WALLET,
                "Wallet & Cards",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for wallet, cards, and payment activity"
            },

            NotificationChannel(
                CHANNEL_FINANCES,
                "Finances & Budgets",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Budget limit warnings, debt due dates, and salary alerts"
            },

            NotificationChannel(
                CHANNEL_EMAIL,
                "E-Mail",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for incoming emails and sync"
            },

            NotificationChannel(
                CHANNEL_FEED,
                "Feed & Prayer Times",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Feed updates and daily prayer time reminders"
            },

            NotificationChannel(
                CHANNEL_BOOKS,
                "Books & Reading Goals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reading goal reminders and reading streak milestones"
            },

            NotificationChannel(
                CHANNEL_MARKET,
                "Marketplace",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Listing updates, orders, and buyer/seller messages"
            },

            NotificationChannel(
                CHANNEL_HABITS,
                "Habits & Grace Tokens",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Habit slot start, slot expiry warnings, and grace tactic prompts"
            },

            NotificationChannel(
                CHANNEL_BROWSER,
                "Browser Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress and completion status"
            },

            NotificationChannel(
                CHANNEL_SYSTEM,
                "System & Activity",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Profile views, security logins, and system nudges"
            }
        )

        manager.createNotificationChannels(channels)

        // Ensure legacy channel IDs mapped cleanly
        val legacyChats = NotificationChannel("chat_messages", "Chat Messages", NotificationManager.IMPORTANCE_HIGH)
        val legacyCalls = NotificationChannel("incoming_calls_v3", "Incoming Calls", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(legacyChats)
        manager.createNotificationChannel(legacyCalls)
    }

    /**
     * Helper to select the correct NotificationChannel ID for a given feature.
     */
    fun getChannelIdForFeature(featureKey: String): String {
        return when (featureKey.lowercase()) {
            "chats", "chat", "message" -> CHANNEL_CHATS
            "calls", "call" -> CHANNEL_CALLS
            "sms", "mms", "sms_mms" -> CHANNEL_SMS
            "notes", "note" -> CHANNEL_NOTES
            "vault" -> CHANNEL_VAULT
            "secrets", "secret" -> CHANNEL_SECRETS
            "calendar" -> CHANNEL_CALENDAR
            "wallet" -> CHANNEL_WALLET
            "finances", "finance" -> CHANNEL_FINANCES
            "email", "mail" -> CHANNEL_EMAIL
            "feed", "prayer" -> CHANNEL_FEED
            "books", "book", "reading" -> CHANNEL_BOOKS
            "market", "marketplace" -> CHANNEL_MARKET
            "habits", "habit" -> CHANNEL_HABITS
            "browser", "download" -> CHANNEL_BROWSER
            else -> CHANNEL_SYSTEM
        }
    }
}
