package com.keeftalk.chat.feature.wallet.repository

import com.keeftalk.chat.feature.wallet.model.*
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    fun getTransactions(): Flow<List<Transaction>>
    fun getCategories(): Flow<List<WalletCategory>>
    fun getSavingsGoal(): Flow<SavingsGoal>
    fun getRecurringTransactions(): Flow<List<RecurringTransaction>>
    fun getPaymentMethods(): Flow<List<PaymentMethod>>
    suspend fun addTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transactionId: String)
    suspend fun updateSavingsGoal(goal: SavingsGoal)
    suspend fun addRecurringTransaction(recurring: RecurringTransaction)
    suspend fun deleteRecurringTransaction(id: String)
    suspend fun addPaymentMethod(method: PaymentMethod)
}
