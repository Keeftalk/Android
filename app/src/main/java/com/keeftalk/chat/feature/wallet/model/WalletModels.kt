package com.keeftalk.chat.feature.wallet.model

data class Transaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val amount: Double,
    val date: Long,
    val merchant: String,
    val categoryId: String,
    val type: TransactionType,
    val note: String? = null
)

enum class TransactionType {
    WALLET_RECEIVE, WALLET_SEND, EXPENSE, INCOME
}

data class WalletCategory(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val budget: Double
)

data class SavingsGoal(
    val target: Double,
    val saved: Double
)

data class RecurringTransaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val merchant: String,
    val amount: Double,
    val categoryId: String,
    val frequency: String,
    val nextDue: Long
)

data class PaymentMethod(
    val id: String,
    val name: String,
    val icon: String
)
