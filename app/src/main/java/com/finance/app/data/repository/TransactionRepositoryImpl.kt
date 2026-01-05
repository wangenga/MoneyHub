package com.finance.app.data.repository

import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.mapper.toDomain
import com.finance.app.data.mapper.toEntity
import com.finance.app.domain.model.SyncStatus
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.TransactionRepository
import com.finance.app.domain.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of TransactionRepository using Room database
 * Handles transaction CRUD operations with validation and sync status management
 */
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return authRepository.getCurrentUser().map { user ->
            if (user != null) {
                transactionDao.getAllTransactions(user.id).first().map { it.toDomain() }
            } else {
                emptyList()
            }
        }
    }

    override fun getTransactionsPaginated(limit: Int, offset: Int): Flow<List<Transaction>> {
        return authRepository.getCurrentUser().map { user ->
            if (user != null) {
                transactionDao.getTransactionsPaginated(user.id, limit, offset).first().map { it.toDomain() }
            } else {
                emptyList()
            }
        }
    }

    override fun getTransactionById(id: String): Flow<Transaction?> {
        return transactionDao.getTransactionById(id).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>> {
        return authRepository.getCurrentUser().map { user ->
            if (user != null) {
                transactionDao.getTransactionsByDateRange(user.id, start, end)
                    .first()
                    .map { it.toDomain() }
            } else {
                emptyList()
            }
        }
    }

    override fun getTransactionsByDateRangePaginated(start: Long, end: Long, limit: Int, offset: Int): Flow<List<Transaction>> {
        return authRepository.getCurrentUser().map { user ->
            if (user != null) {
                transactionDao.getTransactionsByDateRangePaginated(user.id, start, end, limit, offset)
                    .first()
                    .map { it.toDomain() }
            } else {
                emptyList()
            }
        }
    }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> {
        return authRepository.getCurrentUser().map { user ->
            if (user != null) {
                transactionDao.getTransactionsByCategory(user.id, categoryId)
                    .first()
                    .map { it.toDomain() }
            } else {
                emptyList()
            }
        }
    }

    override fun getTransactionsByCategoryPaginated(categoryId: String, limit: Int, offset: Int): Flow<List<Transaction>> {
        return authRepository.getCurrentUser().map { user ->
            if (user != null) {
                transactionDao.getTransactionsByCategoryPaginated(user.id, categoryId, limit, offset)
                    .first()
                    .map { it.toDomain() }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction): Result<Unit> {
        return try {
            // Validate transaction
            validateTransaction(transaction)
            
            // Get current user
            val user = authRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Set sync status to PENDING and ensure userId is set
            val transactionToInsert = transaction.copy(
                userId = user.id,
                syncStatus = SyncStatus.PENDING
            )
            
            transactionDao.insert(transactionToInsert.toEntity())
            
            // Schedule post-operation sync
            syncScheduler.schedulePostOperationSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        return try {
            // Validate transaction
            validateTransaction(transaction)
            
            // Get current user
            val user = authRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Update timestamp and set sync status to PENDING
            val currentTime = System.currentTimeMillis()
            val transactionToUpdate = transaction.copy(
                userId = user.id,
                updatedAt = currentTime,
                syncStatus = SyncStatus.PENDING
            )
            
            transactionDao.update(transactionToUpdate.toEntity())
            
            // Schedule post-operation sync
            syncScheduler.schedulePostOperationSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> {
        return try {
            // Verify user is authenticated
            authRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Mark transaction as PENDING for deletion sync before deleting
            val transaction = transactionDao.getTransactionById(id).first()
            if (transaction != null) {
                val updatedTransaction = transaction.copy(
                    syncStatus = SyncStatus.PENDING.name,
                    updatedAt = System.currentTimeMillis()
                )
                transactionDao.update(updatedTransaction)
            }
            
            // Delete the transaction
            transactionDao.delete(id)
            
            // Schedule post-operation sync
            syncScheduler.schedulePostOperationSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates transaction data
     * @throws IllegalArgumentException if validation fails
     */
    private fun validateTransaction(transaction: Transaction) {
        // Validate amount is positive
        if (transaction.amount <= 0) {
            throw IllegalArgumentException("Transaction amount must be positive")
        }
        
        // Validate required fields
        if (transaction.categoryId.isBlank()) {
            throw IllegalArgumentException("Category is required")
        }
        
        if (transaction.userId.isBlank()) {
            throw IllegalArgumentException("User ID is required")
        }
        
        // Validate date is not in the future
        if (transaction.date > System.currentTimeMillis()) {
            throw IllegalArgumentException("Transaction date cannot be in the future")
        }
    }
}
