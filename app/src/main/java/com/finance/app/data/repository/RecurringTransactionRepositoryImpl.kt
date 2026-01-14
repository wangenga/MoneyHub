package com.finance.app.data.repository

import com.finance.app.data.local.dao.RecurringTransactionDao
import com.finance.app.data.mapper.toDomain
import com.finance.app.data.mapper.toEntity
import com.finance.app.data.remote.FirestoreDataSource
import com.finance.app.domain.model.RecurringTransaction
import com.finance.app.domain.model.SyncStatus
import com.finance.app.domain.model.isValid
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.RecurringTransactionRepository
import com.finance.app.domain.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Implementation of RecurringTransactionRepository using Room database and Firestore
 * Handles recurring transaction CRUD operations with validation and sync status management
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecurringTransactionRepositoryImpl @Inject constructor(
    private val recurringTransactionDao: RecurringTransactionDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler
) : RecurringTransactionRepository {

    override fun getRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return authRepository.getCurrentUser().flatMapLatest { user ->
            if (user != null) {
                recurringTransactionDao.getRecurringTransactionsForUser(user.id).map { entities ->
                    entities.map { it.toDomain() }
                }
            } else {
                flowOf(emptyList())
            }
        }
    }

    override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return authRepository.getCurrentUser().flatMapLatest { user ->
            if (user != null) {
                recurringTransactionDao.getActiveRecurringTransactionsForUser(user.id).map { entities ->
                    entities.map { it.toDomain() }
                }
            } else {
                flowOf(emptyList())
            }
        }
    }

    override fun getRecurringTransactionById(id: String): Flow<RecurringTransaction?> {
        return recurringTransactionDao.getRecurringTransactionsForUser("").map { entities ->
            entities.find { it.id == id }?.toDomain()
        }
    }

    override suspend fun getNextDueTransactions(): List<RecurringTransaction> {
        return try {
            val currentTime = System.currentTimeMillis()
            recurringTransactionDao.getDueRecurringTransactions(currentTime)
                .map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun createRecurringTransaction(recurringTransaction: RecurringTransaction): Result<Unit> {
        return try {
            // Validate recurring transaction
            validateRecurringTransaction(recurringTransaction)
            
            // Get current user
            val user = authRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Ensure userId is set and add timestamps
            val currentTime = System.currentTimeMillis()
            val recurringTransactionToInsert = recurringTransaction.copy(
                userId = user.id,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            
            recurringTransactionDao.insertRecurringTransaction(recurringTransactionToInsert.toEntity())
            
            // Schedule post-operation sync
            syncScheduler.schedulePostOperationSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction): Result<Unit> {
        return try {
            // Validate recurring transaction
            validateRecurringTransaction(recurringTransaction)
            
            // Get current user
            val user = authRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Update timestamp and ensure userId is set
            val currentTime = System.currentTimeMillis()
            val recurringTransactionToUpdate = recurringTransaction.copy(
                userId = user.id,
                updatedAt = currentTime
            )
            
            recurringTransactionDao.updateRecurringTransaction(recurringTransactionToUpdate.toEntity())
            
            // Schedule post-operation sync
            syncScheduler.schedulePostOperationSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRecurringTransaction(id: String): Result<Unit> {
        return try {
            // Verify user is authenticated
            authRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Delete the recurring transaction
            recurringTransactionDao.deleteRecurringTransactionById(id)
            
            // Schedule post-operation sync
            syncScheduler.schedulePostOperationSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deactivateRecurringTransaction(id: String): Result<Unit> {
        return try {
            // Verify user is authenticated
            authRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val currentTime = System.currentTimeMillis()
            recurringTransactionDao.deactivateRecurringTransaction(id, currentTime)
            
            // Schedule post-operation sync
            syncScheduler.schedulePostOperationSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNextDueDate(id: String, nextDueDate: Long): Result<Unit> {
        return try {
            // Verify user is authenticated
            authRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val currentTime = System.currentTimeMillis()
            recurringTransactionDao.updateNextDueDate(id, nextDueDate, currentTime)
            
            // Schedule post-operation sync
            syncScheduler.schedulePostOperationSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates recurring transaction data
     * @throws IllegalArgumentException if validation fails
     */
    private fun validateRecurringTransaction(recurringTransaction: RecurringTransaction) {
        // Validate using domain model validation
        if (!recurringTransaction.isValid()) {
            throw IllegalArgumentException("Recurring transaction validation failed")
        }
        
        // Validate template transaction
        if (!recurringTransaction.templateTransaction.isValid()) {
            throw IllegalArgumentException("Template transaction validation failed")
        }
        
        // Validate next due date is not in the past (with some tolerance for immediate scheduling)
        val currentTime = System.currentTimeMillis()
        val tolerance = 60 * 1000L // 1 minute tolerance
        if (recurringTransaction.nextDueDate < (currentTime - tolerance)) {
            throw IllegalArgumentException("Next due date cannot be in the past")
        }
        
        // Validate template transaction amount is positive
        if (recurringTransaction.templateTransaction.amount <= 0) {
            throw IllegalArgumentException("Template transaction amount must be positive")
        }
        
        // Validate required fields
        if (recurringTransaction.templateTransaction.categoryId.isBlank()) {
            throw IllegalArgumentException("Template transaction category is required")
        }
    }
    
    /**
     * Sync local recurring transactions with remote Firestore
     * This method is called by the sync system
     */
    suspend fun syncRecurringTransactions(userId: String): Result<Unit> {
        return try {
            // Get remote recurring transactions
            val remoteResult = firestoreDataSource.getRecurringTransactions(userId)
            if (remoteResult.isFailure) {
                return Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to fetch remote recurring transactions"))
            }
            
            val remoteRecurringTransactions = remoteResult.getOrNull() ?: emptyList()
            
            // Get local recurring transactions
            val localRecurringTransactions = recurringTransactionDao.getRecurringTransactionsForUser(userId).first()
                .map { it.toDomain() }
            
            // Sync logic: merge remote and local data
            val mergedRecurringTransactions = mergeRecurringTransactions(localRecurringTransactions, remoteRecurringTransactions)
            
            // Update local database
            mergedRecurringTransactions.forEach { recurringTransaction ->
                recurringTransactionDao.insertRecurringTransaction(recurringTransaction.toEntity())
            }
            
            // Push local changes to remote
            val localPendingChanges = localRecurringTransactions.filter { 
                it.templateTransaction.syncStatus == SyncStatus.PENDING 
            }
            
            if (localPendingChanges.isNotEmpty()) {
                val batchResult = firestoreDataSource.saveRecurringTransactionsBatch(userId, localPendingChanges)
                if (batchResult.isFailure) {
                    return Result.failure(batchResult.exceptionOrNull() ?: Exception("Failed to sync local changes"))
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Merge local and remote recurring transactions, resolving conflicts by timestamp
     */
    private fun mergeRecurringTransactions(
        local: List<RecurringTransaction>, 
        remote: List<RecurringTransaction>
    ): List<RecurringTransaction> {
        val merged = mutableMapOf<String, RecurringTransaction>()
        
        // Add all local recurring transactions
        local.forEach { merged[it.id] = it }
        
        // Add remote recurring transactions, resolving conflicts by timestamp
        remote.forEach { remoteRecurringTransaction ->
            val localRecurringTransaction = merged[remoteRecurringTransaction.id]
            if (localRecurringTransaction == null || remoteRecurringTransaction.updatedAt > localRecurringTransaction.updatedAt) {
                // Remote is newer or doesn't exist locally
                merged[remoteRecurringTransaction.id] = remoteRecurringTransaction.copy(
                    templateTransaction = remoteRecurringTransaction.templateTransaction.copy(
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            }
        }
        
        return merged.values.toList()
    }
}