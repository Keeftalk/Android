package tn.keeftalk.chat.feature.finances.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.feature.finances.repository.FinancesRepository

class FakeFinancesRepository : FinancesRepository {
    private val _txs = MutableStateFlow<List<FinanceTransactionEntity>>(emptyList())
    private val _budgets = MutableStateFlow<List<FinanceBudgetEntity>>(emptyList())
    private val _goals = MutableStateFlow<List<FinanceGoalEntity>>(emptyList())
    private val _assets = MutableStateFlow<List<FinanceAssetEntity>>(emptyList())
    private val _debts = MutableStateFlow<List<FinanceDebtEntity>>(emptyList())
    private val _rec = MutableStateFlow<List<FinanceRecurringEntity>>(emptyList())
    private val _acc = MutableStateFlow<List<FinanceAccountEntity>>(emptyList())
    private val _salaries = MutableStateFlow<List<FinanceSalaryEntity>>(emptyList())
    private val _workLogs = MutableStateFlow<List<FinanceWorkLogEntity>>(emptyList())
    private val _sharedPeople = MutableStateFlow<List<FinanceSharedPersonEntity>>(emptyList())
    private val _sharedLogs = MutableStateFlow<List<FinanceSharedLogEntity>>(emptyList())
    private val _challenges = MutableStateFlow<List<FinanceChallengeEntity>>(emptyList())
    private val _docs = MutableStateFlow<List<FinanceDocEntity>>(emptyList())
    private val _settings = MutableStateFlow<FinanceSettingsEntity?>(FinanceSettingsEntity())

    override fun getTransactions(): Flow<List<FinanceTransactionEntity>> = _txs
    override suspend fun saveTransaction(tx: FinanceTransactionEntity) {
        _txs.value = _txs.value.filter { it.id != tx.id } + tx
    }
    override suspend fun deleteTransaction(tx: FinanceTransactionEntity) { _txs.value = _txs.value.filter { it.id != tx.id } }
    override suspend fun deleteTransactionById(id: String) { _txs.value = _txs.value.filter { it.id != id } }

    override fun getBudgets(): Flow<List<FinanceBudgetEntity>> = _budgets
    override suspend fun saveBudget(budget: FinanceBudgetEntity) { _budgets.value = _budgets.value + budget }
    override suspend fun deleteBudget(budget: FinanceBudgetEntity) { _budgets.value = _budgets.value.filter { it.id != budget.id } }

    override fun getGoals(): Flow<List<FinanceGoalEntity>> = _goals
    override suspend fun saveGoal(goal: FinanceGoalEntity) {
        _goals.value = _goals.value.filter { it.id != goal.id } + goal
    }
    override suspend fun deleteGoal(goal: FinanceGoalEntity) { _goals.value = _goals.value.filter { it.id != goal.id } }

    override fun getAssets(): Flow<List<FinanceAssetEntity>> = _assets
    override suspend fun saveAsset(asset: FinanceAssetEntity) { _assets.value = _assets.value + asset }
    override suspend fun deleteAsset(asset: FinanceAssetEntity) { _assets.value = _assets.value.filter { it.id != asset.id } }

    override fun getDebts(): Flow<List<FinanceDebtEntity>> = _debts
    override suspend fun saveDebt(debt: FinanceDebtEntity) {
        _debts.value = _debts.value.filter { it.id != debt.id } + debt
    }
    override suspend fun deleteDebt(debt: FinanceDebtEntity) { _debts.value = _debts.value.filter { it.id != debt.id } }

    override fun getRecurring(): Flow<List<FinanceRecurringEntity>> = _rec
    override suspend fun saveRecurring(rec: FinanceRecurringEntity) { _rec.value = _rec.value + rec }
    override suspend fun deleteRecurring(rec: FinanceRecurringEntity) { _rec.value = _rec.value.filter { it.id != rec.id } }

    override fun getAccounts(): Flow<List<FinanceAccountEntity>> = _acc
    override suspend fun saveAccount(account: FinanceAccountEntity) {
        _acc.value = _acc.value.filter { it.id != account.id } + account
    }
    override suspend fun deleteAccount(account: FinanceAccountEntity) { _acc.value = _acc.value.filter { it.id != account.id } }

    override fun getSalaries(): Flow<List<FinanceSalaryEntity>> = _salaries
    override suspend fun saveSalary(salary: FinanceSalaryEntity) {
        _salaries.value = _salaries.value.filter { it.id != salary.id } + salary
    }
    override suspend fun deleteSalary(salary: FinanceSalaryEntity) {
        _salaries.value = _salaries.value.filter { it.id != salary.id }
    }

    override fun getWorkLogs(): Flow<List<FinanceWorkLogEntity>> = _workLogs
    override suspend fun saveWorkLog(workLog: FinanceWorkLogEntity) {
        _workLogs.value = _workLogs.value.filter { it.date != workLog.date } + workLog
    }
    override suspend fun deleteWorkLog(workLog: FinanceWorkLogEntity) {
        _workLogs.value = _workLogs.value.filter { it.date != workLog.date }
    }

    override fun getSharedPeople(): Flow<List<FinanceSharedPersonEntity>> = _sharedPeople
    override suspend fun saveSharedPerson(person: FinanceSharedPersonEntity) {
        _sharedPeople.value = _sharedPeople.value.filter { it.id != person.id } + person
    }

    override fun getSharedLogs(): Flow<List<FinanceSharedLogEntity>> = _sharedLogs
    override suspend fun saveSharedLog(log: FinanceSharedLogEntity) { _sharedLogs.value = _sharedLogs.value + log }

    override fun getChallenges(): Flow<List<FinanceChallengeEntity>> = _challenges
    override suspend fun saveChallenge(challenge: FinanceChallengeEntity) {
        _challenges.value = _challenges.value.filter { it.id != challenge.id } + challenge
    }

    override fun getDocs(): Flow<List<FinanceDocEntity>> = _docs
    override suspend fun saveDoc(doc: FinanceDocEntity) { _docs.value = _docs.value + doc }

    override fun getSettings(): Flow<FinanceSettingsEntity?> = _settings
    override suspend fun saveSettings(settings: FinanceSettingsEntity) { _settings.value = settings }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FinancesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeFinancesRepository
    private lateinit var viewModel: FinancesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeFinancesRepository()
        viewModel = FinancesViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testQuickAddExpense() = runTest(testDispatcher) {
        viewModel.quickAdd("coffee 4.5 food")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.transactions.size)
        val tx = state.transactions.first()
        assertEquals(4.5, tx.amt, 0.001)
        assertEquals("exp", tx.type)
        assertEquals("food", tx.cat)
    }

    @Test
    fun testQuickAddIncome() = runTest(testDispatcher) {
        viewModel.quickAdd("+1200 freelance")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.transactions.size)
        val tx = state.transactions.first()
        assertEquals(1200.0, tx.amt, 0.001)
        assertEquals("inc", tx.type)
        assertEquals("income", tx.cat)
    }

    @Test
    fun testNetWorthCalculation() = runTest(testDispatcher) {
        repository.saveAsset(FinanceAssetEntity(title = "Stock ETF", kind = "stock", value = 10000.0))
        repository.saveDebt(FinanceDebtEntity(title = "Card", bal = 2000.0, rate = 20.0, min = 100.0, orig = 2000.0))
        testScheduler.advanceUntilIdle()

        val netWorth = viewModel.computeNetWorth()
        assertEquals(8000.0, netWorth, 0.001)
    }

    @Test
    fun testAccountBalanceUpdateOnExpense() = runTest(testDispatcher) {
        val bankAcc = FinanceAccountEntity(id = "acc_bank", title = "Bank A", kind = "bank", bal = 1000.0, cur = "TND")
        repository.saveAccount(bankAcc)
        testScheduler.advanceUntilIdle()

        viewModel.addExpense(title = "Groceries", amt = 80.0, cat = "food", date = "2025-05-10", sourceAccountId = "acc_bank")
        testScheduler.advanceUntilIdle()

        val updatedAcc = viewModel.uiState.value.accounts.find { it.id == "acc_bank" }
        assertNotNull(updatedAcc)
        assertEquals(920.0, updatedAcc!!.bal, 0.001)
    }

    @Test
    fun testAccountTransfer() = runTest(testDispatcher) {
        val bankA = FinanceAccountEntity(id = "acc_a", title = "Bank A", kind = "bank", bal = 1000.0, cur = "TND")
        val bankB = FinanceAccountEntity(id = "acc_b", title = "Bank B", kind = "bank", bal = 200.0, cur = "TND")
        repository.saveAccount(bankA)
        repository.saveAccount(bankB)
        testScheduler.advanceUntilIdle()

        viewModel.addTransfer(title = "Transfer", amt = 200.0, date = "2025-05-10", sourceAccountId = "acc_a", destinationAccountId = "acc_b")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(800.0, state.accounts.find { it.id == "acc_a" }!!.bal, 0.001)
        assertEquals(400.0, state.accounts.find { it.id == "acc_b" }!!.bal, 0.001)

        // Verify transfer is not counted as income or expense
        val monthTxs = viewModel.getMonthTransactions(0)
        assertEquals(0.0, viewModel.computeIncome(monthTxs), 0.001)
        assertEquals(0.0, viewModel.computeExpenses(monthTxs), 0.001)
    }

    @Test
    fun testDebtPaymentRecord() = runTest(testDispatcher) {
        val cashAcc = FinanceAccountEntity(id = "acc_cash", title = "Cash", kind = "cash", bal = 500.0, cur = "TND")
        val debt = FinanceDebtEntity(id = "debt_ahmed", title = "Ahmed", personOrOrg = "Ahmed", bal = 150.0, orig = 150.0, direction = "i_owe")
        repository.saveAccount(cashAcc)
        repository.saveDebt(debt)
        testScheduler.advanceUntilIdle()

        viewModel.recordDebtPayment(debtId = "debt_ahmed", amt = 50.0, date = "2025-05-10", sourceAccountId = "acc_cash")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(450.0, state.accounts.find { it.id == "acc_cash" }!!.bal, 0.001)
        assertEquals(100.0, state.debts.find { it.id == "debt_ahmed" }!!.bal, 0.001)
    }

    @Test
    fun testReconcileAccount() = runTest(testDispatcher) {
        val bankAcc = FinanceAccountEntity(id = "acc_bank", title = "Bank A", kind = "bank", bal = 1245.0, cur = "TND")
        repository.saveAccount(bankAcc)
        testScheduler.advanceUntilIdle()

        viewModel.reconcileAccount(accountId = "acc_bank", actualBalance = 1250.0)
        testScheduler.advanceUntilIdle()

        val updatedAcc = viewModel.uiState.value.accounts.find { it.id == "acc_bank" }
        assertEquals(1250.0, updatedAcc!!.bal, 0.001)

        val txs = viewModel.uiState.value.transactions
        assertTrue(txs.any { it.type == "adjustment" && it.amt == 5.0 })
    }

    @Test
    fun testWorkLogAndStats() = runTest(testDispatcher) {
        viewModel.setWorkLog(date = "2025-05-01", status = "working", workHours = 8.0)
        viewModel.setWorkLog(date = "2025-05-02", status = "absent", workHours = 0.0, isPaid = false)
        testScheduler.advanceUntilIdle()

        val stats = viewModel.computeWorkStats("2025-05")
        assertEquals(1, stats.workingDaysCount)
        assertEquals(1, stats.absentDaysCount)
        assertEquals(1, stats.unpaidLeaveCount)
    }

    @Test
    fun testAiAssistantAnswer() = runTest(testDispatcher) {
        viewModel.askAi("What's my net worth?")
        testScheduler.advanceUntilIdle()

        val chat = viewModel.uiState.value.chatHistory
        assertTrue(chat.any { it.sender == "ai" && it.text.contains("net worth") })
    }
}
