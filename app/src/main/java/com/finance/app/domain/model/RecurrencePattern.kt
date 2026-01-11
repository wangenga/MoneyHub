package com.finance.app.domain.model

/**
 * Enum representing the frequency patterns for recurring transactions
 */
enum class RecurrencePattern {
    DAILY,
    WEEKLY,
    MONTHLY;
    
    companion object {
        /**
         * Converts a string to RecurrencePattern
         * @param value The string value to convert
         * @return RecurrencePattern or null if invalid
         */
        fun fromString(value: String?): RecurrencePattern? {
            return try {
                value?.let { valueOf(it.uppercase()) }
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
    
    /**
     * Calculates the next due date based on this recurrence pattern
     * @param currentDate The current date in milliseconds
     * @return The next due date in milliseconds
     */
    fun calculateNextDueDate(currentDate: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentDate
        
        return when (this) {
            DAILY -> {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.timeInMillis
            }
            WEEKLY -> {
                calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                calendar.timeInMillis
            }
            MONTHLY -> {
                calendar.add(java.util.Calendar.MONTH, 1)
                calendar.timeInMillis
            }
        }
    }
}