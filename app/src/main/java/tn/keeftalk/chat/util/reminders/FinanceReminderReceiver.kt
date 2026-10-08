package tn.keeftalk.chat.util.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.NotificationPriority
import tn.keeftalk.chat.domain.model.NotificationType
import tn.keeftalk.chat.util.notifications.NotificationFeature
import tn.keeftalk.chat.util.notifications.NotificationPayload

class FinanceReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.intent.action.MY_PACKAGE_REPLACED"
        ) {
            return
        }

        val reminderType = intent.getStringExtra("finance_type") ?: "GENERAL"
        val itemId = intent.getStringExtra("item_id")

        val (title, body) = when (reminderType) {
            "BUDGET_LIMIT" -> "💳 Budget Limit Alert" to "A budget limit threshold was reached in My Finances."
            "DEBT_DUE" -> "💳 Debt Payoff Reminder" to "A scheduled debt payment is due today."
            "SALARY" -> "💳 Salary Deposit Alert" to "Expected salary deposit date in My Finances."
            else -> "💳 Financial Reminder" to "Scheduled financial update in My Finances."
        }

        val dispatcher = AppModule.provideNotificationDispatcher(context)
        dispatcher.dispatch(
            NotificationPayload(
                feature = NotificationFeature.FINANCES,
                type = NotificationType.REMINDER,
                priority = NotificationPriority.NORMAL,
                title = title,
                body = body,
                sourceId = itemId,
                isSensitive = true
            )
        )
    }
}
