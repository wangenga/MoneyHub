package com.finance.app.domain.repository

import com.finance.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for transaction operations
 */
interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionById(id: String): Flow<Transaction?>
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction): Result<Unit>
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>
    suspend fun deleteTransaction(id: String): Result<Unit>
}
