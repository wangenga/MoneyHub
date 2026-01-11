package com.finance.app.data.local.dao

import androidx.room.*
import com.finance.app.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for recurring transaction operations in Room database
 */
@Dao
interface RecurringTransactionDao {
    
    /**
     * Get all recurring transactions for a specific user
     * @param userId The user ID to filter by
     * @return Flow of recurring transaction list for reactive updates
     */
    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getRecurringTransactionsForUser(userId: String): Flow<List<RecurringTransactionEntity>>
    
    /**
     * Get all active recurring transactions for a specific user
     * @param userId The user ID to filter by
     * @return Flow of active recurring transaction list
     */
    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveRecurringTransactionsForUser(userId: String): Flow<List<RecurringTransactionEntity>>
    
    /**
     * Get recurring transactions that are due (nextDueDate <= current time)
     * @param currentTime The current timestamp
     * @return List of due recurring transactions
     */
    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextDueDate <= :currentTime ORDER BY nextDueDate ASC")
    suspend fun getDueRecurringTransactions(currentTime: Long): List<RecurringTransactionEntity>
    
    /**
     * Get recurring transaction by ID
     * @param recurringTransactionId The recurring transaction ID
     * @return Recurring transaction entity or null if not found
     */
    @Query("SELECT * FROM recurring_transactions WHERE id = :recurringTransactionId")
    suspend fun getRecurringTransactionById(recurringTransactionId: String): RecurringTransactionEntity?
    
    /**
     * Insert a new recurring transaction
     * @param recurringTransaction The recurring transaction entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransactionEntity)
    
    /**
     * Update an existing recurring transaction
     * @param recurringTransaction The recurring transaction entity to update
     */
    @Update
    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransactionEntity)
    
    /**
     * Delete a recurring transaction by ID
     * @param recurringTransactionId The recurring transaction ID to delete
     */
    @Query("DELETE FROM recurring_transactions WHERE id = :recurringTransactionId")
    suspend fun deleteRecurringTransactionById(recurringTransactionId: String)
    
    /**
     * Delete all recurring transactions for a user
     * @param userId The user ID
     */
    @Query("DELETE FROM recurring_transactions WHERE userId = :userId")
    suspend fun deleteAllRecurringTransactionsForUser(userId: String)
    
    /**
     * Deactivate a recurring transaction (set isActive to false)
     * @param recurringTransactionId The recurring transaction ID to deactivate
     * @param updatedAt The timestamp when the transaction was updated
     */
    @Query("UPDATE recurring_transactions SET isActive = 0, updatedAt = :updatedAt WHERE id = :recurringTransactionId")
    suspend fun deactivateRecurringTransaction(recurringTransactionId: String, updatedAt: Long)
    
    /**
     * Update the next due date for a recurring transaction
     * @param recurringTransactionId The recurring transaction ID
     * @param nextDueDate The new next due date
     * @param updatedAt The timestamp when the transaction was updated
     */
    @Query("UPDATE recurring_transactions SET nextDueDate = :nextDueDate, updatedAt = :updatedAt WHERE id = :recurringTransactionId")
    suspend fun updateNextDueDate(recurringTransactionId: String, nextDueDate: Long, updatedAt: Long)
}