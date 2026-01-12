package com.finance.app.domain.repository

import com.finance.app.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for recurring transaction operations
 */
interface RecurringTransactionRepository {
    
    /**
     * Get all recurring transactions for the current user
     * @return Flow of recurring transaction list for reactive updates
     */
    fun getRecurringTransactions(): Flow<List<RecurringTransaction>>
    
    /**
     * Get all active recurring transactions for the current user
     * @return Flow of active recurring transaction list
     */
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>>
    
    /**
     * Get recurring transaction by ID
     * @param id The recurring transaction ID
     * @return Flow of recurring transaction or null if not found
     */
    fun getRecurringTransactionById(id: String): Flow<RecurringTransaction?>
    
    /**
     * Get recurring transactions that are due (nextDueDate <= current time)
     * @return List of due recurring transactions
     */
    suspend fun getNextDueTransactions(): List<RecurringTransaction>
    
    /**
     * Create a new recurring transaction
     * @param recurringTransaction The recurring transaction to create
     * @return Result indicating success or failure
     */
    suspend fun createRecurringTransaction(recurringTransaction: RecurringTransaction): Result<Unit>
    
    /**
     * Update an existing recurring transaction
     * @param recurringTransaction The recurring transaction to update
     * @return Result indicating success or failure
     */
    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction): Result<Unit>
    
    /**
     * Delete a recurring transaction by ID
     * @param id The recurring transaction ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteRecurringTransaction(id: String): Result<Unit>
    
    /**
     * Deactivate a recurring transaction (set isActive to false)
     * @param id The recurring transaction ID to deactivate
     * @return Result indicating success or failure
     */
    suspend fun deactivateRecurringTransaction(id: String): Result<Unit>
    
    /**
     * Update the next due date for a recurring transaction
     * @param id The recurring transaction ID
     * @param nextDueDate The new next due date
     * @return Result indicating success or failure
     */
    suspend fun updateNextDueDate(id: String, nextDueDate: Long): Result<Unit>
}