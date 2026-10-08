package tn.keeftalk.chat.feature.finances.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.feature.finances.repository.FinancesRepository
import androidx.compose.ui.graphics.vector.ImageVector
import tn.keeftalk.chat.feature.finances.ui.FinancesIcons
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class CategoryMeta(val icon: ImageVector, val label: String, val colorHex: String)

data class ChatMessage(val sender: String, val text: String)

data class ScenarioComparison(
    val name: String,
    val description: String,
    val year1: Double,
    val year5: Double,
    val yearsToFi: Double?
)

data class OcrResult(
    val merchant: String,
    val amount: Double,
    val category: String,
    val items: List<String>
)

data class WorkMonthStats(
    val yearMonth: String,
    val totalDaysInMonth: Int,
    val workingDaysCount: Int,
    val absentDaysCount: Int,
    val paidLeaveCount: Int,
    val unpaidLeaveCount: Int,
    val totalWorkHours: Double,
    val totalOvertimeHours: Double,
    val attendancePercentage: Double,
    val expectedMonthlySalary: Double,
    val calculatedActualSalary: Double
)

data class ThisMonthSummary(
    val salaryReceived: Double,
    val expectedSalary: Double,
    val otherIncome: Double,
    val totalIncome: Double,
    val expenses: Double,
    val debtPayments: Double,
    val transfers: Double,
    val netCashFlow: Double
)

data class FinancesUiState(
    val transactions: List<FinanceTransactionEntity> = emptyList(),
    val budgets: List<FinanceBudgetEntity> = emptyList(),
    val goals: List<FinanceGoalEntity> = emptyList(),
    val assets: List<FinanceAssetEntity> = emptyList(),
    val debts: List<FinanceDebtEntity> = emptyList(),
    val recurring: List<FinanceRecurringEntity> = emptyList(),
    val accounts: List<FinanceAccountEntity> = emptyList(),
    val salaries: List<FinanceSalaryEntity> = emptyList(),
    val workLogs: List<FinanceWorkLogEntity> = emptyList(),
    val sharedPeople: List<FinanceSharedPersonEntity> = emptyList(),
    val sharedLogs: List<FinanceSharedLogEntity> = emptyList(),
    val challenges: List<FinanceChallengeEntity> = emptyList(),
    val docs: List<FinanceDocEntity> = emptyList(),
    val settings: FinanceSettingsEntity = FinanceSettingsEntity(),

    // UI Navigation & View State
    val activeTab: String = "home", // "home", "spend", "plan", "wealth"
    val activeToolScreen: String? = null,
    val activeSheet: String? = null,

    // Filters
    val period: String = "month", // "month" or "year"
    val searchQuery: String = "",
    val categoryFilter: String = "all",
    val typeFilter: String = "all",
    val accountFilter: String = "all",
    val docKindFilter: String = "all",
    val selectedCalendarDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
    val selectedCalendarMonth: String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date()),

    // Simulators & Sliders State
    val extraSavings: Double = 0.0,
    val salaryRaise: Double = 0.0,
    val oneOffPurchase: Double = 0.0,
    val expectedReturn: Double = 6.0,
    val extraDebtPayment: Double = 0.0,
    val forecastDays: Int = 30,
    val netWorthMonths: Int = 12,
    val netWorthScrubIndex: Int = 11,
    val debtMethod: String = "avl", // "avl" (Avalanche) or "snow" (Snowball)
    val savedScenarios: List<ScenarioComparison> = emptyList(),

    // AI Assistant
    val chatHistory: List<ChatMessage> = listOf(
        ChatMessage("ai", "Hi! Ask me about your accounts, net worth, salary, or spending.")
    ),

    // Security & Locks
    val isVaultUnlocked: Boolean = false,
    val unlockTimeMillis: Long = 0L,

    // OCR Receipt Scan
    val ocrState: String? = null,
    val ocrResult: OcrResult? = null,

    // Widgets order
    val widgetsOrder: List<Pair<String, Boolean>> = listOf(
        "chart" to true,
        "insights" to true,
        "where" to true,
        "coming" to true,
        "summary" to true
    ),

    val exportImportText: String = "",
    val toastMessage: String? = null
)

class FinancesViewModel(
    private val repository: FinancesRepository
) : ViewModel() {

    companion object {
        val CATEGORIES = mapOf(
            "rent" to CategoryMeta(FinancesIcons.Housing, "Housing", "#7D5CFF"),
            "food" to CategoryMeta(FinancesIcons.Food, "Food", "#00CCCC"),
            "transport" to CategoryMeta(FinancesIcons.Transport, "Transport", "#329FE0"),
            "fun" to CategoryMeta(FinancesIcons.Fun, "Fun", "#FF7AB6"),
            "health" to CategoryMeta(FinancesIcons.Health, "Health", "#2ECC71"),
            "shop" to CategoryMeta(FinancesIcons.Shopping, "Shopping", "#FFB020"),
            "bills" to CategoryMeta(FinancesIcons.Bills, "Bills", "#6472F5"),
            "income" to CategoryMeta(FinancesIcons.Income, "Income", "#2ECC71")
        )
        val EXPENSE_CATEGORIES = listOf("rent", "food", "transport", "fun", "health", "shop", "bills")
        val EXCHANGE_RATES = mapOf("USD" to 1.0, "EUR" to 0.92, "GBP" to 0.79, "TND" to 3.1, "JPY" to 150.0)
    }

    private val _uiState = MutableStateFlow(FinancesUiState())
    val uiState: StateFlow<FinancesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getTransactions(),
                repository.getBudgets(),
                repository.getGoals(),
                repository.getAssets(),
                repository.getDebts()
            ) { txs, budgets, goals, assets, debts ->
                Triple(txs, budgets, goals) to (assets to debts)
            }.combine(
                combine(repository.getRecurring(), repository.getAccounts(), repository.getSalaries(), repository.getWorkLogs()) { rec, acc, sal, wl ->
                    Tuple4(rec, acc, sal, wl)
                }
            ) { (t1, t2), (rec, acc, sal, wl) ->
                val (txs, budgets, goals) = t1
                val (assets, debts) = t2
                _uiState.update { current ->
                    current.copy(
                        transactions = txs,
                        budgets = budgets,
                        goals = goals,
                        assets = assets,
                        debts = debts,
                        recurring = rec,
                        accounts = acc,
                        salaries = sal,
                        workLogs = wl
                    )
                }
            }.collect()
        }

        viewModelScope.launch {
            combine(
                repository.getSharedPeople(),
                repository.getSharedLogs(),
                repository.getChallenges(),
                repository.getDocs(),
                repository.getSettings()
            ) { people, logs, challenges, docs, settings ->
                _uiState.update { current ->
                    val updatedSettings = settings ?: FinanceSettingsEntity()
                    val widgetList = parseWidgetsOrder(updatedSettings.widgetsOrder)
                    current.copy(
                        sharedPeople = people,
                        sharedLogs = logs,
                        challenges = challenges,
                        docs = docs,
                        settings = updatedSettings,
                        widgetsOrder = widgetList
                    )
                }
            }.collect()
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private fun parseWidgetsOrder(raw: String): List<Pair<String, Boolean>> {
        return try {
            raw.split(",").mapNotNull { part ->
                val tokens = part.split(":")
                if (tokens.size == 2) tokens[0] to (tokens[1] == "1") else null
            }
        } catch (e: Exception) {
            listOf("chart" to true, "insights" to true, "where" to true, "coming" to true, "summary" to true)
        }
    }

    private fun serializeWidgetsOrder(list: List<Pair<String, Boolean>>): String {
        return list.joinToString(",") { "${it.first}:${if (it.second) "1" else "0"}" }
    }

    // ─── ACTIONS & NAVIGATION ───

    fun selectTab(tab: String) { _uiState.update { it.copy(activeTab = tab, activeToolScreen = null) } }
    fun openToolScreen(tool: String) { _uiState.update { it.copy(activeToolScreen = tool) } }
    fun closeToolScreen() { _uiState.update { it.copy(activeToolScreen = null) } }
    fun openSheet(type: String) { _uiState.update { it.copy(activeSheet = type) } }
    fun closeSheet() { _uiState.update { it.copy(activeSheet = null) } }
    fun setPeriod(p: String) { _uiState.update { it.copy(period = p) } }
    fun setSearchQuery(q: String) { _uiState.update { it.copy(searchQuery = q) } }
    fun setCategoryFilter(c: String) { _uiState.update { it.copy(categoryFilter = c) } }
    fun setTypeFilter(t: String) { _uiState.update { it.copy(typeFilter = t) } }
    fun setAccountFilter(a: String) { _uiState.update { it.copy(accountFilter = a) } }
    fun setDocKindFilter(k: String) { _uiState.update { it.copy(docKindFilter = k) } }
    fun setSelectedCalendarDay(d: Int) { _uiState.update { it.copy(selectedCalendarDay = d) } }
    fun setSelectedCalendarMonth(ym: String) { _uiState.update { it.copy(selectedCalendarMonth = ym) } }

    fun setExtraSavings(v: Double) { _uiState.update { it.copy(extraSavings = v) } }
    fun setSalaryRaise(v: Double) { _uiState.update { it.copy(salaryRaise = v) } }
    fun setOneOffPurchase(v: Double) { _uiState.update { it.copy(oneOffPurchase = v) } }
    fun setExpectedReturn(v: Double) { _uiState.update { it.copy(expectedReturn = v) } }
    fun setExtraDebtPayment(v: Double) { _uiState.update { it.copy(extraDebtPayment = v) } }
    fun setForecastDays(v: Int) { _uiState.update { it.copy(forecastDays = v) } }
    fun setNetWorthMonths(v: Int) { _uiState.update { it.copy(netWorthMonths = v, netWorthScrubIndex = v - 1) } }
    fun setNetWorthScrubIndex(v: Int) { _uiState.update { it.copy(netWorthScrubIndex = v) } }
    fun setDebtMethod(v: String) { _uiState.update { it.copy(debtMethod = v) } }

    fun showToast(msg: String) { _uiState.update { it.copy(toastMessage = msg) } }
    fun clearToast() { _uiState.update { it.copy(toastMessage = null) } }

    fun checkPin(enteredPin: String): Boolean {
        val currentPin = _uiState.value.settings.pin
        return if (enteredPin == currentPin) {
            _uiState.update { it.copy(isVaultUnlocked = true, unlockTimeMillis = System.currentTimeMillis() + 30000L) }
            true
        } else {
            showToast("Wrong PIN")
            false
        }
    }

    fun lockVault() { _uiState.update { it.copy(isVaultUnlocked = false) } }

    fun updatePin(newPin: String) {
        if (newPin.length == 4) {
            val updated = _uiState.value.settings.copy(pin = newPin)
            viewModelScope.launch {
                repository.saveSettings(updated)
                showToast("PIN updated")
            }
        }
    }

    fun setCurrency(newCur: String) {
        val updated = _uiState.value.settings.copy(cur = newCur)
        viewModelScope.launch {
            repository.saveSettings(updated)
            showToast("Display currency changed to $newCur")
        }
    }

    fun toggleHideAmounts() {
        val updated = _uiState.value.settings.copy(hide = !_uiState.value.settings.hide)
        viewModelScope.launch { repository.saveSettings(updated) }
    }

    fun toggleSetting(key: String) {
        val s = _uiState.value.settings
        val updated = when (key) {
            "rb" -> s.copy(rb = !s.rb)
            "rd" -> s.copy(rd = !s.rd)
            "rs" -> s.copy(rs = !s.rs)
            "local" -> s.copy(local = !s.local, sync = if (!s.local) false else s.sync)
            "sync" -> s.copy(sync = !s.sync, local = if (!s.sync) false else s.local)
            else -> s
        }
        viewModelScope.launch { repository.saveSettings(updated) }
    }

    fun updateReminderDays(days: Int) {
        val updated = _uiState.value.settings.copy(rem = days)
        viewModelScope.launch { repository.saveSettings(updated) }
    }

    fun moveWidget(index: Int, direction: Int) {
        val list = _uiState.value.widgetsOrder.toMutableList()
        val targetIndex = index + direction
        if (targetIndex in list.indices) {
            val temp = list[index]
            list[index] = list[targetIndex]
            list[targetIndex] = temp
            _uiState.update { it.copy(widgetsOrder = list) }
            val updatedSettings = _uiState.value.settings.copy(widgetsOrder = serializeWidgetsOrder(list))
            viewModelScope.launch { repository.saveSettings(updatedSettings) }
        }
    }

    fun toggleWidget(index: Int) {
        val list = _uiState.value.widgetsOrder.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].first to !list[index].second
            _uiState.update { it.copy(widgetsOrder = list) }
            val updatedSettings = _uiState.value.settings.copy(widgetsOrder = serializeWidgetsOrder(list))
            viewModelScope.launch { repository.saveSettings(updatedSettings) }
        }
    }

    // ─── UNIFIED LEDGER & ACCOUNT MUTATIONS ───

    private suspend fun updateAccountBalanceForTx(tx: FinanceTransactionEntity, revert: Boolean = false) {
        val mult = if (revert) -1.0 else 1.0

        // 1. Source Account
        if (!tx.sourceAccountId.isNullOrEmpty()) {
            val srcAcc = _uiState.value.accounts.find { it.id == tx.sourceAccountId }
            if (srcAcc != null) {
                val updatedBal = when (tx.type) {
                    "exp" -> {
                        if (srcAcc.kind == "credit_card" || srcAcc.kind == "card") {
                            srcAcc.bal + (tx.amt * mult)
                        } else if (srcAcc.kind == "debit_card" && !srcAcc.linkedBankAccountId.isNullOrEmpty()) {
                            val parentBank = _uiState.value.accounts.find { it.id == srcAcc.linkedBankAccountId }
                            if (parentBank != null) {
                                repository.saveAccount(parentBank.copy(bal = parentBank.bal - (tx.amt * mult), updatedAt = System.currentTimeMillis()))
                            }
                            srcAcc.bal
                        } else {
                            srcAcc.bal - (tx.amt * mult)
                        }
                    }
                    "transfer" -> srcAcc.bal - (tx.amt * mult)
                    "debt_payment" -> srcAcc.bal - (tx.amt * mult)
                    "adjustment" -> srcAcc.bal + (tx.amt * mult)
                    "inc" -> srcAcc.bal + (tx.amt * mult)
                    else -> srcAcc.bal
                }
                repository.saveAccount(srcAcc.copy(bal = updatedBal, updatedAt = System.currentTimeMillis()))
            }
        }

        // 2. Destination Account
        if (!tx.destinationAccountId.isNullOrEmpty()) {
            val destAcc = _uiState.value.accounts.find { it.id == tx.destinationAccountId }
            if (destAcc != null) {
                val destAmt = tx.destinationAmount ?: tx.amt
                val updatedBal = when (tx.type) {
                    "inc" -> destAcc.bal + (destAmt * mult)
                    "transfer" -> destAcc.bal + (destAmt * mult)
                    "exp" -> destAcc.bal - (destAmt * mult)
                    else -> destAcc.bal
                }
                repository.saveAccount(destAcc.copy(bal = updatedBal, updatedAt = System.currentTimeMillis()))
            }
        }

        // 3. Related Debt
        if (!tx.relatedDebtId.isNullOrEmpty()) {
            val debt = _uiState.value.debts.find { it.id == tx.relatedDebtId }
            if (debt != null) {
                val newBal = max(0.0, debt.bal - (tx.amt * mult))
                repository.saveDebt(debt.copy(bal = newBal))
            }
        }
    }

    fun addExpense(
        title: String,
        amt: Double,
        cat: String,
        date: String,
        sourceAccountId: String? = null,
        tags: String = "",
        note: String? = null,
        merchantOrPerson: String? = null
    ) {
        viewModelScope.launch {
            val accId = sourceAccountId ?: _uiState.value.settings.lastUsedAccountId ?: _uiState.value.accounts.firstOrNull()?.id
            val tagList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val tx = FinanceTransactionEntity(
                date = date,
                title = title,
                amt = amt,
                cat = cat,
                type = "exp",
                tags = tagList.joinToString(","),
                note = note,
                sourceAccountId = accId,
                merchantOrPerson = merchantOrPerson,
                currency = _uiState.value.settings.cur
            )
            repository.saveTransaction(tx)
            updateAccountBalanceForTx(tx)

            if (accId != null) {
                repository.saveSettings(_uiState.value.settings.copy(lastUsedAccountId = accId, lastUsedCategory = cat))
            }

            closeSheet()
            showToast("Expense saved")
        }
    }

    fun addIncome(
        source: String,
        amt: Double,
        date: String,
        destinationAccountId: String? = null,
        cat: String = "income",
        note: String? = null
    ) {
        viewModelScope.launch {
            val accId = destinationAccountId ?: _uiState.value.settings.lastUsedAccountId ?: _uiState.value.accounts.firstOrNull()?.id
            val tx = FinanceTransactionEntity(
                date = date,
                title = source,
                amt = amt,
                cat = cat,
                type = "inc",
                destinationAccountId = accId,
                note = note,
                currency = _uiState.value.settings.cur
            )
            repository.saveTransaction(tx)
            updateAccountBalanceForTx(tx)

            if (accId != null) {
                repository.saveSettings(_uiState.value.settings.copy(lastUsedAccountId = accId))
            }

            closeSheet()
            showToast("Income saved")
        }
    }

    fun addTransfer(
        title: String,
        amt: Double,
        date: String,
        sourceAccountId: String,
        destinationAccountId: String,
        destinationAmount: Double? = null,
        note: String? = null
    ) {
        viewModelScope.launch {
            val destAmt = destinationAmount ?: amt
            val tx = FinanceTransactionEntity(
                date = date,
                title = title.ifBlank { "Transfer" },
                amt = amt,
                cat = "bills",
                type = "transfer",
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                destinationAmount = destAmt,
                note = note,
                currency = _uiState.value.settings.cur
            )
            repository.saveTransaction(tx)
            updateAccountBalanceForTx(tx)
            closeSheet()
            showToast("Transfer recorded")
        }
    }

    fun recordDebtPayment(
        debtId: String,
        amt: Double,
        date: String,
        sourceAccountId: String,
        note: String? = null
    ) {
        val debt = _uiState.value.debts.find { it.id == debtId } ?: return
        viewModelScope.launch {
            val tx = FinanceTransactionEntity(
                date = date,
                title = "Paid ${debt.personOrOrg.ifBlank { debt.title }}",
                amt = amt,
                cat = "bills",
                type = "debt_payment",
                sourceAccountId = sourceAccountId,
                relatedDebtId = debtId,
                note = note,
                currency = _uiState.value.settings.cur
            )
            repository.saveTransaction(tx)
            updateAccountBalanceForTx(tx)
            closeSheet()
            showToast("Debt payment recorded")
        }
    }

    fun reconcileAccount(
        accountId: String,
        actualBalance: Double,
        note: String? = null
    ) {
        val acc = _uiState.value.accounts.find { it.id == accountId } ?: return
        val diff = actualBalance - acc.bal
        if (Math.abs(diff) < 0.001) {
            showToast("Account balance matches!")
            return
        }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        viewModelScope.launch {
            val tx = FinanceTransactionEntity(
                date = todayStr,
                title = "Balance Reconciliation (${if (diff > 0) "+" else ""}${formatCurrency(diff)})",
                amt = diff,
                cat = "bills",
                type = "adjustment",
                sourceAccountId = accountId,
                note = note ?: "Manual balance adjustment",
                currency = acc.cur
            )
            repository.saveTransaction(tx)
            repository.saveAccount(acc.copy(bal = actualBalance, updatedAt = System.currentTimeMillis()))
            showToast("Account reconciled to ${formatCurrency(actualBalance)}")
        }
    }

    fun deleteTransaction(id: String) {
        val tx = _uiState.value.transactions.find { it.id == id } ?: return
        viewModelScope.launch {
            updateAccountBalanceForTx(tx, revert = true)
            repository.deleteTransactionById(id)
            showToast("Transaction deleted")
        }
    }

    // ─── SALARY & EMPLOYMENT ───

    fun addSalary(
        employer: String,
        amount: Double,
        payPeriod: String = "monthly",
        currency: String = "TND",
        payDay: Int = 25,
        startDate: String = "",
        workingSchedule: String = "Mon-Fri (8h/day)",
        hoursPerDay: Double = 8.0,
        daysPerWeek: Int = 5,
        hourlyRate: Double = 0.0,
        targetAccountId: String? = null,
        expectedRaisePct: Double = 0.0,
        expectedRaiseDate: String? = null
    ) {
        viewModelScope.launch {
            val salary = FinanceSalaryEntity(
                employer = employer,
                amount = amount,
                payPeriod = payPeriod,
                currency = currency,
                payDay = payDay,
                startDate = startDate,
                workingSchedule = workingSchedule,
                hoursPerDay = hoursPerDay,
                daysPerWeek = daysPerWeek,
                hourlyRate = hourlyRate,
                targetAccountId = targetAccountId ?: _uiState.value.accounts.firstOrNull()?.id,
                expectedRaisePct = expectedRaisePct,
                expectedRaiseDate = expectedRaiseDate,
                active = true
            )
            repository.saveSalary(salary)
            closeSheet()
            showToast("Salary saved")
        }
    }

    fun recordSalaryReceived(
        salaryId: String,
        actualAmount: Double,
        date: String,
        targetAccountId: String? = null,
        note: String? = null
    ) {
        val salary = _uiState.value.salaries.find { it.id == salaryId }
        val employerName = salary?.employer ?: "Employer"
        val accId = targetAccountId ?: salary?.targetAccountId ?: _uiState.value.accounts.firstOrNull()?.id

        viewModelScope.launch {
            val tx = FinanceTransactionEntity(
                date = date,
                title = "Salary received — $employerName",
                amt = actualAmount,
                cat = "income",
                type = "inc",
                destinationAccountId = accId,
                note = note,
                currency = salary?.currency ?: _uiState.value.settings.cur
            )
            repository.saveTransaction(tx)
            updateAccountBalanceForTx(tx)
            showToast("Salary recorded")
        }
    }

    fun deleteSalary(salaryId: String) {
        val sal = _uiState.value.salaries.find { it.id == salaryId } ?: return
        viewModelScope.launch {
            repository.deleteSalary(sal)
            showToast("Salary deleted")
        }
    }

    // ─── WORK LOGS & CALENDAR ───

    fun setWorkLog(
        date: String,
        status: String,
        workHours: Double = 8.0,
        overtimeHours: Double = 0.0,
        isPaid: Boolean = true,
        note: String? = null,
        customStatusName: String? = null
    ) {
        viewModelScope.launch {
            val log = FinanceWorkLogEntity(
                date = date,
                status = status,
                customStatusName = customStatusName,
                workHours = workHours,
                overtimeHours = overtimeHours,
                isPaid = isPaid,
                note = note
            )
            repository.saveWorkLog(log)
            showToast("Work status updated")
        }
    }

    fun deleteWorkLog(date: String) {
        val log = _uiState.value.workLogs.find { it.date == date } ?: return
        viewModelScope.launch {
            repository.deleteWorkLog(log)
            showToast("Work status cleared")
        }
    }

    fun computeWorkStats(yearMonth: String): WorkMonthStats {
        val logs = _uiState.value.workLogs.filter { it.date.startsWith(yearMonth) }
        val cal = Calendar.getInstance()
        val parts = yearMonth.split("-")
        if (parts.size == 2) {
            val y = parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR)
            val m = (parts[1].toIntOrNull() ?: 1) - 1
            cal.set(y, m, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        var workingCount = 0
        var absentCount = 0
        var paidLeaveCount = 0
        var unpaidLeaveCount = 0
        var totalHours = 0.0
        var totalOvertime = 0.0

        logs.forEach { l ->
            when (l.status) {
                "working" -> {
                    workingCount++
                    totalHours += l.workHours
                    totalOvertime += l.overtimeHours
                }
                "absent" -> {
                    absentCount++
                    if (l.isPaid) paidLeaveCount++ else unpaidLeaveCount++
                }
                "vacation", "sick", "holiday", "custom" -> {
                    if (l.isPaid) paidLeaveCount++ else unpaidLeaveCount++
                }
            }
        }

        val totalExpectedWorkDays = max(1, daysInMonth * 5 / 7)
        val attendance = min(100.0, ((workingCount + paidLeaveCount).toDouble() / totalExpectedWorkDays) * 100.0)

        val mainSalary = _uiState.value.salaries.firstOrNull { it.active }
        val expectedSal = mainSalary?.amount ?: 0.0
        var actualSal = expectedSal
        if (mainSalary != null && unpaidLeaveCount > 0) {
            val dailyDeduction = expectedSal / totalExpectedWorkDays
            actualSal = max(0.0, expectedSal - (dailyDeduction * unpaidLeaveCount))
        }

        return WorkMonthStats(
            yearMonth = yearMonth,
            totalDaysInMonth = daysInMonth,
            workingDaysCount = workingCount,
            absentDaysCount = absentCount,
            paidLeaveCount = paidLeaveCount,
            unpaidLeaveCount = unpaidLeaveCount,
            totalWorkHours = totalHours,
            totalOvertimeHours = totalOvertime,
            attendancePercentage = attendance,
            expectedMonthlySalary = expectedSal,
            calculatedActualSalary = actualSal
        )
    }

    // ─── BUDGETS, GOALS, ASSETS, DEBTS & RECURRING ───

    fun addBudget(cat: String, limit: Double) {
        viewModelScope.launch {
            repository.saveBudget(FinanceBudgetEntity(cat = cat, limit = limit))
            closeSheet()
            showToast("Budget saved")
        }
    }

    fun addGoal(title: String, target: Double, saved: Double, monthly: Double) {
        viewModelScope.launch {
            repository.saveGoal(FinanceGoalEntity(title = title, emoji = "", target = target, saved = saved, monthly = monthly))
            closeSheet()
            showToast("Goal saved")
        }
    }

    fun fundGoal(goalId: String, amount: Double = 100.0, sourceAccountId: String? = null) {
        val goal = _uiState.value.goals.find { it.id == goalId } ?: return
        val newSaved = min(goal.target, goal.saved + amount)
        val accId = sourceAccountId ?: _uiState.value.accounts.firstOrNull()?.id
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        viewModelScope.launch {
            repository.saveGoal(goal.copy(saved = newSaved))
            val tx = FinanceTransactionEntity(
                date = todayStr,
                title = "Saved → ${goal.title}",
                amt = amount,
                cat = "bills",
                type = "exp",
                sourceAccountId = accId
            )
            repository.saveTransaction(tx)
            updateAccountBalanceForTx(tx)
            val isComplete = newSaved >= goal.target
            showToast(if (isComplete) "Goal reached! 🎉" else "Added ${formatCurrency(amount)} to ${goal.title}")
        }
    }

    fun addAsset(title: String, kind: String, value: Double) {
        viewModelScope.launch {
            repository.saveAsset(FinanceAssetEntity(title = title, kind = kind, value = value, chg = 0.0))
            closeSheet()
            showToast("Asset saved")
        }
    }

    fun addDebt(
        personOrOrg: String,
        bal: Double,
        direction: String = "i_owe",
        rate: Double = 0.0,
        min: Double = 0.0,
        dueDate: String? = null,
        paymentSchedule: String? = null,
        note: String? = null
    ) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        viewModelScope.launch {
            repository.saveDebt(
                FinanceDebtEntity(
                    title = personOrOrg,
                    personOrOrg = personOrOrg,
                    bal = bal,
                    orig = bal,
                    rate = rate,
                    min = min,
                    direction = direction,
                    dateCreated = todayStr,
                    dueDate = dueDate,
                    paymentSchedule = paymentSchedule,
                    note = note
                )
            )
            closeSheet()
            showToast("Debt saved")
        }
    }

    fun addRecurring(title: String, amt: Double, day: Int, kind: String) {
        viewModelScope.launch {
            repository.saveRecurring(FinanceRecurringEntity(title = title, amt = amt, day = day, kind = kind, on = true))
            closeSheet()
            showToast("Recurring item saved")
        }
    }

    fun toggleRecurring(recId: String) {
        val rec = _uiState.value.recurring.find { it.id == recId } ?: return
        val updated = rec.copy(on = !rec.on)
        viewModelScope.launch {
            repository.saveRecurring(updated)
            showToast(if (updated.on) "${rec.title} resumed" else "${rec.title} paused")
        }
    }

    fun addAccount(
        title: String,
        kind: String,
        cur: String,
        bal: Double,
        num: String?,
        rib: String?,
        bic: String?,
        exp: String?,
        cvv: String?,
        holder: String?,
        linkedBankAccountId: String? = null,
        creditLimit: Double = 0.0
    ) {
        // Mask account number safely
        val maskedNum = formatMaskedCardNumber(num)
        viewModelScope.launch {
            repository.saveAccount(
                FinanceAccountEntity(
                    title = title,
                    kind = kind,
                    cur = cur,
                    bal = bal,
                    num = maskedNum,
                    rib = rib,
                    bic = bic,
                    exp = exp,
                    cvv = null, // NEVER persist CVV
                    holder = holder,
                    linkedBankAccountId = linkedBankAccountId,
                    creditLimit = creditLimit
                )
            )
            closeSheet()
            showToast("Account / Card saved")
        }
    }

    fun addShared(title: String, amt: Double, withPerson: String, paidBy: String) {
        val person = _uiState.value.sharedPeople.find { it.name == withPerson }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        viewModelScope.launch {
            if (person != null) {
                val delta = (if (paidBy == "me") 1.0 else -1.0) * amt / 2.0
                repository.saveSharedPerson(person.copy(bal = person.bal + delta))
            }
            repository.saveSharedLog(
                FinanceSharedLogEntity(title = title, amt = amt, paid = paidBy, withPerson = withPerson, date = todayStr)
            )
            closeSheet()
            showToast("Split recorded")
        }
    }

    fun settleSharedPerson(personName: String) {
        val person = _uiState.value.sharedPeople.find { it.name == personName } ?: return
        viewModelScope.launch {
            repository.saveSharedPerson(person.copy(bal = 0.0))
            showToast("Settled with $personName")
        }
    }

    fun addChallenge(title: String, target: Double, days: Int) {
        viewModelScope.launch {
            repository.saveChallenge(FinanceChallengeEntity(title = title, target = target, days = days, saved = 0.0))
            closeSheet()
            showToast("Challenge created")
        }
    }

    fun depositChallenge(challengeId: String) {
        val c = _uiState.value.challenges.find { it.id == challengeId } ?: return
        val depositAmt = c.target / c.days
        val newSaved = min(c.target, c.saved + depositAmt)
        viewModelScope.launch {
            repository.saveChallenge(c.copy(saved = newSaved))
            showToast(if (newSaved >= c.target) "Challenge complete! 🏆" else "Deposit logged")
        }
    }

    fun addDoc(title: String, kind: String, date: String) {
        viewModelScope.launch {
            repository.saveDoc(FinanceDocEntity(title = title, kind = kind, date = date))
            closeSheet()
            showToast("Document saved")
        }
    }

    // ─── QUICK ADD PARSER ───

    fun quickAdd(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val lower = trimmed.lowercase()
        val accounts = _uiState.value.accounts

        val matchedAccount = accounts.find { acc ->
            lower.contains(acc.title.lowercase()) ||
            (acc.kind == "cash" && lower.contains("cash")) ||
            (acc.kind == "bank" && lower.contains("bank")) ||
            (acc.kind == "wallet" && lower.contains("wallet")) ||
            (acc.num != null && acc.num.length >= 4 && lower.contains(acc.num.takeLast(4))) ||
            (lower.contains("visa") && acc.title.lowercase().contains("visa")) ||
            (lower.contains("mastercard") && acc.title.lowercase().contains("mastercard"))
        } ?: accounts.firstOrNull()

        if (lower.startsWith("paid") || lower.startsWith("pay ")) {
            val debts = _uiState.value.debts
            val matchedDebt = debts.find { d -> lower.contains(d.personOrOrg.lowercase()) || lower.contains(d.title.lowercase()) }
            val regex = Regex("(\\d+(?:[.,]\\d+)?)")
            val match = regex.find(trimmed)
            if (match != null && matchedDebt != null) {
                val amt = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
                recordDebtPayment(
                    debtId = matchedDebt.id,
                    amt = amt,
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                    sourceAccountId = matchedAccount?.id ?: ""
                )
                return
            }
        }

        val regex = Regex("(\\d+(?:[.,]\\d+)?)")
        val match = regex.find(trimmed)
        if (match == null) {
            showToast("Include an amount, e.g. 'coffee 4.5 food'")
            return
        }
        val amt = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
        val isIncome = trimmed.startsWith("+") || lower.contains("salary") || lower.contains("bonus") || lower.contains("freelance")

        val words = trimmed.replace(match.value, "").replace("+", "").trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val catKey = EXPENSE_CATEGORIES.find { cat ->
            words.any { w ->
                val meta = CATEGORIES[cat]
                meta?.label?.lowercase()?.startsWith(w.lowercase()) == true || w.lowercase() == cat
            }
        } ?: "shop"

        val titleText = words.filter { w ->
            val meta = CATEGORIES[catKey]
            meta?.label?.lowercase()?.startsWith(w.lowercase()) != true &&
            w.lowercase() != catKey &&
            w.lowercase() != "cash" &&
            w.lowercase() != "visa" &&
            w.lowercase() != "bank" &&
            w.lowercase() != "wallet"
        }.joinToString(" ")

        val finalTitle = if (titleText.isNotBlank()) titleText.replaceFirstChar { it.uppercase() } else if (isIncome) "Income" else CATEGORIES[catKey]?.label ?: "Expense"
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (isIncome) {
            addIncome(
                source = finalTitle,
                amt = amt,
                date = todayStr,
                destinationAccountId = matchedAccount?.id,
                cat = if (lower.contains("salary")) "income" else "income"
            )
        } else {
            addExpense(
                title = finalTitle,
                amt = amt,
                cat = catKey,
                date = todayStr,
                sourceAccountId = matchedAccount?.id
            )
        }
    }

    fun startOcrScan() {
        _uiState.update { it.copy(ocrState = "busy") }
        viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            val emptyResult = OcrResult("", 0.0, "food", emptyList())
            _uiState.update { it.copy(ocrState = "done", ocrResult = emptyResult) }
        }
    }

    fun saveOcrReceipt(merchant: String, amount: Double, category: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val docId = UUID.randomUUID().toString()
        val itemsStr = _uiState.value.ocrResult?.items?.joinToString(", ") ?: ""
        viewModelScope.launch {
            repository.saveDoc(FinanceDocEntity(id = docId, title = "Receipt — $merchant", kind = "receipt", date = todayStr))
            addExpense(title = merchant, amt = amount, cat = category, date = todayStr, tags = "receipt", note = itemsStr)
            _uiState.update { it.copy(ocrState = null, ocrResult = null) }
            showToast("Expense saved with receipt attached")
        }
    }

    fun askAi(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        val currentChat = _uiState.value.chatHistory + ChatMessage("me", trimmed)
        val answer = computeAiAnswer(trimmed)
        _uiState.update { it.copy(chatHistory = currentChat + ChatMessage("ai", answer)) }
    }

    private fun computeAiAnswer(q: String): String {
        val query = q.lowercase()
        val numRegex = Regex("(\\d[\\d,.]*)")
        val match = numRegex.find(query)
        val num = match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        val currentMonthTxs = getMonthTransactions(0)
        val spentMap = EXPENSE_CATEGORIES.map { c -> c to getSpentByCategory(currentMonthTxs, c) }.sortedByDescending { it.second }

        if (query.contains("afford") || query.contains("buy") || query.contains("purchase")) {
            if (num > 0) {
                val currentLiquid = computeLiquidMoney()
                val left = currentLiquid - num
                val mo = left / max(1.0, computeAverageMonthlySpend(6))
                val freeSurplus = computeSurplus()
                val monthsToSave = if (freeSurplus > 0) (num / freeSurplus).toInt() else 0
                val statusText = when {
                    left < 0 -> "Not from available liquid money alone"
                    mo >= 3 -> "Yes, you can afford it"
                    else -> "You can, but it's tight"
                }
                return "$statusText: ${formatCurrency(num)} leaves ${formatCurrency(left)} in liquid funds (${String.format(Locale.US, "%.1f", mo)} months of spending). At your free ${formatCurrency(max(0.0, freeSurplus))}/month, saving takes $monthsToSave months."
            }
        }
        if (query.contains("where") || query.contains("most") || query.contains("went") || query.contains("spend") || query.contains("spent")) {
            val top3 = spentMap.take(3).joinToString(", ") { (c, v) ->
                val meta = CATEGORIES[c]
                "${meta?.label} ${formatCurrency(v)}"
            }
            return "This month you've spent ${formatCurrency(computeExpenses(currentMonthTxs))}. Top categories: $top3."
        }
        if (query.contains("worth") || query.contains("wealth")) {
            return "Your net worth is ${formatCurrency(computeNetWorth())}: ${formatCurrency(computeLiquidMoney())} in liquid funds and ${formatCurrency(computeAssetsTotal())} in assets, minus ${formatCurrency(computeTotalDebtOwed())} total debt."
        }
        if (query.contains("salary") || query.contains("pay") || query.contains("income")) {
            val mainSalary = _uiState.value.salaries.firstOrNull { it.active }
            val summary = computeThisMonthSummary()
            return if (mainSalary != null) {
                "Your expected salary is ${formatCurrency(mainSalary.amount)} from ${mainSalary.employer}. So far this month you've received ${formatCurrency(summary.salaryReceived)} in salary."
            } else {
                "You haven't added a employment/salary record yet. Add it from Plan → Work & Salary."
            }
        }
        if (query.contains("debt") || query.contains("owe")) {
            val totalD = computeTotalDebtOwed()
            return "Total debt owed: ${formatCurrency(totalD)} (${formatCurrency(computeCreditCardDebt())} credit card, ${formatCurrency(computePersonalDebtOwed())} personal)."
        }
        return "Try asking: “Where did most of my money go?”, “What is my net worth?”, “What is my salary status?”, or “Can I afford a $1,000 purchase?”"
    }

    // ─── CALCULATIONS & FINANCIAL MATH ───

    fun formatCurrency(amount: Double, decimals: Int = 0): String {
        val curCode = _uiState.value.settings.cur
        val rate = EXCHANGE_RATES[curCode] ?: 1.0
        val converted = amount * rate
        val nf = NumberFormat.getCurrencyInstance(Locale.US)
        nf.currency = Currency.getInstance(curCode)
        nf.maximumFractionDigits = decimals
        nf.minimumFractionDigits = decimals
        return nf.format(converted)
    }

    fun formatCompactCurrency(amount: Double): String {
        val absVal = Math.abs(amount)
        return if (absVal >= 1000) {
            String.format(Locale.US, "%.1fk", amount / 1000.0).replace(".0k", "k")
        } else {
            amount.roundToInt().toString()
        }
    }

    fun formatMaskedCardNumber(num: String?): String {
        if (num.isNullOrBlank()) return "•••• 0000"
        val digits = num.filter { it.isDigit() }
        val last4 = if (digits.length >= 4) digits.takeLast(4) else "0000"
        return "•••• $last4"
    }

    fun getMonthTransactions(monthOffset: Int): List<FinanceTransactionEntity> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthOffset)
        val ym = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
        return _uiState.value.transactions.filter { it.date.startsWith(ym) }
    }

    fun computeIncome(txs: List<FinanceTransactionEntity>): Double = txs.filter { it.type == "inc" }.sumOf { it.amt }
    fun computeExpenses(txs: List<FinanceTransactionEntity>): Double = txs.filter { it.type == "exp" }.sumOf { it.amt }
    fun getSpentByCategory(txs: List<FinanceTransactionEntity>, cat: String): Double = txs.filter { it.type == "exp" && it.cat == cat }.sumOf { it.amt }

    fun computeLiquidMoney(): Double {
        val accounts = _uiState.value.accounts
        return if (accounts.isNotEmpty()) {
            accounts.filter { it.kind != "credit_card" && it.kind != "card" && it.bal > 0 }.sumOf { acc ->
                val rate = EXCHANGE_RATES[acc.cur] ?: 1.0
                acc.bal / rate
            }
        } else 0.0
    }

    fun computeCreditCardDebt(): Double {
        val accounts = _uiState.value.accounts
        return accounts.filter { it.kind == "credit_card" || it.kind == "card" }.sumOf { acc ->
            val rate = EXCHANGE_RATES[acc.cur] ?: 1.0
            acc.bal / rate
        }
    }

    fun computePersonalDebtOwed(): Double {
        return _uiState.value.debts.filter { it.direction == "i_owe" }.sumOf { it.bal }
    }

    fun computeTotalDebtOwed(): Double {
        return computeCreditCardDebt() + computePersonalDebtOwed() + _uiState.value.debts.filter { it.direction != "they_owe" && it.direction != "i_owe" }.sumOf { it.bal }
    }

    fun computeAssetsTotal(): Double = _uiState.value.assets.sumOf { it.value }

    fun computeNetWorth(): Double = computeLiquidMoney() + computeAssetsTotal() - computeTotalDebtOwed()

    fun computeInvested(): Double = computeAssetsTotal() - _uiState.value.assets.filter { it.kind == "cash" }.sumOf { it.value }

    fun computeThisMonthSummary(): ThisMonthSummary {
        val monthTxs = getMonthTransactions(0)
        val salaryReceived = monthTxs.filter { it.type == "inc" && (it.cat == "salary" || it.title.lowercase().contains("salary")) }.sumOf { it.amt }
        val expectedSalary = _uiState.value.salaries.filter { it.active }.sumOf { it.amount }
        val otherIncome = monthTxs.filter { it.type == "inc" && it.cat != "salary" && !it.title.lowercase().contains("salary") }.sumOf { it.amt }
        val expenses = monthTxs.filter { it.type == "exp" }.sumOf { it.amt }
        val debtPayments = monthTxs.filter { it.type == "debt_payment" }.sumOf { it.amt }
        val transfers = monthTxs.filter { it.type == "transfer" }.sumOf { it.amt }
        val totalIncome = salaryReceived + otherIncome
        val netCashFlow = totalIncome - expenses - debtPayments

        return ThisMonthSummary(
            salaryReceived = salaryReceived,
            expectedSalary = expectedSalary,
            otherIncome = otherIncome,
            totalIncome = totalIncome,
            expenses = expenses,
            debtPayments = debtPayments,
            transfers = transfers,
            netCashFlow = netCashFlow
        )
    }

    fun computeMonthlySummaries(count: Int): List<MonthlySummary> {
        return (0 until count).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -(count - 1 - offset))
            val label = SimpleDateFormat("MMM", Locale.US).format(cal.time)
            val monthTxs = getMonthTransactions(count - 1 - offset)
            MonthlySummary(label, computeIncome(monthTxs), computeExpenses(monthTxs))
        }
    }

    fun computeAverageMonthlySpend(monthCount: Int = 6): Double {
        val ms = computeMonthlySummaries(monthCount)
        val sum = ms.sumOf { it.exp }
        return if (ms.isNotEmpty()) sum / ms.size else 0.0
    }

    fun computeSurplus(): Double {
        val salaryInc = _uiState.value.salaries.filter { it.active }.sumOf { it.amount }
        val expRec = _uiState.value.recurring.filter { it.on && it.kind != "income" }.sumOf { it.amt }
        val avg3Mo = (0..2).map { getMonthTransactions(it) }.map { computeExpenses(it) }.average()
        val varSpend = max(0.0, avg3Mo - expRec)
        return salaryInc - expRec - varSpend
    }

    fun computeProjectedNetWorth(months: Int, extra: Double = 0.0, raise: Double = 0.0, buy: Double = 0.0): Double {
        val r = _uiState.value.expectedReturn / 1200.0
        val g = (1.0 + r).pow(months.toDouble()) - 1.0
        val inv0 = computeInvested()
        val salaryInc = _uiState.value.salaries.filter { it.active }.sumOf { it.amount }
        val surplusVal = computeSurplus()
        val monthContribution = surplusVal + (salaryInc * raise / 100.0)
        return computeNetWorth() + (monthContribution * months) + (inv0 * g) + (if (r > 0) extra * g / r else extra * months) - buy
    }

    fun computeForecast(days: Int): List<Double> {
        var b = computeLiquidMoney()
        val avgDailyVar = (computeAverageMonthlySpend(3) - _uiState.value.recurring.filter { it.on && it.kind != "income" }.sumOf { it.amt }) / 30.4
        val cal = Calendar.getInstance()
        val res = mutableListOf(b)

        val recEvents = _uiState.value.recurring.filter { it.on }.map { r ->
            r.day to (if (r.kind == "income") r.amt else -r.amt)
        }
        val debtEvents = _uiState.value.debts.mapIndexed { idx, d ->
            (5 + idx * 7) to -d.min
        }
        val allEvents = recEvents + debtEvents

        for (i in 1..days) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val dom = cal.get(Calendar.DAY_OF_MONTH)
            allEvents.filter { it.first == dom }.forEach { b += it.second }
            b -= max(0.0, avgDailyVar)
            res.add(b)
        }
        return res
    }

    fun computeDebtPayoffSimulation(method: String, extra: Double): DebtPayoffResult {
        data class TempDebt(val title: String, var b: Double, val r: Double, val m: Double, var done: Boolean = false)
        val debts = _uiState.value.debts.map { TempDebt(it.personOrOrg.ifBlank { it.title }, it.bal, it.rate, it.min) }
        val minSum = debts.sumOf { it.m }
        val pool = minSum + extra
        var mo = 0
        var totalInterest = 0.0
        val order = mutableListOf<Pair<String, Int>>()

        while (debts.any { it.b > 0.005 } && mo < 600) {
            mo++
            debts.forEach { d ->
                if (d.b > 0) {
                    val interest = d.b * d.r / 1200.0
                    d.b += interest
                    totalInterest += interest
                }
            }
            var p = pool
            debts.filter { it.b > 0 }.forEach { d ->
                val a = min(d.m, d.b)
                d.b -= a
                p -= a
            }
            val target = debts.filter { it.b > 0 }.sortedWith(
                if (method == "snow") compareBy { it.b } else compareByDescending { it.r }
            ).firstOrNull()

            if (target != null) {
                val a = min(p, target.b)
                target.b -= a
            }
            debts.forEach { d ->
                if (d.b <= 0.005 && !d.done) {
                    d.done = true
                    order.add(d.title to mo)
                }
            }
        }
        return DebtPayoffResult(mo, totalInterest, order)
    }

    fun computeHealthScore(): HealthResult {
        val currentMonthTxs = getMonthTransactions(0)
        val inc = max(1.0, computeIncome(currentMonthTxs))
        val exp = computeExpenses(currentMonthTxs)
        val sr = max(0.0, (inc - exp) / inc)
        val avgE = max(1.0, computeAverageMonthlySpend(6))
        val em = (_uiState.value.goals.firstOrNull()?.saved ?: 0.0) / avgE
        val dti = _uiState.value.debts.sumOf { it.min } / inc
        val budgets = _uiState.value.budgets
        val budOnTrack = if (budgets.isNotEmpty()) {
            budgets.count { getSpentByCategory(currentMonthTxs, it.cat) <= it.limit }.toDouble() / budgets.size
        } else 1.0
        val nw = computeNetWorth()

        val factors = listOf(
            HealthFactor("Savings rate", min(1.0, sr / 0.20), 30, "${(sr * 100).roundToInt()}% saved · aim for 20%"),
            HealthFactor("Emergency fund", min(1.0, em / 6.0), 25, "${String.format(Locale.US, "%.1f", em)} of 6 months covered"),
            HealthFactor("Debt load", max(0.0, 1.0 - dti / 0.36), 20, "${(dti * 100).roundToInt()}% of income to debt"),
            HealthFactor("Budget discipline", budOnTrack, 15, "${(budOnTrack * 100).roundToInt()}% of budgets on track"),
            HealthFactor("Net worth", if (nw > 0) 1.0 else 0.0, 10, if (nw > 0) "Positive" else "Negative")
        )
        val totalScore = factors.sumOf { it.progress * it.weight }.roundToInt()
        return HealthResult(totalScore, factors)
    }

    fun computeNetWorthHistory(monthsCount: Int): Pair<List<Double>, List<String>> {
        val ms = computeMonthlySummaries(monthsCount)
        val hist = mutableListOf(computeNetWorth())
        for (i in (ms.size - 1) downTo 1) {
            val netFlow = ms[i].inc - ms[i].exp
            hist.add(0, hist[0] - netFlow)
        }
        val labels = ms.map { it.label }
        return hist to labels
    }

    fun computeInsights(): List<FinanceInsight> {
        val list = mutableListOf<FinanceInsight>()
        val cm = getMonthTransactions(0)
        val pm = getMonthTransactions(1)
        val I = computeIncome(cm)
        val E = computeExpenses(cm)
        val pI = computeIncome(pm)
        val pE = computeExpenses(pm)
        val sr = if (I > 0) (I - E) / I else 0.0
        val psr = if (pI > 0) (pI - pE) / pI else 0.0

        if (I > 0) {
            list.add(
                FinanceInsight(
                    icon = FinancesIcons.Spa,
                    colorHex = if (sr >= 0.2) "#2ECC71" else "#FFB020",
                    title = "You're saving ${(sr * 100).roundToInt()}% this month",
                    subtitle = "${if (sr >= psr) "Up" else "Down"} from ${(psr * 100).roundToInt()}% last month."
                )
            )
        }

        val budgets = _uiState.value.budgets
        if (budgets.isNotEmpty()) {
            val sortedByRatio = budgets.map { b -> b to (getSpentByCategory(cm, b.cat) / b.limit) }.sortedByDescending { it.second }
            val topBudget = sortedByRatio.first()
            val ratio = topBudget.second
            val catMeta = CATEGORIES[topBudget.first.cat]
            val daysRemaining = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) - Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + 1
            val spent = getSpentByCategory(cm, topBudget.first.cat)
            val left = max(0.0, topBudget.first.limit - spent)

            if (ratio >= 0.85) {
                list.add(
                    FinanceInsight(
                        icon = FinancesIcons.Warning,
                        colorHex = "#FF5C7A",
                        title = "${catMeta?.label} budget is ${(ratio * 100).roundToInt()}% used",
                        subtitle = "${formatCurrency(left)} left for $daysRemaining days."
                    )
                )
            } else {
                list.add(
                    FinanceInsight(
                        icon = FinancesIcons.Check,
                        colorHex = "#2ECC71",
                        title = "Budgets on track",
                        subtitle = "${catMeta?.label} is closest at ${(ratio * 100).roundToInt()}%."
                    )
                )
            }
        }

        val avgSpend = computeAverageMonthlySpend(6)
        val currentLiquid = computeLiquidMoney()
        if (avgSpend > 0 && currentLiquid > 0) {
            val runway = currentLiquid / avgSpend
            list.add(
                FinanceInsight(
                    icon = FinancesIcons.Runway,
                    colorHex = "#2ECC71",
                    title = "${String.format(Locale.US, "%.1f", runway)} months of runway in liquid funds",
                    subtitle = "Based on ${formatCurrency(avgSpend)}/mo average spend."
                )
            )
        }

        return list
    }
}

data class MonthlySummary(val label: String, val inc: Double, val exp: Double)

data class DebtPayoffResult(
    val months: Int,
    val totalInterest: Double,
    val payoffOrder: List<Pair<String, Int>>
)

data class HealthFactor(
    val label: String,
    val progress: Double,
    val weight: Int,
    val description: String
)

data class HealthResult(
    val score: Int,
    val factors: List<HealthFactor>
)

data class FinanceInsight(
    val icon: ImageVector,
    val colorHex: String,
    val title: String,
    val subtitle: String
)
