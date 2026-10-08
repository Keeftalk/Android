package tn.keeftalk.chat.util.spotlight

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.local.dao.TimeTrackingDao
import tn.keeftalk.chat.data.prefs.UserPreferencesRepository
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.NotificationPriority
import tn.keeftalk.chat.domain.model.NotificationType
import tn.keeftalk.chat.util.notifications.NotificationFeature
import tn.keeftalk.chat.util.notifications.NotificationPayload
import java.time.LocalDate

data class NotificationNudgeSpec(
    val id: String,
    val feature: NotificationFeature,
    val title: String,
    val body: String
)

class FeatureSpotlightNotificationEngine(
    private val context: Context,
    private val userPrefsRepo: UserPreferencesRepository,
    private val timeTrackingDao: TimeTrackingDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = tn.keeftalk.chat.util.KeeftalkStore.getInstance()

    fun evaluateAndDispatchNotificationNudge() {
        scope.launch {
            runCatching {
                val fullSettings = userPrefsRepo.fullSettingsFlow.first()
                if (!fullSettings.notificationSettings.systemNudgesEnabled) return@launch

                val lastNudgeTimeStr = store.getString("last_notification_nudge_timestamp", "0")
                val lastNudgeTime = lastNudgeTimeStr?.toLongOrNull() ?: 0L
                val now = System.currentTimeMillis()
                val threeDaysMs = 3 * 86_400_000L

                // Enforce strict 3-day cooldown between system notification nudges
                if (now - lastNudgeTime < threeDaysMs) return@launch

                val today = LocalDate.now()
                val recentLogs = timeTrackingDao.getLogsForRange(today.minusDays(30).toEpochDay(), today.toEpochDay())
                val featureTotals = recentLogs.groupBy { it.featureKey }.mapValues { (_, logs) -> logs.sumOf { it.durationSeconds } }

                val dismissed = store.getString("dismissed_notification_nudges_json", "") ?: ""

                val candidates = mutableListOf<NotificationNudgeSpec>()

                // Rule 1: Vault Tip
                if ((featureTotals["VAULT"] ?: 0L) < 60L && !dismissed.contains("nudge_vault")) {
                    candidates.add(
                        NotificationNudgeSpec(
                            id = "nudge_vault",
                            feature = NotificationFeature.VAULT,
                            title = "Secure Document Vault",
                            body = "Store your important files safely in Cloud Vault with end-to-end hardware encryption."
                        )
                    )
                }

                // Rule 2: Book & Habit Link
                if ((featureTotals["BOOKS"] ?: 0L) > 300L && (featureTotals["HABITS"] ?: 0L) > 0L && !dismissed.contains("nudge_book_habit")) {
                    candidates.add(
                        NotificationNudgeSpec(
                            id = "nudge_book_habit",
                            feature = NotificationFeature.BOOKS,
                            title = "Auto-Log Progress",
                            body = "Link PDF and EPUB books in My Books directly to your reading habit to complete it automatically!"
                        )
                    )
                }

                // Rule 3: Secrets Tip
                if ((featureTotals["SECRETS"] ?: 0L) < 30L && !dismissed.contains("nudge_secrets")) {
                    candidates.add(
                        NotificationNudgeSpec(
                            id = "nudge_secrets",
                            feature = NotificationFeature.SECRETS,
                            title = "Auto-Expiring Secrets",
                            body = "Send sensitive notes, PINs, or recovery phrases that self-destruct after viewing."
                        )
                    )
                }

                // Rule 4: Time on Keeftalk
                if ((featureTotals["SETTINGS"] ?: 0L) > 120L && !dismissed.contains("nudge_time_stats")) {
                    candidates.add(
                        NotificationNudgeSpec(
                            id = "nudge_time_stats",
                            feature = NotificationFeature.SYSTEM,
                            title = "Time on Keeftalk Analytics",
                            body = "Check out Account Settings > Time on Keeftalk to see interactive charts and feature time breakdown!"
                        )
                    )
                }

                val nudge = candidates.firstOrNull() ?: return@launch

                store.putString("last_notification_nudge_timestamp", now.toString())
                store.putString("dismissed_notification_nudges_json", "$dismissed,${nudge.id}")

                val dispatcher = AppModule.provideNotificationDispatcher(context)
                dispatcher.dispatchDirect(
                    NotificationPayload(
                        feature = nudge.feature,
                        type = NotificationType.SYSTEM,
                        priority = NotificationPriority.LOW,
                        title = nudge.title,
                        body = nudge.body
                    )
                )
            }.onFailure { e ->
                Log.d("SpotlightNotification", "Nudge error notice: ${e.message}")
            }
        }
    }
}
