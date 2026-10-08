package tn.keeftalk.chat.feature.wallet.repository

import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.*

interface WalletRepository {
    fun getTransactions(): Flow<List<FinanceTransactionEntity>>
    suspend fun saveTransaction(tx: FinanceTransactionEntity)
    suspend fun deleteTransaction(tx: FinanceTransactionEntity)
    suspend fun deleteTransactionById(id: String)

    fun getBudgets(): Flow<List<FinanceBudgetEntity>>
    suspend fun saveBudget(budget: FinanceBudgetEntity)
    suspend fun deleteBudget(budget: FinanceBudgetEntity)

    fun getGoals(): Flow<List<FinanceGoalEntity>>
    suspend fun saveGoal(goal: FinanceGoalEntity)
    suspend fun deleteGoal(goal: FinanceGoalEntity)

    fun getAssets(): Flow<List<FinanceAssetEntity>>
    suspend fun saveAsset(asset: FinanceAssetEntity)
    suspend fun deleteAsset(asset: FinanceAssetEntity)

    fun getDebts(): Flow<List<FinanceDebtEntity>>
    suspend fun saveDebt(debt: FinanceDebtEntity)
    suspend fun deleteDebt(debt: FinanceDebtEntity)

    fun getRecurring(): Flow<List<FinanceRecurringEntity>>
    suspend fun saveRecurring(rec: FinanceRecurringEntity)
    suspend fun deleteRecurring(rec: FinanceRecurringEntity)

    fun getAccounts(): Flow<List<FinanceAccountEntity>>
    suspend fun saveAccount(account: FinanceAccountEntity)
    suspend fun deleteAccount(account: FinanceAccountEntity)

    fun getSharedPeople(): Flow<List<FinanceSharedPersonEntity>>
    suspend fun saveSharedPerson(person: FinanceSharedPersonEntity)

    fun getSharedLogs(): Flow<List<FinanceSharedLogEntity>>
    suspend fun saveSharedLog(log: FinanceSharedLogEntity)

    fun getChallenges(): Flow<List<FinanceChallengeEntity>>
    suspend fun saveChallenge(challenge: FinanceChallengeEntity)

    fun getDocs(): Flow<List<FinanceDocEntity>>
    suspend fun saveDoc(doc: FinanceDocEntity)

    fun getSettings(): Flow<FinanceSettingsEntity?>
    suspend fun saveSettings(settings: FinanceSettingsEntity)
}
