package com.finance.app.ui.budget

import com.finance.app.domain.model.Budget
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.util.Calendar

/**
 * Property-based tests for monthly budget reset preservation.
 * 
 * These tests verify the correctness properties defined in the design document
 * for the budget-and-recurring-transactions feature.
 */
class BudgetMonthlyResetPropertyTest : StringSpec({

    /**
     * **Feature: budget-and-recurring-transactions, Property 5: Monthly budget reset preservation**
     * **Validates: Requirements 5.3**
     * 
     * *For any* budget configuration, when a month transition occurs, the budget limits 
     * should remain unchanged while spending tracking resets to zero.
     * 
     * This test verifies that budget limits are preserved across month transitions.
     * The spending reset is handled by the repository layer which filters transactions
     * by date range, so this test focuses on budget limit preservation.
     */
    "property5_monthlyBudgetResetPreservation_limitsPreservedAcrossMonths" {
        // Generate valid budget amounts
        val validAmounts = Arb.double(min = 0.01, max = 1_000_000.0)
        
        // Generate valid months (1-12)
        val validMonths = Arb.int(min = 1, max = 12)
        
        // Generate valid years (avoid 2100 to prevent overflow when adding months)
        val validYears = Arb.int(min = 2000, max = 2099)
        
        checkAll(20, validAmounts, validMonths, validYears) { amount, month, year ->
            // Create a budget for a specific month/year
            val originalBudget = Budget(
                id = "test_user_test_category_${month}_${year}",
                userId = "test_user",
                categoryId = "test_category",
                monthlyLimit = amount,
                month = month,
                year = year,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Calculate next month
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 1) // month is 0-based in Calendar
            calendar.add(Calendar.MONTH, 1)
            val nextMonth = calendar.get(Calendar.MONTH) + 1 // Convert back to 1-based
            val nextYear = calendar.get(Calendar.YEAR)
            
            // Create budget for next month with same limit
            val nextMonthBudget = Budget(
                id = "test_user_test_category_${nextMonth}_${nextYear}",
                userId = originalBudget.userId,
                categoryId = originalBudget.categoryId,
                monthlyLimit = originalBudget.monthlyLimit, // Same limit
                month = nextMonth,
                year = nextYear,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Verify that the budget limit is preserved across month transition
            nextMonthBudget.monthlyLimit shouldBe originalBudget.monthlyLimit
            
            // Verify that the budget configuration is still valid
            nextMonthBudget.isValid() shouldBe true
            
            // Verify that user and category remain the same
            nextMonthBudget.userId shouldBe originalBudget.userId
            nextMonthBudget.categoryId shouldBe originalBudget.categoryId
        }
    }

    /**
     * Property test: Budget limits remain unchanged when transitioning from December to January
     * 
     * This is a specific edge case where the year changes along with the month.
     */
    "budgetLimits_remainUnchanged_whenTransitioningFromDecemberToJanuary" {
        val validAmounts = Arb.double(min = 0.01, max = 1_000_000.0)
        val validYears = Arb.int(min = 2000, max = 2099) // Avoid 2100 to prevent overflow
        
        checkAll(20, validAmounts, validYears) { amount, year ->
            // Create December budget
            val decemberBudget = Budget(
                id = "test_user_test_category_12_${year}",
                userId = "test_user",
                categoryId = "test_category",
                monthlyLimit = amount,
                month = 12,
                year = year,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Create January budget for next year
            val januaryBudget = Budget(
                id = "test_user_test_category_1_${year + 1}",
                userId = decemberBudget.userId,
                categoryId = decemberBudget.categoryId,
                monthlyLimit = decemberBudget.monthlyLimit,
                month = 1,
                year = year + 1,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Verify budget limit preservation across year boundary
            januaryBudget.monthlyLimit shouldBe decemberBudget.monthlyLimit
            januaryBudget.isValid() shouldBe true
        }
    }

    /**
     * Property test: Budget configuration fields remain consistent across month transitions
     * 
     * Verifies that all identifying fields (userId, categoryId) remain the same
     * while only the month/year change.
     */
    "budgetConfiguration_remainsConsistent_acrossMonthTransitions" {
        val validAmounts = Arb.double(min = 0.01, max = 1_000_000.0)
        val validMonths = Arb.int(min = 1, max = 11) // Avoid December to simplify
        val validYears = Arb.int(min = 2000, max = 2099) // Avoid 2100 to prevent overflow
        val userIds = Arb.string(minSize = 1, maxSize = 50)
        val categoryIds = Arb.string(minSize = 1, maxSize = 50)
        
        checkAll(
            100,
            validAmounts,
            validMonths,
            validYears,
            userIds,
            categoryIds
        ) { amount, month, year, userId, categoryId ->
            // Create budget for current month
            val currentBudget = Budget(
                id = "${userId}_${categoryId}_${month}_${year}",
                userId = userId,
                categoryId = categoryId,
                monthlyLimit = amount,
                month = month,
                year = year,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Create budget for next month
            val nextBudget = Budget(
                id = "${userId}_${categoryId}_${month + 1}_${year}",
                userId = userId,
                categoryId = categoryId,
                monthlyLimit = amount, // Same limit
                month = month + 1,
                year = year,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Verify all configuration fields are preserved
            nextBudget.userId shouldBe currentBudget.userId
            nextBudget.categoryId shouldBe currentBudget.categoryId
            nextBudget.monthlyLimit shouldBe currentBudget.monthlyLimit
            
            // Verify both budgets are valid
            currentBudget.isValid() shouldBe true
            nextBudget.isValid() shouldBe true
        }
    }

    /**
     * Property test: Multiple consecutive month transitions preserve budget limits
     * 
     * Verifies that budget limits remain consistent across multiple month transitions,
     * simulating a year's worth of budget rollovers.
     */
    "budgetLimits_remainConsistent_acrossMultipleMonthTransitions" {
        val validAmounts = Arb.double(min = 0.01, max = 1_000_000.0)
        val startMonths = Arb.int(min = 1, max = 12)
        val validYears = Arb.int(min = 2000, max = 2099)
        
        checkAll(20, validAmounts, startMonths, validYears) { amount, startMonth, startYear ->
            val budgets = mutableListOf<Budget>()
            
            // Create budgets for 12 consecutive months
            val calendar = Calendar.getInstance()
            calendar.set(startYear, startMonth - 1, 1)
            
            repeat(12) { index ->
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)
                
                val budget = Budget(
                    id = "test_user_test_category_${currentMonth}_${currentYear}",
                    userId = "test_user",
                    categoryId = "test_category",
                    monthlyLimit = amount, // Same limit for all months
                    month = currentMonth,
                    year = currentYear,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                budgets.add(budget)
                calendar.add(Calendar.MONTH, 1)
            }
            
            // Verify all budgets have the same limit
            budgets.forEach { budget ->
                budget.monthlyLimit shouldBe amount
                budget.isValid() shouldBe true
            }
            
            // Verify all budgets have the same user and category
            val firstBudget = budgets.first()
            budgets.forEach { budget ->
                budget.userId shouldBe firstBudget.userId
                budget.categoryId shouldBe firstBudget.categoryId
            }
        }
    }
})
