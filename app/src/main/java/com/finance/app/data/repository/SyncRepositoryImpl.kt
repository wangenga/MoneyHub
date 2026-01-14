package com.finance.app.data.repository

import android.content.SharedPreferences
import com.finance.app.data.local.dao.BudgetDao
import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.local.dao.RecurringTransactionDao
import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.mapper.toDomain
import com.finance.app.data.mapper.toEntity
import com.finance.app.data.remote.FirestoreDataSource
import com.finance.app.domain.model.Budget
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.RecurringTransaction
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
    private val budgetDao: BudgetDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val authRepository: AuthRepository,
    private val sharedPreferences: SharedPreferences,
    private val errorHandler: ErrorHandler
) : SyncRepository {
    
    companion object {
        private const val LAST_SYNC_TIMESTAMP_KEY = "last_sync_timestamp"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val LAST_BUDGET_SYNC_TIMESTAMP_KEY = "last_budget_sync_timestamp"
        private const val LAST_RECURRING_TRANSACTION_SYNC_TIMESTAMP_KEY = "last_recurring_transaction_sync_timestamp"
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
            if (lastException != null && !errorHandler.isRecoverableError(lastException)) {
                break
            }
            
            // Apply intelligent delay before retry
            if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                val delayMs = errorHandler.getRetryDelay(lastException ?: Exception("Unknown error"), attempt)
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
        
        // Sync transactions first, then categories, budgets, and recurring transactions
        val transactionResult = performTransactionSync()
        if (transactionResult.isFailure) {
            return transactionResult
        }
        
        val categoryResult = performCategorySync()
        if (categoryResult.isFailure) {
            return categoryResult
        }
        
        val budgetResult = performBudgetSync()
        if (budgetResult.isFailure) {
            return budgetResult
        }
        
        val recurringTransactionResult = performRecurringTransactionSync()
        if (recurringTransactionResult.isFailure) {
            return recurringTransactionResult
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Performs transaction synchronization with delta sync optimization
     */
    private suspend fun performTransactionSync(): Result<Unit> {
        val user = authRepository.getCurrentUser().first()
            ?: return Result.failure(Exception("User not authenticated"))
        
        try {
            val lastSyncTimestamp = getLastSyncTimestamp() ?: 0L
            
            // Step 1: Upload pending local transactions (delta sync - only PENDING items)
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
            
            // Step 2: Download remote changes (delta sync - only items updated after last sync)
            val remoteTransactionsResult = if (lastSyncTimestamp > 0) {
                // Delta sync: only get items updated since last sync
                firestoreDataSource.getTransactionsUpdatedAfter(user.id, lastSyncTimestamp)
            } else {
                // Full sync: first time sync, get all items
                firestoreDataSource.getTransactions(user.id)
            }
            
            if (remoteTransactionsResult.isFailure) {
                return Result.failure(remoteTransactionsResult.exceptionOrNull() ?: Exception("Unknown sync error"))
            }
            
            val remoteTransactions = remoteTransactionsResult.getOrNull() ?: emptyList()
            
            // Step 3: Resolve conflicts and merge data (only for changed items)
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
     * Performs category synchronization with delta sync optimization
     */
    private suspend fun performCategorySync(): Result<Unit> {
        val user = authRepository.getCurrentUser().first()
            ?: return Result.failure(Exception("User not authenticated"))
        
        try {
            val lastSyncTimestamp = getLastSyncTimestamp() ?: 0L
            
            // Step 1: Upload local categories (delta sync - only user-created categories that changed)
            val localCategories = categoryDao.getCategoriesByUserId(user.id).map { it.toDomain() }
            val categoriesToSync = if (lastSyncTimestamp > 0) {
                // Delta sync: only sync categories updated after last sync
                localCategories.filter { it.updatedAt > lastSyncTimestamp }
            } else {
                // Full sync: sync all user categories
                localCategories
            }
            
            if (categoriesToSync.isNotEmpty()) {
                val uploadResult = firestoreDataSource.saveCategoriesBatch(user.id, categoriesToSync)
                if (uploadResult.isFailure) {
                    return uploadResult
                }
            }
            
            // Step 2: Download remote changes (delta sync - only items updated after last sync)
            val remoteCategoriesResult = if (lastSyncTimestamp > 0) {
                // Delta sync: only get categories updated since last sync
                firestoreDataSource.getCategoriesUpdatedAfter(user.id, lastSyncTimestamp)
            } else {
                // Full sync: first time sync, get all categories
                firestoreDataSource.getCategories(user.id)
            }
            
            if (remoteCategoriesResult.isFailure) {
                return Result.failure(remoteCategoriesResult.exceptionOrNull() ?: Exception("Unknown sync error"))
            }
            
            val remoteCategories = remoteCategoriesResult.getOrNull() ?: emptyList()
            
            // Step 3: Resolve conflicts and merge data (only for changed items)
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
    
    /**
     * Performs budget synchronization with delta sync optimization and conflict resolution
     */
    private suspend fun performBudgetSync(): Result<Unit> {
        val user = authRepository.getCurrentUser().first()
            ?: return Result.failure(Exception("User not authenticated"))
        
        try {
            val lastSyncTimestamp = sharedPreferences.getLong(LAST_BUDGET_SYNC_TIMESTAMP_KEY, -1L)
                .let { if (it == -1L) null else it }
            
            // Step 1: Upload pending local budgets (delta sync - only changed items)
            val localBudgets = budgetDao.getBudgetsForUser(user.id).first()
                .map { it.toDomain() }
            
            val budgetsToSync = if (lastSyncTimestamp != null) {
                // Delta sync: only sync budgets updated after last sync
                localBudgets.filter { it.updatedAt > lastSyncTimestamp }
            } else {
                // Full sync: sync all budgets
                localBudgets
            }
            
            if (budgetsToSync.isNotEmpty()) {
                val uploadResult = firestoreDataSource.saveBudgetsBatch(user.id, budgetsToSync)
                if (uploadResult.isFailure) {
                    return uploadResult
                }
            }
            
            // Step 2: Download remote changes (delta sync - only items updated after last sync)
            val remoteBudgetsResult = if (lastSyncTimestamp != null) {
                // Delta sync: only get budgets updated since last sync
                firestoreDataSource.getBudgetsUpdatedAfter(user.id, lastSyncTimestamp)
            } else {
                // Full sync: first time sync, get all budgets
                firestoreDataSource.getBudgets(user.id)
            }
            
            if (remoteBudgetsResult.isFailure) {
                return Result.failure(remoteBudgetsResult.exceptionOrNull() ?: Exception("Unknown sync error"))
            }
            
            val remoteBudgets = remoteBudgetsResult.getOrNull() ?: emptyList()
            
            // Step 3: Resolve conflicts and merge data using timestamp-based conflict resolution
            for (remoteBudget in remoteBudgets) {
                val localBudget = budgetDao.getBudgetByIdFlow(remoteBudget.id).first()
                
                if (localBudget == null) {
                    // New remote budget - insert locally
                    budgetDao.insertBudget(remoteBudget.toEntity())
                } else {
                    // Conflict resolution using last-write-wins (timestamp comparison)
                    val localDomain = localBudget.toDomain()
                    if (remoteBudget.updatedAt > localDomain.updatedAt) {
                        // Remote is newer - update local
                        budgetDao.insertBudget(remoteBudget.toEntity())
                    }
                    // If local is newer or equal, keep local version (already uploaded)
                }
            }
            
            // Update last sync timestamp
            val currentTime = System.currentTimeMillis()
            sharedPreferences.edit()
                .putLong(LAST_BUDGET_SYNC_TIMESTAMP_KEY, currentTime)
                .apply()
            
            return Result.success(Unit)
            
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Performs recurring transaction synchronization with delta sync optimization and conflict resolution
     */
    private suspend fun performRecurringTransactionSync(): Result<Unit> {
        val user = authRepository.getCurrentUser().first()
            ?: return Result.failure(Exception("User not authenticated"))
        
        try {
            val lastSyncTimestamp = sharedPreferences.getLong(LAST_RECURRING_TRANSACTION_SYNC_TIMESTAMP_KEY, -1L)
                .let { if (it == -1L) null else it }
            
            // Step 1: Upload pending local recurring transactions (delta sync - only changed items)
            val localRecurringTransactions = recurringTransactionDao.getRecurringTransactionsForUser(user.id).first()
                .map { it.toDomain() }
            
            val recurringTransactionsToSync = if (lastSyncTimestamp != null) {
                // Delta sync: only sync recurring transactions updated after last sync
                localRecurringTransactions.filter { it.updatedAt > lastSyncTimestamp }
            } else {
                // Full sync: sync all recurring transactions
                localRecurringTransactions
            }
            
            if (recurringTransactionsToSync.isNotEmpty()) {
                val uploadResult = firestoreDataSource.saveRecurringTransactionsBatch(user.id, recurringTransactionsToSync)
                if (uploadResult.isFailure) {
                    return uploadResult
                }
            }
            
            // Step 2: Download remote changes (delta sync - only items updated after last sync)
            val remoteRecurringTransactionsResult = if (lastSyncTimestamp != null) {
                // Delta sync: only get recurring transactions updated since last sync
                firestoreDataSource.getRecurringTransactionsUpdatedAfter(user.id, lastSyncTimestamp)
            } else {
                // Full sync: first time sync, get all recurring transactions
                firestoreDataSource.getRecurringTransactions(user.id)
            }
            
            if (remoteRecurringTransactionsResult.isFailure) {
                return Result.failure(remoteRecurringTransactionsResult.exceptionOrNull() ?: Exception("Unknown sync error"))
            }
            
            val remoteRecurringTransactions = remoteRecurringTransactionsResult.getOrNull() ?: emptyList()
            
            // Step 3: Resolve conflicts and merge data using timestamp-based conflict resolution
            for (remoteRecurringTransaction in remoteRecurringTransactions) {
                val localRecurringTransaction = recurringTransactionDao.getRecurringTransactionByIdFlow(remoteRecurringTransaction.id).first()
                
                if (localRecurringTransaction == null) {
                    // New remote recurring transaction - insert locally
                    recurringTransactionDao.insertRecurringTransaction(remoteRecurringTransaction.toEntity())
                } else {
                    // Conflict resolution using last-write-wins (timestamp comparison)
                    val localDomain = localRecurringTransaction.toDomain()
                    if (remoteRecurringTransaction.updatedAt > localDomain.updatedAt) {
                        // Remote is newer - update local
                        recurringTransactionDao.insertRecurringTransaction(remoteRecurringTransaction.toEntity())
                    }
                    // If local is newer or equal, keep local version (already uploaded)
                }
            }
            
            // Update last sync timestamp
            val currentTime = System.currentTimeMillis()
            sharedPreferences.edit()
                .putLong(LAST_RECURRING_TRANSACTION_SYNC_TIMESTAMP_KEY, currentTime)
                .apply()
            
            return Result.success(Unit)
            
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}