package tn.keeftalk.chat.feature.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.feature.wallet.repository.WalletRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class CategoryMeta(val emoji: String, val label: String, val colorHex: String)

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

data class FinancesUiState(
    val transactions: List<FinanceTransactionEntity> = emptyList(),
    val budgets: List<FinanceBudgetEntity> = emptyList(),
    val goals: List<FinanceGoalEntity> = emptyList(),
    val assets: List<FinanceAssetEntity> = emptyList(),
    val debts: List<FinanceDebtEntity> = emptyList(),
    val recurring: List<FinanceRecurringEntity> = emptyList(),
    val accounts: List<FinanceAccountEntity> = emptyList(),
    val sharedPeople: List<FinanceSharedPersonEntity> = emptyList(),
    val sharedLogs: List<FinanceSharedLogEntity> = emptyList(),
    val challenges: List<FinanceChallengeEntity> = emptyList(),
    val docs: List<FinanceDocEntity> = emptyList(),
    val settings: FinanceSettingsEntity = FinanceSettingsEntity(),

    // UI Navigation & View State
    val activeTab: String = "home", // "home", "spend", "plan", "wealth"
    val activeToolScreen: String? = null, // null, "t_wallets", "t_cal", "t_fc", "t_sim", "t_health", "t_debt", "t_subs", "t_shared", "t_chal", "t_nw", "t_road", "t_ai", "t_docs", "t_trend", "settings", "tools"
    val activeSheet: String? = null, // null, "expense", "income", "budget", "goal", "asset", "debt", "recurring", "account", "shared", "challenge", "doc"

    // Filters
    val period: String = "month", // "month" or "year"
    val searchQuery: String = "",
    val categoryFilter: String = "all",
    val typeFilter: String = "all",
    val docKindFilter: String = "all",
    val selectedCalendarDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),

    // Simulators & Sliders State
    val extraSavings: Double = 200.0,
    val salaryRaise: Double = 10.0,
    val oneOffPurchase: Double = 0.0,
    val expectedReturn: Double = 6.0,
    val extraDebtPayment: Double = 100.0,
    val forecastDays: Int = 30,
    val netWorthMonths: Int = 12,
    val netWorthScrubIndex: Int = 11,
    val debtMethod: String = "avl", // "avl" (Avalanche) or "snow" (Snowball)
    val savedScenarios: List<ScenarioComparison> = emptyList(),

    // AI Assistant
    val chatHistory: List<ChatMessage> = listOf(
        ChatMessage("ai", "Hi! Ask me where your money went, or whether you can afford something.")
    ),

    // Security & Locks
    val isVaultUnlocked: Boolean = false,
    val unlockTimeMillis: Long = 0L,

    // OCR Receipt Scan
    val ocrState: String? = null, // null, "busy", or result present
    val ocrResult: OcrResult? = null,

    // Widgets order & visibility
    val widgetsOrder: List<Pair<String, Boolean>> = listOf(
        "chart" to true,
        "insights" to true,
        "where" to true,
        "coming" to true,
        "summary" to true
    ),

    // Export/Import text output
    val exportImportText: String = "",
    val toastMessage: String? = null
)

class WalletViewModel(
    private val repository: WalletRepository
) : ViewModel() {

    companion object {
        val CATEGORIES = mapOf(
            "rent" to CategoryMeta("🏠", "Housing", "#7D5CFF"),
            "food" to CategoryMeta("🍜", "Food", "#00CCCC"),
            "transport" to CategoryMeta("🚌", "Transport", "#329FE0"),
            "fun" to CategoryMeta("🎬", "Fun", "#FF7AB6"),
            "health" to CategoryMeta("💊", "Health", "#2ECC71"),
            "shop" to CategoryMeta("🛍️", "Shopping", "#FFB020"),
            "bills" to CategoryMeta("💡", "Bills", "#6472F5"),
            "income" to CategoryMeta("💼", "Income", "#2ECC71")
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
                combine(repository.getRecurring(), repository.getAccounts()) { rec, acc -> rec to acc }
            ) { (t1, t2), (rec, acc) ->
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
                        accounts = acc
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

    // ─── ACTIONS & MUTATIONS ───

    fun selectTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab, activeToolScreen = null) }
    }

    fun openToolScreen(tool: String) {
        _uiState.update { it.copy(activeToolScreen = tool) }
    }

    fun closeToolScreen() {
        _uiState.update { it.copy(activeToolScreen = null) }
    }

    fun openSheet(type: String) {
        _uiState.update { it.copy(activeSheet = type) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = null) }
    }

    fun setPeriod(p: String) {
        _uiState.update { it.copy(period = p) }
    }

    fun setSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun setCategoryFilter(c: String) {
        _uiState.update { it.copy(categoryFilter = c) }
    }

    fun setTypeFilter(t: String) {
        _uiState.update { it.copy(typeFilter = t) }
    }

    fun setDocKindFilter(k: String) {
        _uiState.update { it.copy(docKindFilter = k) }
    }

    fun setSelectedCalendarDay(d: Int) {
        _uiState.update { it.copy(selectedCalendarDay = d) }
    }

    fun setExtraSavings(v: Double) { _uiState.update { it.copy(extraSavings = v) } }
    fun setSalaryRaise(v: Double) { _uiState.update { it.copy(salaryRaise = v) } }
    fun setOneOffPurchase(v: Double) { _uiState.update { it.copy(oneOffPurchase = v) } }
    fun setExpectedReturn(v: Double) { _uiState.update { it.copy(expectedReturn = v) } }
    fun setExtraDebtPayment(v: Double) { _uiState.update { it.copy(extraDebtPayment = v) } }
    fun setForecastDays(v: Int) { _uiState.update { it.copy(forecastDays = v) } }
    fun setNetWorthMonths(v: Int) { _uiState.update { it.copy(netWorthMonths = v, netWorthScrubIndex = v - 1) } }
    fun setNetWorthScrubIndex(v: Int) { _uiState.update { it.copy(netWorthScrubIndex = v) } }
    fun setDebtMethod(v: String) { _uiState.update { it.copy(debtMethod = v) } }

    fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun checkPin(enteredPin: String): Boolean {
        val currentPin = _uiState.value.settings.pin
        return if (enteredPin == currentPin) {
            _uiState.update {
                it.copy(
                    isVaultUnlocked = true,
                    unlockTimeMillis = System.currentTimeMillis() + 30000L
                )
            }
            true
        } else {
            showToast("Wrong PIN")
            false
        }
    }

    fun lockVault() {
        _uiState.update { it.copy(isVaultUnlocked = false) }
    }

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
            showToast("Currency changed to $newCur")
        }
    }

    fun toggleHideAmounts() {
        val updated = _uiState.value.settings.copy(hide = !_uiState.value.settings.hide)
        viewModelScope.launch {
            repository.saveSettings(updated)
        }
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
        viewModelScope.launch {
            repository.saveSettings(updated)
        }
    }

    fun updateReminderDays(days: Int) {
        val updated = _uiState.value.settings.copy(rem = days)
        viewModelScope.launch {
            repository.saveSettings(updated)
        }
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

    // ─── ENTITY MUTATIONS ───

    fun addExpense(title: String, amt: Double, cat: String, date: String, tags: String = "", note: String? = null) {
        viewModelScope.launch {
            val tagList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            repository.saveTransaction(
                FinanceTransactionEntity(
                    date = date,
                    title = title,
                    amt = amt,
                    cat = cat,
                    type = "exp",
                    tags = tagList.joinToString(","),
                    note = note
                )
            )
            closeSheet()
            showToast("Expense saved")
        }
    }

    fun addIncome(source: String, amt: Double, date: String) {
        viewModelScope.launch {
            repository.saveTransaction(
                FinanceTransactionEntity(
                    date = date,
                    title = source,
                    amt = amt,
                    cat = "income",
                    type = "inc"
                )
            )
            closeSheet()
            showToast("Income saved")
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
            showToast("Transaction deleted")
        }
    }

    fun addBudget(cat: String, limit: Double) {
        viewModelScope.launch {
            repository.saveBudget(FinanceBudgetEntity(cat = cat, limit = limit))
            closeSheet()
            showToast("Budget saved")
        }
    }

    fun addGoal(title: String, target: Double, saved: Double, monthly: Double) {
        viewModelScope.launch {
            repository.saveGoal(FinanceGoalEntity(title = title, emoji = "⭐", target = target, saved = saved, monthly = monthly))
            closeSheet()
            showToast("Goal saved")
        }
    }

    fun fundGoal(goalId: String, amount: Double = 100.0) {
        val goal = _uiState.value.goals.find { it.id == goalId } ?: return
        val newSaved = min(goal.target, goal.saved + amount)
        viewModelScope.launch {
            repository.saveGoal(goal.copy(saved = newSaved))
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.saveTransaction(
                FinanceTransactionEntity(
                    date = todayStr,
                    title = "Saved → ${goal.title}",
                    amt = amount,
                    cat = "bills",
                    type = "exp"
                )
            )
            val isComplete = newSaved >= goal.target
            showToast(if (isComplete) "Goal reached! 🎉" else "Added ${formatCurrency(amount)} to ${goal.title}")
        }
    }

    fun fundEmergencyFund(amount: Double = 100.0) {
        val emergencyGoal = _uiState.value.goals.firstOrNull() ?: return
        fundGoal(emergencyGoal.id, amount)
    }

    fun addAsset(title: String, kind: String, value: Double) {
        viewModelScope.launch {
            repository.saveAsset(FinanceAssetEntity(title = title, kind = kind, value = value, chg = 0.0))
            closeSheet()
            showToast("Asset saved")
        }
    }

    fun addDebt(title: String, bal: Double, rate: Double, min: Double) {
        viewModelScope.launch {
            repository.saveDebt(FinanceDebtEntity(title = title, bal = bal, rate = rate, min = min, orig = bal))
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

    fun addAccount(title: String, kind: String, cur: String, bal: Double, num: String?, rib: String?, bic: String?, exp: String?, cvv: String?, holder: String?) {
        viewModelScope.launch {
            repository.saveAccount(
                FinanceAccountEntity(
                    title = title, kind = kind, cur = cur, bal = bal,
                    num = num, rib = rib, bic = bic, exp = exp, cvv = cvv, holder = holder
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

    fun moveGoalPriority(index: Int) {
        val list = _uiState.value.goals.toMutableList()
        if (index in 1 until list.size) {
            val temp = list[index - 1]
            list[index - 1] = list[index]
            list[index] = temp
            viewModelScope.launch {
                list.forEach { repository.saveGoal(it) }
            }
        }
    }

    fun quickAdd(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val regex = Regex("(\\d+(?:[.,]\\d+)?)")
        val match = regex.find(trimmed)
        if (match == null) {
            showToast("Include an amount, e.g. 'coffee 4.5 food'")
            return
        }
        val amt = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
        val isIncome = trimmed.startsWith("+")
        val words = trimmed.replace(match.value, "").replace("+", "").trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val catKey = EXPENSE_CATEGORIES.find { cat ->
            words.any { w ->
                val meta = CATEGORIES[cat]
                meta?.label?.lowercase()?.startsWith(w.lowercase()) == true || w.lowercase() == cat
            }
        } ?: "shop"
        val titleText = words.filter { w ->
            val meta = CATEGORIES[catKey]
            meta?.label?.lowercase()?.startsWith(w.lowercase()) != true && w.lowercase() != catKey
        }.joinToString(" ")
        val finalTitle = if (titleText.isNotBlank()) titleText.replaceFirstChar { it.uppercase() } else if (isIncome) "Income" else CATEGORIES[catKey]?.label ?: "Expense"
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        viewModelScope.launch {
            repository.saveTransaction(
                FinanceTransactionEntity(
                    date = todayStr,
                    title = finalTitle,
                    amt = amt,
                    cat = if (isIncome) "income" else catKey,
                    type = if (isIncome) "inc" else "exp"
                )
            )
            closeSheet()
            showToast("${if (isIncome) "Income" else "Expense"} added: ${formatCurrency(amt)}")
        }
    }

    fun startOcrScan() {
        _uiState.update { it.copy(ocrState = "busy") }
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            val sample = OcrResult("Carrefour Market", 63.40, "food", listOf("Olive oil 1L", "Pasta x3", "Tomatoes 2kg", "Yogurt x6"))
            _uiState.update { it.copy(ocrState = "done", ocrResult = sample) }
        }
    }

    fun saveOcrReceipt(merchant: String, amount: Double, category: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val docId = UUID.randomUUID().toString()
        val itemsStr = _uiState.value.ocrResult?.items?.joinToString(", ") ?: ""
        viewModelScope.launch {
            repository.saveDoc(FinanceDocEntity(id = docId, title = "Receipt — $merchant", kind = "receipt", date = todayStr))
            repository.saveTransaction(
                FinanceTransactionEntity(
                    date = todayStr,
                    title = merchant,
                    amt = amount,
                    cat = category,
                    type = "exp",
                    tags = "receipt",
                    note = itemsStr,
                    rcpt = docId
                )
            )
            _uiState.update { it.copy(ocrState = null, ocrResult = null) }
            showToast("Expense saved with receipt attached")
        }
    }

    fun trackDetectedSubscription(title: String) {
        viewModelScope.launch {
            repository.saveRecurring(
                FinanceRecurringEntity(title = title, amt = 2.99, day = 9, kind = "subscription", on = true)
            )
            showToast("$title is now tracked")
        }
    }

    fun saveScenario() {
        val currentScenarios = _uiState.value.savedScenarios
        if (currentScenarios.size >= 3) {
            showToast("Clear scenarios to add more")
            return
        }
        val xSavings = _uiState.value.extraSavings
        val xRaise = _uiState.value.salaryRaise
        val xBuy = _uiState.value.oneOffPurchase
        val descParts = mutableListOf<String>()
        if (xSavings > 0) descParts.add("+${formatCurrency(xSavings)}/mo")
        if (xRaise > 0) descParts.add("+$xRaise% pay")
        if (xBuy > 0) descParts.add("buy ${formatCurrency(xBuy)}")

        val y1 = computeProjectedNetWorth(12, xSavings, xRaise, xBuy)
        val y5 = computeProjectedNetWorth(60, xSavings, xRaise, xBuy)
        val fy = computeFireYears(xSavings, xRaise, xBuy)

        val scenario = ScenarioComparison(
            name = "Scenario ${currentScenarios.size + 1}",
            description = descParts.joinToString(", ").ifEmpty { "No changes" },
            year1 = y1,
            year5 = y5,
            yearsToFi = fy
        )
        _uiState.update { it.copy(savedScenarios = currentScenarios + scenario) }
        showToast("Scenario saved")
    }

    fun clearScenarios() {
        _uiState.update { it.copy(savedScenarios = emptyList()) }
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
                val currentCash = computeCashBalance()
                val left = currentCash - num
                val mo = left / max(1.0, computeAverageMonthlySpend(6))
                val freeSurplus = computeSurplus()
                val monthsToSave = if (freeSurplus > 0) (num / freeSurplus).toInt() else 0
                val statusText = when {
                    left < 0 -> "Not from cash alone"
                    mo >= 3 -> "Yes, you can afford it"
                    else -> "You can, but it's tight"
                }
                return "$statusText: ${formatCurrency(num)} leaves ${formatCurrency(left)} in cash (${String.format(Locale.US, "%.1f", mo)} months of spending). At your free ${formatCurrency(max(0.0, freeSurplus))}/month, saving for it takes about $monthsToSave months."
            }
        }
        if (query.contains("where") || query.contains("most") || query.contains("went") || query.contains("spend") || query.contains("spent")) {
            val top3 = spentMap.take(3).joinToString(", ") { (c, v) ->
                val meta = CATEGORIES[c]
                "${meta?.emoji} ${meta?.label} ${formatCurrency(v)}"
            }
            return "This month you've spent ${formatCurrency(computeExpenses(currentMonthTxs))}. Top areas: $top3."
        }
        if (query.contains("sav")) {
            val income = computeIncome(currentMonthTxs)
            val exp = computeExpenses(currentMonthTxs)
            val saved = income - exp
            val pct = if (income > 0) ((saved / income) * 100).roundToInt() else 0
            return "You've saved ${formatCurrency(saved)} so far, $pct% of income."
        }
        if (query.contains("subscri")) {
            val annualSubs = _uiState.value.recurring.filter { it.on && it.kind == "subscription" }.sumOf { it.amt } * 12
            return "Subscriptions cost ${formatCurrency(annualSubs)} a year."
        }
        if (query.contains("debt") || query.contains("owe") || query.contains("loan")) {
            val topDebt = _uiState.value.debts.sortedByDescending { it.rate }.firstOrNull()
            return if (topDebt != null) {
                "You owe ${formatCurrency(computeDebtTotal())} in total. Pay ${topDebt.title} first, it charges ${topDebt.rate}% APR."
            } else {
                "You have no active debts! 🎉"
            }
        }
        if (query.contains("worth") || query.contains("wealth")) {
            return "Your net worth is ${formatCurrency(computeNetWorth())}: ${formatCurrency(computeCashBalance())} cash and ${formatCurrency(computeAssetsTotal())} assets, minus ${formatCurrency(computeDebtTotal())} debt."
        }
        if (query.contains("budget")) {
            val overBudgets = _uiState.value.budgets.filter { b -> getSpentByCategory(currentMonthTxs, b.cat) > b.limit * 0.85 }
            return if (overBudgets.isNotEmpty()) {
                val names = overBudgets.joinToString(" and ") { CATEGORIES[it.cat]?.label ?: it.cat }
                "Watch $names: above 85% used."
            } else {
                "All budgets are on track."
            }
        }
        return "Try: “Where did most of my money go?”, “Can I afford a $1,500 laptop?”, or “How much do subscriptions cost?”"
    }

    fun exportCsv() {
        val header = "date,title,amount,category,tags\n"
        val rows = _uiState.value.transactions.sortedBy { it.date }.joinToString("\n") { t ->
            "${t.date},\"${t.title}\",${if (t.type == "inc") "" else "-"}${t.amt},${t.cat},${t.tags}"
        }
        _uiState.update { it.copy(exportImportText = header + rows) }
        showToast("CSV generated")
    }

    fun exportReport() {
        val currentMonthTxs = getMonthTransactions(0)
        val inc = computeIncome(currentMonthTxs)
        val exp = computeExpenses(currentMonthTxs)
        val cal = Calendar.getInstance()
        val monthYearName = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
        val topCategories = EXPENSE_CATEGORIES
            .map { c -> c to getSpentByCategory(currentMonthTxs, c) }
            .sortedByDescending { it.second }
            .take(5)
            .joinToString("\n") { (c, v) -> "- ${CATEGORIES[c]?.label}: ${formatCurrency(v)}" }

        val report = """
            Monthly report — $monthYearName
            Income: ${formatCurrency(inc)}
            Expenses: ${formatCurrency(exp)}
            Saved: ${formatCurrency(inc - exp)}
            Net worth: ${formatCurrency(computeNetWorth())}

            Top categories
            $topCategories
        """.trimIndent()

        _uiState.update { it.copy(exportImportText = report) }
        showToast("Monthly report generated")
    }

    fun importPastedData(text: String) {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        var importedCount = 0
        viewModelScope.launch {
            for (line in lines.drop(if (lines.firstOrNull()?.contains("date") == true) 1 else 0)) {
                val cols = line.split(Regex("[,;]")).map { it.trim().removeSurrounding("\"") }
                if (cols.size >= 3) {
                    val dateStr = if (cols[0].matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) cols[0] else todayStr
                    val titleStr = cols[1]
                    val amtVal = cols[2].toDoubleOrNull()
                    if (amtVal != null) {
                        val isExp = amtVal < 0 || (cols.getOrNull(3)?.lowercase() != "income")
                        val catStr = if (isExp) (cols.getOrNull(3)?.lowercase()?.takeIf { EXPENSE_CATEGORIES.contains(it) } ?: "shop") else "income"
                        repository.saveTransaction(
                            FinanceTransactionEntity(
                                date = dateStr,
                                title = titleStr,
                                amt = Math.abs(amtVal),
                                cat = catStr,
                                type = if (isExp) "exp" else "inc",
                                tags = "imported"
                            )
                        )
                        importedCount++
                    }
                }
            }
            showToast(if (importedCount > 0) "Imported $importedCount transactions" else "Nothing recognized. Use: date,title,amount,category")
        }
    }

    // ─── CALCULATIONS & HELPER MATH ───

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

    fun getMonthTransactions(monthOffset: Int): List<FinanceTransactionEntity> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthOffset)
        val ym = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
        return _uiState.value.transactions.filter { it.date.startsWith(ym) }
    }

    fun computeIncome(txs: List<FinanceTransactionEntity>): Double = txs.filter { it.type == "inc" }.sumOf { it.amt }
    fun computeExpenses(txs: List<FinanceTransactionEntity>): Double = txs.filter { it.type == "exp" }.sumOf { it.amt }
    fun getSpentByCategory(txs: List<FinanceTransactionEntity>, cat: String): Double = txs.filter { it.type == "exp" && it.cat == cat }.sumOf { it.amt }

    fun computeCashBalance(): Double {
        val accounts = _uiState.value.accounts
        return if (accounts.isNotEmpty()) {
            accounts.sumOf { acc ->
                val rate = EXCHANGE_RATES[acc.cur] ?: 1.0
                val netVal = acc.bal
                netVal / rate
            }
        } else {
            val netTx = _uiState.value.transactions.sumOf { if (it.type == "inc") it.amt else -it.amt }
            2600.0 + netTx
        }
    }

    fun computeAssetsTotal(): Double = _uiState.value.assets.sumOf { it.value }
    fun computeDebtTotal(): Double = _uiState.value.debts.sumOf { it.bal }
    fun computeNetWorth(): Double = computeCashBalance() + computeAssetsTotal() - computeDebtTotal()

    fun computeInvested(): Double = computeAssetsTotal() - _uiState.value.assets.filter { it.kind == "cash" }.sumOf { it.value }

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
        return if (ms.isNotEmpty()) sum / ms.size else 1.0
    }

    fun computeSurplus(): Double {
        val incRec = _uiState.value.recurring.filter { it.on && it.kind == "income" }.sumOf { it.amt }
        val expRec = _uiState.value.recurring.filter { it.on && it.kind != "income" }.sumOf { it.amt }
        val avg3Mo = (0..2).map { getMonthTransactions(it) }.map { computeExpenses(it) }.average()
        val varSpend = max(0.0, avg3Mo - expRec)
        return incRec - expRec - varSpend
    }

    fun computeProjectedNetWorth(months: Int, extra: Double = 0.0, raise: Double = 0.0, buy: Double = 0.0): Double {
        val r = _uiState.value.expectedReturn / 1200.0
        val g = (1.0 + r).pow(months.toDouble()) - 1.0
        val inv0 = computeInvested()
        val incRec = _uiState.value.recurring.filter { it.on && it.kind == "income" }.sumOf { it.amt }
        val surplusVal = computeSurplus()
        val monthContribution = surplusVal + (incRec * raise / 100.0)
        return computeNetWorth() + (monthContribution * months) + (inv0 * g) + (if (r > 0) extra * g / r else extra * months) - buy
    }

    fun computeFireYears(extra: Double = 0.0, raise: Double = 0.0, buy: Double = 0.0): Double? {
        val r = _uiState.value.expectedReturn / 1200.0
        val target = 25.0 * 12.0 * computeAverageMonthlySpend(6)
        val incRec = _uiState.value.recurring.filter { it.on && it.kind == "income" }.sumOf { it.amt }
        val s = max(0.0, computeSurplus() + (incRec * raise / 100.0) + extra)
        var v = computeInvested() + computeCashBalance() - buy
        var m = 0
        while (v < target && m < 720) {
            v = v * (1.0 + r) + s
            m++
        }
        return if (m >= 720) null else m / 12.0
    }

    fun computeForecast(days: Int): List<Double> {
        var b = computeCashBalance()
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
        val debts = _uiState.value.debts.map { TempDebt(it.title, it.bal, it.rate, it.min) }
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
            HealthFactor("Debt load", max(0.0, 1.0 - dti / 0.36), 20, "${(dti * 100).roundToInt()}% of income to debt · keep under 36%"),
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
                    emoji = if (sr >= 0.2) "🌱" else "🪴",
                    colorHex = if (sr >= 0.2) "#2ECC71" else "#FFB020",
                    title = "You're saving ${(sr * 100).roundToInt()}% this month",
                    subtitle = "${if (sr >= psr) "Up" else "Down"} from ${(psr * 100).roundToInt()}% last month. ${if (sr >= 0.2) "You're past the 20% rule of thumb." else "Moving ${formatCurrency(I * 0.2 - (I - E))} more into savings would reach 20%."}"
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
                        emoji = "🚨",
                        colorHex = "#FF5C7A",
                        title = "${catMeta?.label} budget is ${(ratio * 100).roundToInt()}% used",
                        subtitle = "${formatCurrency(left)} left for $daysRemaining days — about ${formatCurrency(left / max(1, daysRemaining))}/day."
                    )
                )
            } else {
                list.add(
                    FinanceInsight(
                        emoji = "✅",
                        colorHex = "#2ECC71",
                        title = "Every budget is on track",
                        subtitle = "${catMeta?.label} is your closest at ${(ratio * 100).roundToInt()}%. At this pace you'll finish in budget."
                    )
                )
            }
        }

        val unusedRec = _uiState.value.recurring.filter { it.unused && it.on }
        if (unusedRec.isNotEmpty()) {
            val u = unusedRec.first()
            list.add(
                FinanceInsight(
                    emoji = "✂️",
                    colorHex = "#7D5CFF",
                    title = "Cancel ${u.title}, keep ${formatCurrency(u.amt * 12)}/yr",
                    subtitle = "You haven't used it in 60 days. Pause it from Plan → Recurring."
                )
            )
        }

        val topDebt = _uiState.value.debts.sortedByDescending { it.rate }.firstOrNull()
        if (topDebt != null) {
            list.add(
                FinanceInsight(
                    emoji = "🔥",
                    colorHex = "#FF5C7A",
                    title = "Hit ${topDebt.title} first (${topDebt.rate}% APR)",
                    subtitle = "+${formatCurrency(100.0)}/mo clears it sooner and saves interest."
                )
            )
        }

        val activeGoal = _uiState.value.goals.filter { it.saved < it.target }.minByOrNull { (it.target - it.saved) / max(1.0, it.monthly) }
        if (activeGoal != null && activeGoal.monthly > 0) {
            val mo = Math.ceil((activeGoal.target - activeGoal.saved) / activeGoal.monthly).toInt()
            val targetCal = Calendar.getInstance()
            targetCal.add(Calendar.MONTH, mo)
            val monthYear = SimpleDateFormat("MMMM yyyy", Locale.US).format(targetCal.time)
            list.add(
                FinanceInsight(
                    emoji = activeGoal.emoji,
                    colorHex = "#00CCCC",
                    title = "${activeGoal.title}: $mo month${if (mo > 1) "s" else ""} to go",
                    subtitle = "At ${formatCurrency(activeGoal.monthly)}/mo you'll get there by $monthYear."
                )
            )
        }

        val avgSpend = computeAverageMonthlySpend(6)
        val runway = computeCashBalance() / max(1.0, avgSpend)
        list.add(
            FinanceInsight(
                emoji = "🛫",
                colorHex = "#2ECC71",
                title = "${String.format(Locale.US, "%.1f", runway)} months of runway in cash",
                subtitle = "Based on ${formatCurrency(avgSpend)}/mo average spend."
            )
        )

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
    val progress: Double, // 0.0 to 1.0
    val weight: Int,
    val description: String
)

data class HealthResult(
    val score: Int,
    val factors: List<HealthFactor>
)

data class FinanceInsight(
    val emoji: String,
    val colorHex: String,
    val title: String,
    val subtitle: String
)
