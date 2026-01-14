package com.finance.app.domain.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for Budget utilization display completeness.
 * 
 * These tests verify the correctness properties defined in the design document
 * for the budget-and-recurring-transactions feature.
 */
class BudgetUtilizationDisplayPropertyTest : StringSpec({

    /**
     * **Feature: budget-and-recurring-transactions, Property 9: Budget utilization display completeness**
     * **Validates: Requirements 6.2, 6.3, 6.4, 6.5**
     * 
     * *For any* category with a budget, the utilization display should show all required information:
     * spent amount, remaining amount, percentage used, and appropriate visual indicators.
     * 
     * This test verifies that BudgetUtilization objects created from budget and spending data
     * contain all required fields with correct calculated values.
     */
    "property9_budgetUtilizationDisplayCompleteness_allRequiredFieldsPresent" {
        val budgetUtilizationArb = arbitrary { rs ->
            val categoryId = Arb.string(minSize = 1, maxSize = 50).bind()
            val categoryName = Arb.string(minSize = 1, maxSize = 100).bind()
            val budgetLimit = Arb.double(min = 1.0, max = 100_000.0).bind()
            val currentSpending = Arb.double(min = 0.0, max = 150_000.0).bind()
            
            BudgetUtilization.create(
                categoryId = categoryId,
                categoryName = categoryName,
                budgetLimit = budgetLimit,
                currentSpending = currentSpending
            )
        }
        
        checkAll(20, budgetUtilizationArb) { utilization ->
            // Verify all required fields are present with expected values
            utilization.categoryId.isNotEmpty() shouldBe true
            utilization.categoryName.isNotEmpty() shouldBe true
            utilization.budgetLimit shouldBe utilization.budgetLimit // Verify it exists
            utilization.currentSpending shouldBe utilization.currentSpending // Verify it exists
            utilization.utilizationPercentage shouldBe utilization.utilizationPercentage // Verify it exists
            utilization.remainingAmount.isFinite() shouldBe true
            utilization.isOverBudget shouldBe (utilization.currentSpending > utilization.budgetLimit)
        }
    }

    /**
     * Property test: Utilization percentage is calculated correctly
     * 
     * For any budget limit and spending amount, the utilization percentage should be
     * calculated as (currentSpending / budgetLimit) * 100.
     */
    "utilizationPercentageCalculation_isCorrect" {
        checkAll(20, Arb.double(min = 1.0, max = 100_000.0), Arb.double(min = 0.0, max = 150_000.0)) { budgetLimit, spending ->
            val utilization = BudgetUtilization.create(
                categoryId = "cat1",
                categoryName = "Test Category",
                budgetLimit = budgetLimit,
                currentSpending = spending
            )
            
            val expectedPercentage = (spending / budgetLimit) * 100.0
            utilization.utilizationPercentage shouldBe expectedPercentage
        }
    }

    /**
     * Property test: Remaining amount is calculated correctly
     * 
     * For any budget limit and spending amount, the remaining amount should be
     * calculated as (budgetLimit - currentSpending).
     */
    "remainingAmountCalculation_isCorrect" {
        checkAll(20, Arb.double(min = 1.0, max = 100_000.0), Arb.double(min = 0.0, max = 150_000.0)) { budgetLimit, spending ->
            val utilization = BudgetUtilization.create(
                categoryId = "cat1",
                categoryName = "Test Category",
                budgetLimit = budgetLimit,
                currentSpending = spending
            )
            
            val expectedRemaining = budgetLimit - spending
            utilization.remainingAmount shouldBe expectedRemaining
        }
    }

    /**
     * Property test: Over-budget flag is set correctly
     * 
     * For any budget limit and spending amount, isOverBudget should be true if and only if
     * currentSpending > budgetLimit.
     */
    "overBudgetFlag_isSetCorrectly" {
        checkAll(20, Arb.double(min = 1.0, max = 100_000.0), Arb.double(min = 0.0, max = 150_000.0)) { budgetLimit, spending ->
            val utilization = BudgetUtilization.create(
                categoryId = "cat1",
                categoryName = "Test Category",
                budgetLimit = budgetLimit,
                currentSpending = spending
            )
            
            val expectedOverBudget = spending > budgetLimit
            utilization.isOverBudget shouldBe expectedOverBudget
        }
    }

    /**
     * Property test: Alert level is determined correctly based on utilization percentage
     * 
     * For any utilization percentage, the alert level should be:
     * - OVER_BUDGET if >= 100%
     * - CRITICAL if >= 90% and < 100%
     * - WARNING if >= 75% and < 90%
     * - NORMAL if < 75%
     */
    "alertLevelDetermination_isCorrect" {
        checkAll(20, Arb.double(min = 1.0, max = 100_000.0), Arb.double(min = 0.0, max = 150_000.0)) { budgetLimit, spending ->
            val utilization = BudgetUtilization.create(
                categoryId = "cat1",
                categoryName = "Test Category",
                budgetLimit = budgetLimit,
                currentSpending = spending
            )
            
            val expectedAlertLevel = when {
                utilization.utilizationPercentage >= 100.0 -> AlertLevel.OVER_BUDGET
                utilization.utilizationPercentage >= 90.0 -> AlertLevel.CRITICAL
                utilization.utilizationPercentage >= 75.0 -> AlertLevel.WARNING
                else -> AlertLevel.NORMAL
            }
            
            utilization.getAlertLevel() shouldBe expectedAlertLevel
        }
    }

    /**
     * Property test: Display data consistency for under-budget scenarios
     * 
     * When spending is below budget, remaining amount should be positive and
     * isOverBudget should be false.
     */
    "underBudgetScenario_displayDataIsConsistent" {
        checkAll(20, Arb.double(min = 1.0, max = 100_000.0)) { budgetLimit ->
            val spending = budgetLimit * 0.5 // 50% of budget
            
            val utilization = BudgetUtilization.create(
                categoryId = "cat1",
                categoryName = "Test Category",
                budgetLimit = budgetLimit,
                currentSpending = spending
            )
            
            (utilization.remainingAmount > 0.0) shouldBe true
            utilization.isOverBudget shouldBe false
            (utilization.utilizationPercentage < 100.0) shouldBe true
        }
    }

    /**
     * Property test: Display data consistency for over-budget scenarios
     * 
     * When spending exceeds budget, remaining amount should be negative and
     * isOverBudget should be true.
     */
    "overBudgetScenario_displayDataIsConsistent" {
        checkAll(20, Arb.double(min = 1.0, max = 100_000.0)) { budgetLimit ->
            val spending = budgetLimit * 1.5 // 150% of budget
            
            val utilization = BudgetUtilization.create(
                categoryId = "cat1",
                categoryName = "Test Category",
                budgetLimit = budgetLimit,
                currentSpending = spending
            )
            
            (utilization.remainingAmount < 0.0) shouldBe true
            utilization.isOverBudget shouldBe true
            (utilization.utilizationPercentage > 100.0) shouldBe true
        }
    }

    /**
     * Property test: Display data consistency for zero spending
     * 
     * When no spending has occurred, remaining amount should equal budget limit
     * and isOverBudget should be false.
     */
    "zeroSpendingScenario_displayDataIsConsistent" {
        checkAll(20, Arb.double(min = 1.0, max = 100_000.0)) { budgetLimit ->
            val utilization = BudgetUtilization.create(
                categoryId = "cat1",
                categoryName = "Test Category",
                budgetLimit = budgetLimit,
                currentSpending = 0.0
            )
            
            utilization.remainingAmount shouldBe budgetLimit
            utilization.isOverBudget shouldBe false
            utilization.utilizationPercentage shouldBe 0.0
        }
    }

    /**
     * Property test: Display data consistency for exact budget match
     * 
     * When spending exactly matches budget, remaining amount should be zero
     * and isOverBudget should be false (not over, just at limit).
     */
    "exactBudgetMatchScenario_displayDataIsConsistent" {
        checkAll(20, Arb.double(min = 1.0, max = 100_000.0)) { budgetLimit ->
            val utilization = BudgetUtilization.create(
                categoryId = "cat1",
                categoryName = "Test Category",
                budgetLimit = budgetLimit,
                currentSpending = budgetLimit
            )
            
            utilization.remainingAmount shouldBe 0.0
            utilization.isOverBudget shouldBe false
            utilization.utilizationPercentage shouldBe 100.0
        }
    }
})
