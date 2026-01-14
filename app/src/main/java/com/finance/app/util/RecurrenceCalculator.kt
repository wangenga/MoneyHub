package com.finance.app.util

import com.finance.app.domain.model.RecurrencePattern
import java.util.*

/**
 * Utility class for calculating recurring transaction due dates
 * Provides robust date calculation logic for different recurrence patterns
 */
object RecurrenceCalculator {
    
    /**
     * Calculate the next due date based on recurrence pattern and current date
     * @param currentDate The current due date in milliseconds
     * @param pattern The recurrence pattern
     * @return The next due date in milliseconds
     */
    fun calculateNextDueDate(currentDate: Long, pattern: RecurrencePattern): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDate
        
        return when (pattern) {
            RecurrencePattern.DAILY -> {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.timeInMillis
            }
            RecurrencePattern.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.timeInMillis
            }
            RecurrencePattern.MONTHLY -> {
                // Handle month-end dates properly (e.g., Jan 31 -> Feb 28/29)
                val originalDay = calendar.get(Calendar.DAY_OF_MONTH)
                calendar.add(Calendar.MONTH, 1)
                
                // If the day doesn't exist in the new month, use the last day of the month
                val maxDayInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (originalDay > maxDayInMonth) {
                    calendar.set(Calendar.DAY_OF_MONTH, maxDayInMonth)
                }
                
                calendar.timeInMillis
            }
        }
    }
    
    /**
     * Calculate multiple future due dates for a recurring transaction
     * @param startDate The starting date in milliseconds
     * @param pattern The recurrence pattern
     * @param count Number of future dates to calculate
     * @return List of future due dates in milliseconds
     */
    fun calculateFutureDueDates(startDate: Long, pattern: RecurrencePattern, count: Int): List<Long> {
        val dates = mutableListOf<Long>()
        var currentDate = startDate
        
        repeat(count) {
            currentDate = calculateNextDueDate(currentDate, pattern)
            dates.add(currentDate)
        }
        
        return dates
    }
    
    /**
     * Check if a date is overdue (past current time)
     * @param dueDate The due date to check in milliseconds
     * @return true if the date is overdue, false otherwise
     */
    fun isOverdue(dueDate: Long): Boolean {
        return dueDate <= System.currentTimeMillis()
    }
    
    /**
     * Calculate how many periods have passed since a due date
     * Useful for catching up on missed recurring transactions
     * @param dueDate The original due date in milliseconds
     * @param pattern The recurrence pattern
     * @return Number of periods that have passed
     */
    fun calculateMissedPeriods(dueDate: Long, pattern: RecurrencePattern): Int {
        val currentTime = System.currentTimeMillis()
        if (dueDate >= currentTime) return 0
        
        val timeDiff = currentTime - dueDate
        
        return when (pattern) {
            RecurrencePattern.DAILY -> {
                (timeDiff / (24 * 60 * 60 * 1000)).toInt()
            }
            RecurrencePattern.WEEKLY -> {
                (timeDiff / (7 * 24 * 60 * 60 * 1000)).toInt()
            }
            RecurrencePattern.MONTHLY -> {
                // Approximate calculation for months (more complex due to varying month lengths)
                var months = 0
                val tempCalendar = Calendar.getInstance().apply { timeInMillis = dueDate }
                
                while (tempCalendar.timeInMillis < currentTime) {
                    tempCalendar.add(Calendar.MONTH, 1)
                    months++
                }
                
                maxOf(0, months - 1) // Subtract 1 because we went one month too far
            }
        }
    }
}