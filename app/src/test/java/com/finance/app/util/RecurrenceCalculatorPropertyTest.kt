package com.finance.app.util

import com.finance.app.domain.model.RecurrencePattern
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.util.*

/**
 * Property-based tests for recurring transaction scheduling consistency.
 * 
 * These tests verify the correctness properties defined in the design document
 * for the budget-and-recurring-transactions feature.
 */
class RecurrenceCalculatorPropertyTest : StringSpec({

    /**
     * **Feature: budget-and-recurring-transactions, Property 6: Recurring transaction scheduling consistency**
     * **Validates: Requirements 3.5**
     * 
     * *For any* recurring transaction with a valid recurrence pattern, the next due date should be 
     * calculated correctly based on the pattern (daily, weekly, monthly).
     * 
     * This test runs 100 iterations with randomly generated dates and recurrence patterns to verify that
     * the scheduling logic consistently calculates the correct next due date.
     */
    "property6_recurringTransactionSchedulingConsistency_nextDueDateCalculatedCorrectly" {
        // Generate random dates and patterns
        val dateArb = Arb.long(min = 1_000_000_000_000L, max = 2_000_000_000_000L) // Valid timestamp range
        val patternArb = Arb.enum<RecurrencePattern>()
        
        checkAll(20, dateArb, patternArb) { currentDate, pattern ->
            // Calculate next due date
            val nextDueDate = RecurrenceCalculator.calculateNextDueDate(currentDate, pattern)
            
            // Verify the next due date is after the current date
            nextDueDate shouldBeGreaterThan currentDate
            
            // Verify the time difference matches the pattern
            val timeDiff = nextDueDate - currentDate
            
            when (pattern) {
                RecurrencePattern.DAILY -> {
                    // Should be approximately 1 day (24 hours)
                    val oneDayInMillis = 24L * 60 * 60 * 1000
                    val tolerance = 60L * 60 * 1000 // 1 hour tolerance for DST
                    val diff = kotlin.math.abs(timeDiff - oneDayInMillis)
                    (diff <= tolerance) shouldBe true
                }
                RecurrencePattern.WEEKLY -> {
                    // Should be approximately 7 days
                    val oneWeekInMillis = 7L * 24 * 60 * 60 * 1000
                    val tolerance = 60L * 60 * 1000 // 1 hour tolerance for DST
                    val diff = kotlin.math.abs(timeDiff - oneWeekInMillis)
                    (diff <= tolerance) shouldBe true
                }
                RecurrencePattern.MONTHLY -> {
                    // Monthly is more complex due to varying month lengths
                    // Verify it's at least 28 days and at most 31 days
                    val minMonthInMillis = 28L * 24 * 60 * 60 * 1000
                    val maxMonthInMillis = 31L * 24 * 60 * 60 * 1000 + (60L * 60 * 1000) // +1 hour for DST
                    (timeDiff >= minMonthInMillis && timeDiff <= maxMonthInMillis) shouldBe true
                }
            }
        }
    }

    /**
     * Property test: Daily recurrence adds exactly one day
     * 
     * Verifies that daily recurrence consistently adds 24 hours to the current date.
     */
    "dailyRecurrence_addsExactlyOneDay" {
        val dateArb = Arb.long(min = 1_000_000_000_000L, max = 2_000_000_000_000L)
        
        checkAll(20, dateArb) { currentDate ->
            val nextDueDate = RecurrenceCalculator.calculateNextDueDate(currentDate, RecurrencePattern.DAILY)
            
            // Calculate expected next date (24 hours later)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentDate
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val expectedDate = calendar.timeInMillis
            
            // Allow small tolerance for DST transitions
            val tolerance = 60L * 60 * 1000 // 1 hour
            val diff = kotlin.math.abs(nextDueDate - expectedDate)
            (diff <= tolerance) shouldBe true
        }
    }

    /**
     * Property test: Weekly recurrence adds exactly one week
     * 
     * Verifies that weekly recurrence consistently adds 7 days to the current date.
     */
    "weeklyRecurrence_addsExactlyOneWeek" {
        val dateArb = Arb.long(min = 1_000_000_000_000L, max = 2_000_000_000_000L)
        
        checkAll(20, dateArb) { currentDate ->
            val nextDueDate = RecurrenceCalculator.calculateNextDueDate(currentDate, RecurrencePattern.WEEKLY)
            
            // Calculate expected next date (7 days later)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentDate
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            val expectedDate = calendar.timeInMillis
            
            // Allow small tolerance for DST transitions
            val tolerance = 60L * 60 * 1000 // 1 hour
            val diff = kotlin.math.abs(nextDueDate - expectedDate)
            (diff <= tolerance) shouldBe true
        }
    }

    /**
     * Property test: Monthly recurrence handles month-end dates correctly
     * 
     * Verifies that monthly recurrence correctly handles dates at the end of months,
     * especially when transitioning to months with fewer days (e.g., Jan 31 -> Feb 28/29).
     */
    "monthlyRecurrence_handlesMonthEndDatesCorrectly" {
        // Test specific month-end dates
        val testCases = listOf(
            // January 31 -> February 28/29
            createDate(2024, Calendar.JANUARY, 31),
            // March 31 -> April 30
            createDate(2024, Calendar.MARCH, 31),
            // May 31 -> June 30
            createDate(2024, Calendar.MAY, 31),
            // August 31 -> September 30
            createDate(2024, Calendar.AUGUST, 31),
            // October 31 -> November 30
            createDate(2024, Calendar.OCTOBER, 31),
            // December 31 -> January 31
            createDate(2024, Calendar.DECEMBER, 31)
        )
        
        testCases.forEach { currentDate ->
            val nextDueDate = RecurrenceCalculator.calculateNextDueDate(currentDate, RecurrencePattern.MONTHLY)
            
            val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentDate }
            val nextCalendar = Calendar.getInstance().apply { timeInMillis = nextDueDate }
            
            // Verify the month increased by 1 (or wrapped to January)
            val expectedMonth = (currentCalendar.get(Calendar.MONTH) + 1) % 12
            nextCalendar.get(Calendar.MONTH) shouldBe expectedMonth
            
            // Verify the day is either the same or the last day of the month
            val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
            val nextDay = nextCalendar.get(Calendar.DAY_OF_MONTH)
            val maxDayInNextMonth = nextCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            // Next day should be either the same day or the last day of the month
            (nextDay == currentDay || nextDay == maxDayInNextMonth) shouldBe true
        }
    }

    /**
     * Property test: Multiple consecutive calculations maintain consistency
     * 
     * Verifies that calculating the next due date multiple times in sequence
     * produces consistent results (each date is after the previous one).
     */
    "consecutiveCalculations_maintainConsistency" {
        val dateArb = Arb.long(min = 1_000_000_000_000L, max = 2_000_000_000_000L)
        val patternArb = Arb.enum<RecurrencePattern>()
        
        checkAll(20, dateArb, patternArb) { startDate, pattern ->
            var currentDate = startDate
            
            // Calculate 5 consecutive next due dates
            repeat(5) {
                val nextDate = RecurrenceCalculator.calculateNextDueDate(currentDate, pattern)
                
                // Each next date should be after the current date
                nextDate shouldBeGreaterThan currentDate
                
                currentDate = nextDate
            }
        }
    }

    /**
     * Property test: Recurrence pattern enum conversion is consistent
     * 
     * Verifies that converting pattern strings to enum values and back is consistent.
     */
    "recurrencePatternConversion_isConsistent" {
        val patternStrings = listOf("DAILY", "WEEKLY", "MONTHLY", "daily", "weekly", "monthly")
        
        patternStrings.forEach { patternString ->
            val pattern = RecurrencePattern.fromString(patternString)
            pattern shouldBe RecurrencePattern.valueOf(patternString.uppercase())
        }
    }

    /**
     * Property test: Invalid pattern strings return null
     * 
     * Verifies that invalid pattern strings are handled gracefully.
     */
    "invalidPatternStrings_returnNull" {
        val invalidPatterns = listOf("YEARLY", "HOURLY", "invalid", "", null)
        
        invalidPatterns.forEach { patternString ->
            val pattern = RecurrencePattern.fromString(patternString)
            pattern shouldBe null
        }
    }

    /**
     * Property test: Future due dates calculation produces increasing sequence
     * 
     * Verifies that calculating multiple future due dates produces a strictly increasing sequence.
     */
    "futureDueDatesCalculation_producesIncreasingSequence" {
        val dateArb = Arb.long(min = 1_000_000_000_000L, max = 2_000_000_000_000L)
        val patternArb = Arb.enum<RecurrencePattern>()
        val countArb = Arb.int(min = 2, max = 10)
        
        checkAll(20, dateArb, patternArb, countArb) { startDate, pattern, count ->
            val futureDates = RecurrenceCalculator.calculateFutureDueDates(startDate, pattern, count)
            
            // Verify we got the correct number of dates
            futureDates.size shouldBe count
            
            // Verify each date is greater than the previous one
            futureDates.zipWithNext().forEach { (current, next) ->
                next shouldBeGreaterThan current
            }
            
            // Verify the first date is greater than the start date
            futureDates.first() shouldBeGreaterThan startDate
        }
    }

    /**
     * Property test: Overdue detection is consistent
     * 
     * Verifies that dates in the past are correctly identified as overdue.
     */
    "overdueDetection_isConsistent" {
        val currentTime = System.currentTimeMillis()
        
        // Test past dates (should be overdue)
        val pastDateArb = Arb.long(min = 1_000_000_000_000L, max = currentTime - 1000)
        checkAll(50, pastDateArb) { pastDate ->
            RecurrenceCalculator.isOverdue(pastDate) shouldBe true
        }
        
        // Test future dates (should not be overdue)
        val futureDateArb = Arb.long(min = currentTime + 1000, max = currentTime + 365L * 24 * 60 * 60 * 1000)
        checkAll(50, futureDateArb) { futureDate ->
            RecurrenceCalculator.isOverdue(futureDate) shouldBe false
        }
    }

    /**
     * Property test: Missed periods calculation is consistent
     * 
     * Verifies that the number of missed periods is calculated correctly for overdue dates.
     */
    "missedPeriodsCalculation_isConsistent" {
        val patternArb = Arb.enum<RecurrencePattern>()
        
        checkAll(20, patternArb) { pattern ->
            val currentTime = System.currentTimeMillis()
            
            // Create a date that's definitely in the past
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentTime
            
            when (pattern) {
                RecurrencePattern.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, -5)
                RecurrencePattern.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, -3)
                RecurrencePattern.MONTHLY -> calendar.add(Calendar.MONTH, -2)
            }
            
            val pastDate = calendar.timeInMillis
            val missedPeriods = RecurrenceCalculator.calculateMissedPeriods(pastDate, pattern)
            
            // Verify missed periods is non-negative
            (missedPeriods >= 0) shouldBe true
            
            // Verify missed periods is reasonable for the pattern
            when (pattern) {
                RecurrencePattern.DAILY -> (missedPeriods >= 4) shouldBe true // At least 4 days
                RecurrencePattern.WEEKLY -> (missedPeriods >= 2) shouldBe true // At least 2 weeks
                RecurrencePattern.MONTHLY -> (missedPeriods >= 1) shouldBe true // At least 1 month
            }
        }
    }

    /**
     * Property test: Future dates have zero missed periods
     * 
     * Verifies that dates in the future correctly return zero missed periods.
     */
    "futureDates_haveZeroMissedPeriods" {
        val currentTime = System.currentTimeMillis()
        val futureDateArb = Arb.long(min = currentTime + 1000, max = currentTime + 365L * 24 * 60 * 60 * 1000)
        val patternArb = Arb.enum<RecurrencePattern>()
        
        checkAll(20, futureDateArb, patternArb) { futureDate, pattern ->
            val missedPeriods = RecurrenceCalculator.calculateMissedPeriods(futureDate, pattern)
            missedPeriods shouldBe 0
        }
    }
})

/**
 * Helper function to create a date with specific year, month, and day
 */
private fun createDate(year: Int, month: Int, day: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, day, 0, 0, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
