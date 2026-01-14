package com.finance.app.domain.usecase

/**
 * Interface for scheduling and managing recurring transactions
 */
interface RecurringTransactionScheduler {
    
    /**
     * Start monitoring for due recurring transactions
     * This should be called when the app starts
     */
    suspend fun startScheduling()
    
    /**
     * Stop monitoring for due recurring transactions
     */
    suspend fun stopScheduling()
    
    /**
     * Schedule a specific recurring transaction for monitoring
     * @param recurringTransactionId The ID of the recurring transaction to schedule
     */
    suspend fun scheduleRecurringTransaction(recurringTransactionId: String)
    
    /**
     * Cancel scheduling for a specific recurring transaction
     * @param recurringTransactionId The ID of the recurring transaction to cancel
     */
    suspend fun cancelRecurringTransaction(recurringTransactionId: String)
    
    /**
     * Process all due recurring transactions immediately
     * This is typically called by the background worker
     */
    suspend fun processDueTransactions()
    
    /**
     * Reschedule all active recurring transactions
     * This is useful after app updates or configuration changes
     */
    suspend fun rescheduleAllTransactions()
}