package tn.keeftalk.chat.util.spotlight

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

data class SpotlightTip(
    val id: String,
    val title: String,
    val message: String,
    val targetFeature: String,
    val iconKey: String = "spark"
)

class FeatureSpotlightManager(
    private val context: Context,
    private val userPrefsRepo: UserPreferencesRepository,
    private val timeTrackingDao: TimeTrackingDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = tn.keeftalk.chat.util.KeeftalkStore.getInstance()

    private val _activeSpotlightTip = MutableStateFlow<SpotlightTip?>(null)
    val activeSpotlightTip: StateFlow<SpotlightTip?> = _activeSpotlightTip.asStateFlow()

    fun evaluateSpotlightNudges() {
        scope.launch {
            runCatching {
                val fullSettings = userPrefsRepo.fullSettingsFlow.first()
                if (!fullSettings.notificationSettings.systemNudgesEnabled) return@launch

                val lastNudgeTimeStr = store.getString("last_spotlight_nudge_timestamp", "0")
                val lastNudgeTime = lastNudgeTimeStr?.toLongOrNull() ?: 0L
                val now = System.currentTimeMillis()
                val threeDaysMs = 3 * 86_400_000L

                // Strict 3-day cooldown between spotlight nudges
                if (now - lastNudgeTime < threeDaysMs) return@launch

                val today = LocalDate.now()
                val recentLogs = timeTrackingDao.getLogsForRange(today.minusDays(30).toEpochDay(), today.toEpochDay())
                val featureTotals = recentLogs.groupBy { it.featureKey }.mapValues { (_, logs) -> logs.sumOf { it.durationSeconds } }

                val dismissed = store.getString("dismissed_spotlight_tips_json", "") ?: ""

                val candidateTips = mutableListOf<SpotlightTip>()

                // Rule 1: Active in Habits/Books but no Vault usage
                val vaultSecs = featureTotals["VAULT"] ?: 0L
                if (vaultSecs < 60L && !dismissed.contains("tip_vault_backup")) {
                    candidateTips.add(
                        SpotlightTip(
                            id = "tip_vault_backup",
                            title = "💡 Secure Document Vault",
                            message = "Store your important PDFs and confidential files safely in Cloud Vault with end-to-end hardware encryption.",
                            targetFeature = "VAULT",
                            iconKey = "shield"
                        )
                    )
                }

                // Rule 2: Active in Books but hasn't linked habits
                val bookSecs = featureTotals["BOOKS"] ?: 0L
                val habitSecs = featureTotals["HABITS"] ?: 0L
                if (bookSecs > 300L && habitSecs > 0L && !dismissed.contains("tip_book_habit_link")) {
                    candidateTips.add(
                        SpotlightTip(
                            id = "tip_book_habit_link",
                            title = "💡 Hack: Auto-Log Reading Progress",
                            message = "Link PDF and EPUB books in My Books directly to your reading habit to complete your habit automatically!",
                            targetFeature = "BOOKS",
                            iconKey = "book"
                        )
                    )
                }

                // Rule 3: Unexplored Secrets
                val secretsSecs = featureTotals["SECRETS"] ?: 0L
                if (secretsSecs < 30L && !dismissed.contains("tip_secrets_expiring")) {
                    candidateTips.add(
                        SpotlightTip(
                            id = "tip_secrets_expiring",
                            title = "💡 Security Hack: Auto-Expiring Secrets",
                            message = "Send sensitive notes, PINs, or recovery phrases that self-destruct after viewing or expiration.",
                            targetFeature = "SECRETS",
                            iconKey = "key"
                        )
                    )
                }

                // Rule 4: Unexplored Time on Keeftalk analytics
                val settingsSecs = featureTotals["SETTINGS"] ?: 0L
                if (settingsSecs > 120L && !dismissed.contains("tip_time_analytics")) {
                    candidateTips.add(
                        SpotlightTip(
                            id = "tip_time_analytics",
                            title = "💡 Monitor Screen Time & Stats",
                            message = "Check out 'Time on Keeftalk' in Account Settings to see interactive charts and feature breakdown since day one!",
                            targetFeature = "SETTINGS",
                            iconKey = "trend"
                        )
                    )
                }

                val tipToSurface = candidateTips.firstOrNull() ?: return@launch

                _activeSpotlightTip.value = tipToSurface
                store.putString("last_spotlight_nudge_timestamp", now.toString())

                // Dispatch system notification nudge
                val dispatcher = AppModule.provideNotificationDispatcher(context)
                dispatcher.dispatchDirect(
                    NotificationPayload(
                        feature = NotificationFeature.SYSTEM,
                        type = NotificationType.SYSTEM,
                        priority = NotificationPriority.LOW,
                        title = tipToSurface.title,
                        body = tipToSurface.message
                    )
                )
            }.onFailure { e ->
                Log.d("FeatureSpotlight", "Nudge evaluation notice: ${e.message}")
            }
        }
    }

    fun dismissActiveTip(tipId: String) {
        val currentDismissed = store.getString("dismissed_spotlight_tips_json", "") ?: ""
        store.putString("dismissed_spotlight_tips_json", "$currentDismissed,$tipId")
        _activeSpotlightTip.value = null
    }
}
