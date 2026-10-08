package tn.keeftalk.chat.feature.finances.repository

import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.dao.FinancesDao
import tn.keeftalk.chat.data.local.entities.*

class FinancesRepositoryImpl(
    private val dao: FinancesDao
) : FinancesRepository {

    override fun getTransactions(): Flow<List<FinanceTransactionEntity>> = dao.getTransactions()
    override suspend fun saveTransaction(tx: FinanceTransactionEntity) = dao.insertTransaction(tx)
    override suspend fun deleteTransaction(tx: FinanceTransactionEntity) = dao.deleteTransaction(tx)
    override suspend fun deleteTransactionById(id: String) = dao.deleteTransactionById(id)

    override fun getBudgets(): Flow<List<FinanceBudgetEntity>> = dao.getBudgets()
    override suspend fun saveBudget(budget: FinanceBudgetEntity) = dao.insertBudget(budget)
    override suspend fun deleteBudget(budget: FinanceBudgetEntity) = dao.deleteBudget(budget)

    override fun getGoals(): Flow<List<FinanceGoalEntity>> = dao.getGoals()
    override suspend fun saveGoal(goal: FinanceGoalEntity) = dao.insertGoal(goal)
    override suspend fun deleteGoal(goal: FinanceGoalEntity) = dao.deleteGoal(goal)

    override fun getAssets(): Flow<List<FinanceAssetEntity>> = dao.getAssets()
    override suspend fun saveAsset(asset: FinanceAssetEntity) = dao.insertAsset(asset)
    override suspend fun deleteAsset(asset: FinanceAssetEntity) = dao.deleteAsset(asset)

    override fun getDebts(): Flow<List<FinanceDebtEntity>> = dao.getDebts()
    override suspend fun saveDebt(debt: FinanceDebtEntity) = dao.insertDebt(debt)
    override suspend fun deleteDebt(debt: FinanceDebtEntity) = dao.deleteDebt(debt)

    override fun getRecurring(): Flow<List<FinanceRecurringEntity>> = dao.getRecurring()
    override suspend fun saveRecurring(rec: FinanceRecurringEntity) = dao.insertRecurring(rec)
    override suspend fun deleteRecurring(rec: FinanceRecurringEntity) = dao.deleteRecurring(rec)

    override fun getAccounts(): Flow<List<FinanceAccountEntity>> = dao.getAccounts()
    override suspend fun saveAccount(account: FinanceAccountEntity) = dao.insertAccount(account)
    override suspend fun deleteAccount(account: FinanceAccountEntity) = dao.deleteAccount(account)

    override fun getSalaries(): Flow<List<FinanceSalaryEntity>> = dao.getSalaries()
    override suspend fun saveSalary(salary: FinanceSalaryEntity) = dao.insertSalary(salary)
    override suspend fun deleteSalary(salary: FinanceSalaryEntity) = dao.deleteSalary(salary)

    override fun getWorkLogs(): Flow<List<FinanceWorkLogEntity>> = dao.getWorkLogs()
    override suspend fun saveWorkLog(workLog: FinanceWorkLogEntity) = dao.insertWorkLog(workLog)
    override suspend fun deleteWorkLog(workLog: FinanceWorkLogEntity) = dao.deleteWorkLog(workLog)

    override fun getSharedPeople(): Flow<List<FinanceSharedPersonEntity>> = dao.getSharedPeople()
    override suspend fun saveSharedPerson(person: FinanceSharedPersonEntity) = dao.insertSharedPerson(person)

    override fun getSharedLogs(): Flow<List<FinanceSharedLogEntity>> = dao.getSharedLogs()
    override suspend fun saveSharedLog(log: FinanceSharedLogEntity) = dao.insertSharedLog(log)

    override fun getChallenges(): Flow<List<FinanceChallengeEntity>> = dao.getChallenges()
    override suspend fun saveChallenge(challenge: FinanceChallengeEntity) = dao.insertChallenge(challenge)

    override fun getDocs(): Flow<List<FinanceDocEntity>> = dao.getDocs()
    override suspend fun saveDoc(doc: FinanceDocEntity) = dao.insertDoc(doc)

    override fun getSettings(): Flow<FinanceSettingsEntity?> = dao.getSettings()
    override suspend fun saveSettings(settings: FinanceSettingsEntity) = dao.insertSettings(settings)
}
