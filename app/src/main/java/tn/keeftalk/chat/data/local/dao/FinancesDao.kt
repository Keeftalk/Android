package tn.keeftalk.chat.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.*

@Dao
interface FinancesDao {
    @Query("SELECT * FROM finance_transactions ORDER BY date DESC")
    fun getTransactions(): Flow<List<FinanceTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: FinanceTransactionEntity)

    @Delete
    suspend fun deleteTransaction(tx: FinanceTransactionEntity)

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("SELECT * FROM finance_budgets")
    fun getBudgets(): Flow<List<FinanceBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: FinanceBudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: FinanceBudgetEntity)

    @Query("SELECT * FROM finance_goals")
    fun getGoals(): Flow<List<FinanceGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FinanceGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: FinanceGoalEntity)

    @Query("SELECT * FROM finance_assets")
    fun getAssets(): Flow<List<FinanceAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: FinanceAssetEntity)

    @Delete
    suspend fun deleteAsset(asset: FinanceAssetEntity)

    @Query("SELECT * FROM finance_debts")
    fun getDebts(): Flow<List<FinanceDebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: FinanceDebtEntity)

    @Delete
    suspend fun deleteDebt(debt: FinanceDebtEntity)

    @Query("SELECT * FROM finance_recurring")
    fun getRecurring(): Flow<List<FinanceRecurringEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(rec: FinanceRecurringEntity)

    @Delete
    suspend fun deleteRecurring(rec: FinanceRecurringEntity)

    @Query("SELECT * FROM finance_accounts")
    fun getAccounts(): Flow<List<FinanceAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(acc: FinanceAccountEntity)

    @Delete
    suspend fun deleteAccount(acc: FinanceAccountEntity)

    @Query("SELECT * FROM finance_salaries")
    fun getSalaries(): Flow<List<FinanceSalaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalary(salary: FinanceSalaryEntity)

    @Delete
    suspend fun deleteSalary(salary: FinanceSalaryEntity)

    @Query("SELECT * FROM finance_work_logs ORDER BY date DESC")
    fun getWorkLogs(): Flow<List<FinanceWorkLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkLog(workLog: FinanceWorkLogEntity)

    @Delete
    suspend fun deleteWorkLog(workLog: FinanceWorkLogEntity)

    @Query("SELECT * FROM finance_shared_people")
    fun getSharedPeople(): Flow<List<FinanceSharedPersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedPerson(person: FinanceSharedPersonEntity)

    @Query("SELECT * FROM finance_shared_logs ORDER BY date DESC")
    fun getSharedLogs(): Flow<List<FinanceSharedLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedLog(log: FinanceSharedLogEntity)

    @Query("SELECT * FROM finance_challenges")
    fun getChallenges(): Flow<List<FinanceChallengeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: FinanceChallengeEntity)

    @Query("SELECT * FROM finance_docs ORDER BY date DESC")
    fun getDocs(): Flow<List<FinanceDocEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoc(doc: FinanceDocEntity)

    @Query("SELECT * FROM finance_settings WHERE id = 1")
    fun getSettings(): Flow<FinanceSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: FinanceSettingsEntity)
}
