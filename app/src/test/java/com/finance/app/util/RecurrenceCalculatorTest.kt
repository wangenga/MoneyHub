package com.finance.app.util

import com.finance.app.domain.model.RecurrencePattern
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for RecurrenceCalculator
 */
class RecurrenceCalculatorTest {

    @Test
    fun `calculateNextDueDate for daily pattern adds one day`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JANUARY, 15, 10, 0, 0)
        val startDate = calendar.timeInMillis
        
        val nextDate = RecurrenceCalculator.calculateNextDueDate(startDate, RecurrencePattern.DAILY)
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val expectedDate = calendar.timeInMillis
        
        assertEquals(expectedDate, nextDate)
    }

    @Test
    fun `calculateNextDueDate for weekly pattern adds one week`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JANUARY, 15, 10, 0, 0)
        val startDate = calendar.timeInMillis
        
        val nextDate = RecurrenceCalculator.calculateNextDueDate(startDate, RecurrencePattern.WEEKLY)
        
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val expectedDate = calendar.timeInMillis
        
        assertEquals(expectedDate, nextDate)
    }

    @Test
    fun `calculateNextDueDate for monthly pattern adds one month`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JANUARY, 15, 10, 0, 0)
        val startDate = calendar.timeInMillis
        
        val nextDate = RecurrenceCalculator.calculateNextDueDate(startDate, RecurrencePattern.MONTHLY)
        
        calendar.add(Calendar.MONTH, 1)
        val expectedDate = calendar.timeInMillis
        
        assertEquals(expectedDate, nextDate)
    }

    @Test
    fun `isOverdue returns true for past dates`() {
        val pastDate = System.currentTimeMillis() - 60000 // 1 minute ago
        assertTrue(RecurrenceCalculator.isOverdue(pastDate))
    }

    @Test
    fun `calculateFutureDueDates generates correct number of dates`() {
        val startDate = System.currentTimeMillis()
        val futureDates = RecurrenceCalculator.calculateFutureDueDates(
            startDate, 
            RecurrencePattern.DAILY, 
            5
        )
        
        assertEquals(5, futureDates.size)
        
        // Verify dates are in ascending order
        for (i in 1 until futureDates.size) {
            assertTrue(futureDates[i] > futureDates[i-1])
        }
    }
}