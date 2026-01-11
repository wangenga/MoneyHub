package com.finance.app.domain.model

/**
 * Domain model representing a recurring transaction configuration.
 * 
 * Recurring transactions allow users to automate regular income and expense entries
 * by defining a template transaction and recurrence pattern. The system automatically
 * creates new transaction instances based on the schedule.
 * 
 * @property id Unique identifier for the recurring transaction
 * @property userId Owner's user ID
 * @property templateTransaction The transaction template used to create instances
 * @property recurrencePattern The frequency pattern (daily, weekly, monthly)
 * @property nextDueDate Timestamp when the next transaction should be created
 * @property isActive Whether this recurring transaction is currently active
 * @property createdAt Timestamp when the recurring transaction was created
 * @property updatedAt Timestamp when the recurring transaction was last updated
 */
data class RecurringTransaction(
    val id: String,
    val userId: String,
    val templateTransaction: Transaction,
    val recurrencePattern: RecurrencePattern,
    val nextDueDate: Long,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        /**
         * Validates a next due date
         * @param nextDueDate The date to validate
         * @return true if the date is valid (not in the past), false otherwise
         */
        fun isValidNextDueDate(nextDueDate: Long): Boolean {
            return nextDueDate > System.currentTimeMillis()
        }
    }
    
    /**
     * Validates this recurring transaction instance
     * @return true if all fields are valid, false otherwise
     */
    fun isValid(): Boolean {
        return id.isNotBlank() &&
                userId.isNotBlank() &&
                templateTransaction.isValid() &&
                nextDueDate > 0 &&
                createdAt > 0 &&
                updatedAt > 0
    }
    
    /**
     * Calculates the next due date based on the current recurrence pattern
     * @return The next due date in milliseconds
     */
    fun calculateNextDueDate(): Long {
        return recurrencePattern.calculateNextDueDate(nextDueDate)
    }
    
    /**
     * Creates a new transaction instance from this recurring transaction template
     * @param instanceDate The date for the new transaction instance
     * @return A new Transaction based on the template
     */
    fun createTransactionInstance(instanceDate: Long = nextDueDate): Transaction {
        return templateTransaction.copy(
            id = java.util.UUID.randomUUID().toString(),
            date = instanceDate,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
    }
}