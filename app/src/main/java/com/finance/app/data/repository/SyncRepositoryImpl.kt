package com.finance.app.data.repository

import android.content.SharedPreferences
import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.mapper.toDomain
import com.finance.app.data.mapper.toEntity
import com.finance.app.data.remote.FirestoreDataSource
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.SyncStatus
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.SyncRepository
import com.finance.app.domain.repository.SyncState
import com.finance.app.util.ErrorHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SyncRepository with enhanced error handling and retry logic
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val authRepository: AuthRepository,
    private val sharedPreferences: SharedPreferences,
    private val errorHandler: ErrorHandler
) : SyncRepository {
    
    companion object {
        private const val LAST_SYNC_TIMESTAMP_KEY = "last_sync_timestamp"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    private val _syncStatus = MutableStateFlow<SyncState>(SyncState.Idle)
    
    override fun getSyncStatus(): Flow<SyncState> = _syncStatus.asStateFlow()
    
    override suspend fun getLastSyncTimestamp(): Long? {
        val timestamp = sharedPreferences.getLong(LAST_SYNC_TIMESTAMP_KEY, -1L)
        return if (timestamp == -1L) null else timestamp
    }
    
    override suspend fun syncAll(): Result<Unit> {
        return executeWithRetry { performSyncAll() }
    }
    
    override suspend fun forceSyncAll(): Result<Unit> {
        // Clear last sync timestamp to force full sync
        sharedPreferences.edit().remove(LAST_SYNC_TIMESTAMP_KEY).apply()
        return syncAll()
    }
    
    override suspend fun syncTransactions(): Result<Unit> {
        return executeWithRetry { performTransactionSync() }
    }
    
    override suspend fun syncCategories(): Result<Unit> {
        return executeWithRetry { performCategorySync() }
    }
    
    /**
     * Executes sync operation with intelligent retry strategy
     */
    private suspend fun executeWithRetry(
        operation: suspend () -> Result<Unit>
    ): Result<Unit> {
        _syncStatus.value = SyncState.Syncing
        
        var lastException: Exception? = null
        
        for (attempt in 0 until MAX_RETRY_ATTEMPTS) {
            try {
                val result = operation()
                if (result.isSuccess) {
                    val currentTime = System.currentTimeMillis()
                    sharedPreferences.edit()
                        .putLong(LAST_SYNC_TIMESTAMP_KEY, currentTime)
                        .apply()
                    
                    _syncStatus.value = SyncState.Success(currentTime)
                    return result
                }
                
                lastException = result.exceptionOrNull() as? Exception
                    ?: Exception("Unknown sync error")
                
            } catch (e: Exception) {
                lastException = e
            }
            
            // Check if error is recoverable before retrying
            if (lastException != null && !errorHandler.isRecoverableError(lastException!!)) {
                break
            }
            
            // Apply intelligent delay before retry
            if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                val delayMs = errorHandler.getRetryDelay(lastException!!, attempt)
                delay(delayMs)
            }
        }
        
        val errorMessage = lastException?.let { errorHandler.getErrorMessage(it) }
            ?: "Sync failed after $MAX_RETRY_ATTEMPTS attempts"
        
        _syncStatus.value = SyncState.Error(errorMessage, MAX_RETRY_ATTEMPTS)
        return Result.failure(lastException ?: Exception(errorMessage))
    }
    
    /**
     * Performs complete synchronization of all data
     */
    private suspend fun performSyncAll(): Result<Unit> {
        // Verify user is authenticated
        authRepository.getCurrentUser().first()
            ?: return Result.failure(Exception("User not authenticated"))
        
        // Sync transactions first, then categories
        val transactionResult = performTransactionSync()
        if (transactionResult.isFailure) {
            return transactionResult
        }
        
        val categoryResult = performCategorySync()
        if (categoryResult.isFailure) {
            return categoryResult
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Performs transaction synchronization
     */
    private suspend fun performTransactionSync(): Result<Unit> {
        val user = authRepository.getCurrentUser().first()
            ?: return Result.failure(Exception("User not authenticated"))
        
        try {
            // Step 1: Upload pending local transactions
            val pendingTransactions = transactionDao.getTransactionsByStatus(user.id, SyncStatus.PENDING.name)
                .map { it.toDomain() }
            
            if (pendingTransactions.isNotEmpty()) {
                val uploadResult = firestoreDataSource.saveTransactionsBatch(user.id, pendingTransactions)
                if (uploadResult.isFailure) {
                    return uploadResult
                }
                
                // Mark uploaded transactions as synced
                pendingTransactions.forEach { transaction ->
                    val syncedTransaction = transaction.copy(syncStatus = SyncStatus.SYNCED)
                    transactionDao.update(syncedTransaction.toEntity())
                }
            }
            
            // Step 2: Download remote changes
            val lastSyncTimestamp = getLastSyncTimestamp() ?: 0L
            val remoteTransactionsResult = if (lastSyncTimestamp > 0) {
                firestoreDataSource.getTransactionsUpdatedAfter(user.id, lastSyncTimestamp)
            } else {
                firestoreDataSource.getTransactions(user.id)
            }
            
            if (remoteTransactionsResult.isFailure) {
                return Result.failure(remoteTransactionsResult.exceptionOrNull()!!)
            }
            
            val remoteTransactions = remoteTransactionsResult.getOrNull() ?: emptyList()
            
            // Step 3: Resolve conflicts and merge data
            for (remoteTransaction in remoteTransactions) {
                val localTransaction = transactionDao.getTransactionById(remoteTransaction.id).first()
                
                if (localTransaction == null) {
                    // New remote transaction - insert locally
                    val syncedTransaction = remoteTransaction.copy(syncStatus = SyncStatus.SYNCED)
                    transactionDao.insert(syncedTransaction.toEntity())
                } else {
                    // Conflict resolution using last-write-wins (timestamp comparison)
                    val localDomain = localTransaction.toDomain()
                    if (remoteTransaction.updatedAt > localDomain.updatedAt) {
                        // Remote is newer - update local
                        val syncedTransaction = remoteTransaction.copy(syncStatus = SyncStatus.SYNCED)
                        transactionDao.update(syncedTransaction.toEntity())
                    }
                    // If local is newer or equal, keep local version (already uploaded)
                }
            }
            
            return Result.success(Unit)
            
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Performs category synchronization
     */
    private suspend fun performCategorySync(): Result<Unit> {
        val user = authRepository.getCurrentUser().first()
            ?: return Result.failure(Exception("User not authenticated"))
        
        try {
            // Step 1: Upload pending local categories (if any have sync status)
            // Note: Categories don't have sync status in current model, so we sync all user categories
            val localCategories = categoryDao.getCategoriesByUserId(user.id).map { it.toDomain() }
            
            if (localCategories.isNotEmpty()) {
                val uploadResult = firestoreDataSource.saveCategoriesBatch(user.id, localCategories)
                if (uploadResult.isFailure) {
                    return uploadResult
                }
            }
            
            // Step 2: Download remote changes
            val lastSyncTimestamp = getLastSyncTimestamp() ?: 0L
            val remoteCategoriesResult = if (lastSyncTimestamp > 0) {
                firestoreDataSource.getCategoriesUpdatedAfter(user.id, lastSyncTimestamp)
            } else {
                firestoreDataSource.getCategories(user.id)
            }
            
            if (remoteCategoriesResult.isFailure) {
                return Result.failure(remoteCategoriesResult.exceptionOrNull()!!)
            }
            
            val remoteCategories = remoteCategoriesResult.getOrNull() ?: emptyList()
            
            // Step 3: Resolve conflicts and merge data
            for (remoteCategory in remoteCategories) {
                val localCategory = categoryDao.getCategoryById(remoteCategory.id).first()
                
                if (localCategory == null) {
                    // New remote category - insert locally
                    categoryDao.insert(remoteCategory.toEntity())
                } else {
                    // Conflict resolution using last-write-wins (timestamp comparison)
                    val localDomain = localCategory.toDomain()
                    if (remoteCategory.updatedAt > localDomain.updatedAt) {
                        // Remote is newer - update local
                        categoryDao.update(remoteCategory.toEntity())
                    }
                    // If local is newer or equal, keep local version (already uploaded)
                }
            }
            
            return Result.success(Unit)
            
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}