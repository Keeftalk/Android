package tn.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finance_transactions")
data class FinanceTransactionEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val date: String, // YYYY-MM-DD
    val title: String,
    val amt: Double,
    val cat: String,
    val type: String, // "exp", "inc", "transfer", "debt_payment", "adjustment"
    val tags: String = "", // comma-separated
    val note: String? = null,
    val rcpt: String? = null,
    val sourceAccountId: String? = null,
    val destinationAccountId: String? = null,
    val relatedDebtId: String? = null,
    val merchantOrPerson: String? = null,
    val currency: String = "TND",
    val destinationAmount: Double? = null,
    val exchangeRate: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "finance_budgets")
data class FinanceBudgetEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val cat: String,
    val limit: Double
)

@Entity(tableName = "finance_goals")
data class FinanceGoalEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "",
    val target: Double,
    val saved: Double = 0.0,
    val monthly: Double = 0.0
)

@Entity(tableName = "finance_assets")
data class FinanceAssetEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val kind: String, // "fund", "stock", "crypto", "cash", "property"
    val value: Double,
    val chg: Double = 0.0
)

@Entity(tableName = "finance_debts")
data class FinanceDebtEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val bal: Double,
    val rate: Double = 0.0,
    val min: Double = 0.0,
    val orig: Double = 0.0,
    val personOrOrg: String = "",
    val direction: String = "i_owe", // "i_owe" or "they_owe"
    val dateCreated: String = "",
    val dueDate: String? = null,
    val paymentSchedule: String? = null,
    val note: String? = null,
    val reminderEnabled: Boolean = true
)

@Entity(tableName = "finance_recurring")
data class FinanceRecurringEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amt: Double,
    val day: Int = 1,
    val kind: String, // "subscription", "bill", "income"
    val on: Boolean = true,
    val unused: Boolean = false
)

@Entity(tableName = "finance_accounts")
data class FinanceAccountEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val kind: String, // "cash", "bank", "credit_card", "debit_card", "wallet", "other"
    val cur: String = "TND",
    val bal: Double = 0.0,
    val num: String? = null, // Masked card/acc number e.g. "•••• 9010"
    val rib: String? = null,
    val bic: String? = null,
    val exp: String? = null,
    val cvv: String? = null, // Kept for schema compatibility, NEVER persisted with non-null secret
    val holder: String? = null,
    val linkedBankAccountId: String? = null,
    val creditLimit: Double = 0.0,
    val colorHex: String? = null,
    val iconName: String? = null,
    val isDefault: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "finance_salaries")
data class FinanceSalaryEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val employer: String,
    val amount: Double,
    val payPeriod: String = "monthly", // "monthly", "weekly", "daily", "hourly"
    val currency: String = "TND",
    val payDay: Int = 25,
    val startDate: String = "",
    val workingSchedule: String = "Mon-Fri (8h/day)",
    val hoursPerDay: Double = 8.0,
    val daysPerWeek: Int = 5,
    val hourlyRate: Double = 0.0,
    val targetAccountId: String? = null,
    val expectedRaisePct: Double = 0.0,
    val expectedRaiseDate: String? = null,
    val active: Boolean = true
)

@Entity(tableName = "finance_work_logs")
data class FinanceWorkLogEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val status: String = "working", // "working", "absent", "vacation", "sick", "holiday", "custom"
    val customStatusName: String? = null,
    val workHours: Double = 8.0,
    val overtimeHours: Double = 0.0,
    val isPaid: Boolean = true,
    val note: String? = null
)

@Entity(tableName = "finance_shared_people")
data class FinanceSharedPersonEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val bal: Double = 0.0
)

@Entity(tableName = "finance_shared_logs")
data class FinanceSharedLogEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amt: Double,
    val paid: String, // "me" or "them"
    val withPerson: String,
    val date: String
)

@Entity(tableName = "finance_challenges")
data class FinanceChallengeEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val target: Double,
    val days: Int,
    val saved: Double = 0.0
)

@Entity(tableName = "finance_docs")
data class FinanceDocEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val kind: String, // "invoice", "payslip", "tax", "contract", "warranty", "receipt"
    val date: String
)

@Entity(tableName = "finance_settings")
data class FinanceSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val rb: Boolean = true,
    val rd: Boolean = true,
    val rs: Boolean = true,
    val hide: Boolean = false,
    val local: Boolean = true,
    val sync: Boolean = false,
    val pin: String = "1234",
    val rem: Int = 3,
    val cur: String = "TND",
    val fch: Int = 30,
    val nwr: Int = 12,
    val nwi: Int = 11,
    val dm: String = "avl",
    val dk: String = "all",
    val widgetsOrder: String = "chart:1,insights:1,where:1,coming:1,summary:1",
    val lastUsedAccountId: String? = null,
    val lastUsedCategory: String? = null
)
