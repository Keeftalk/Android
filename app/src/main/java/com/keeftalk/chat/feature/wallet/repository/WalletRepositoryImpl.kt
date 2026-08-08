package com.keeftalk.chat.feature.wallet.repository

import com.keeftalk.chat.feature.wallet.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.Date

class WalletRepositoryImpl : WalletRepository {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _categories = MutableStateFlow(
        listOf(
            WalletCategory("food", "Food", "utensils", "#f2994a", 500.0),
            WalletCategory("transport", "Transport", "car", "#5b6bf7", 200.0),
            WalletCategory("shopping", "Shopping", "shopping-bag", "#9b59b6", 400.0),
            WalletCategory("bills", "Bills", "file-invoice", "#eb5757", 600.0),
            WalletCategory("entertainment", "Entertainment", "film", "#34c3e0", 150.0),
            WalletCategory("health", "Health", "heartbeat", "#27ae60", 100.0),
            WalletCategory("education", "Education", "graduation-cap", "#f39c12", 80.0),
            WalletCategory("transfer", "Transfer", "exchange-alt", "#5b6bf7", 0.0),
            WalletCategory("deposit", "Deposit", "piggy-bank", "#6fcf97", 0.0),
            WalletCategory("other", "Other", "tag", "#7e8a98", 120.0)
        )
    )
    private val _savingsGoal = MutableStateFlow(SavingsGoal(1000.0, 0.0))
    private val _recurring = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    private val _paymentMethods = MutableStateFlow(
        listOf(
            PaymentMethod("pm1", "Credit Card (•••• 1234)", "💳"),
            PaymentMethod("pm2", "PayPal", "📱"),
            PaymentMethod("pm3", "Bank Transfer", "🏦")
        )
    )

    init {
        // Seed initial data
        val now = Date().time
        _transactions.value = listOf(
            Transaction(amount = 1500.0, date = now, merchant = "Salary Deposit", categoryId = "deposit", type = TransactionType.WALLET_RECEIVE, note = "Monthly salary"),
            Transaction(amount = 200.0, date = now - 86400000, merchant = "Transfer to Alex", categoryId = "transfer", type = TransactionType.WALLET_SEND, note = "Dinner split"),
            Transaction(amount = 45.5, date = now, merchant = "Starbucks", categoryId = "food", type = TransactionType.EXPENSE, note = "Morning coffee"),
            Transaction(amount = 1200.0, date = now - 86400000, merchant = "Freelance Project", categoryId = "other", type = TransactionType.INCOME, note = "Web design")
        )
        
        _recurring.value = listOf(
            RecurringTransaction(merchant = "Netflix", amount = 89.9, categoryId = "entertainment", frequency = "Monthly", nextDue = now + 15 * 86400000L),
            RecurringTransaction(merchant = "Gym", amount = 75.0, categoryId = "health", frequency = "Monthly", nextDue = now + 20 * 86400000L)
        )
    }

    override fun getTransactions(): Flow<List<Transaction>> = _transactions.asStateFlow()
    override fun getCategories(): Flow<List<WalletCategory>> = _categories.asStateFlow()
    override fun getSavingsGoal(): Flow<SavingsGoal> = _savingsGoal.asStateFlow()
    override fun getRecurringTransactions(): Flow<List<RecurringTransaction>> = _recurring.asStateFlow()
    override fun getPaymentMethods(): Flow<List<PaymentMethod>> = _paymentMethods.asStateFlow()

    override suspend fun addTransaction(transaction: Transaction) {
        _transactions.value = _transactions.value + transaction
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        _transactions.value = _transactions.value.map { if (it.id == transaction.id) transaction else it }
    }

    override suspend fun deleteTransaction(transactionId: String) {
        _transactions.value = _transactions.value.filter { it.id != transactionId }
    }

    override suspend fun updateSavingsGoal(goal: SavingsGoal) {
        _savingsGoal.value = goal
    }

    override suspend fun addRecurringTransaction(recurring: RecurringTransaction) {
        _recurring.value = _recurring.value + recurring
    }

    override suspend fun deleteRecurringTransaction(id: String) {
        _recurring.value = _recurring.value.filter { it.id != id }
    }

    override suspend fun addPaymentMethod(method: PaymentMethod) {
        _paymentMethods.value = _paymentMethods.value + method
    }
}
