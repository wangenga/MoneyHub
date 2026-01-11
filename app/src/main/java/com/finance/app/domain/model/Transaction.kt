package com.finance.app.domain.model

/**
 * Domain model representing a financial transaction
 */
data class Transaction(
    val id: String,
    val userId: String,
    val type: TransactionType,
    val amount: Double,
    val categoryId: String,
    val date: Long,
    val paymentMethod: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus
)

enum class TransactionType {
    INCOME, EXPENSE
}

enum class SyncStatus {
    SYNCED, PENDING, FAILED
}

/**
 * Extension function to validate a Transaction instance
 */
fun Transaction.isValid(): Boolean {
    return id.isNotBlank() &&
            userId.isNotBlank() &&
            amount > 0.0 &&
            categoryId.isNotBlank() &&
            date > 0 &&
            createdAt > 0 &&
            updatedAt > 0
}
