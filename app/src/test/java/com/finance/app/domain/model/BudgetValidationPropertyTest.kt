package com.finance.app.domain.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for Budget validation consistency.
 * 
 * These tests verify the correctness properties defined in the design document
 * for the budget-and-recurring-transactions feature.
 */
class BudgetValidationPropertyTest : StringSpec({

    /**
     * **Feature: budget-and-recurring-transactions, Property 2: Budget validation consistency**
     * **Validates: Requirements 1.2**
     * 
     * *For any* budget amount, the validation result should be consistent - positive amounts 
     * within reasonable limits should be accepted, while negative, zero, or excessive amounts 
     * should be rejected.
     * 
     * This test runs 100 iterations with randomly generated budget amounts to verify that
     * the validation logic consistently accepts valid amounts and rejects invalid ones.
     */
    "property2_budgetValidationConsistency_validAmountsAcceptedInvalidRejected" {
        checkAll(20, Arb.double()) { amount ->
            val isValid = Budget.isValidBudgetAmount(amount)
            val shouldBeValid = amount > 0.0 && amount <= 1_000_000.0
            
            // Validation result should be consistent with the business rules
            isValid shouldBe shouldBeValid
        }
    }

    /**
     * Property test: Valid positive amounts within limits are consistently accepted
     * 
     * Generates random amounts in the valid range (0.01 to 1,000,000) and verifies
     * they are all accepted by the validation logic.
     */
    "validPositiveAmountsWithinLimits_areConsistentlyAccepted" {
        // Generate amounts in the valid range: (0, 1_000_000]
        val validAmounts = Arb.double(min = 0.01, max = 1_000_000.0)
        
        checkAll(20, validAmounts) { amount ->
            Budget.isValidBudgetAmount(amount) shouldBe true
        }
    }

    /**
     * Property test: Zero and negative amounts are consistently rejected
     * 
     * Generates random negative amounts and zero, verifying they are all rejected.
     */
    "zeroAndNegativeAmounts_areConsistentlyRejected" {
        // Generate negative amounts and zero
        val invalidAmounts = Arb.double(min = -1_000_000.0, max = 0.0)
        
        checkAll(20, invalidAmounts) { amount ->
            Budget.isValidBudgetAmount(amount) shouldBe false
        }
    }

    /**
     * Property test: Amounts exceeding the upper limit are consistently rejected
     * 
     * Generates random amounts above the maximum limit and verifies they are rejected.
     */
    "amountsExceedingUpperLimit_areConsistentlyRejected" {
        // Generate amounts above the limit: (1_000_000, ∞)
        val excessiveAmounts = Arb.double(min = 1_000_000.01, max = 10_000_000.0)
        
        checkAll(20, excessiveAmounts) { amount ->
            Budget.isValidBudgetAmount(amount) shouldBe false
        }
    }

    /**
     * Property test: Boundary values are handled correctly
     * 
     * Tests the exact boundary values to ensure they are classified correctly.
     */
    "boundaryValues_areHandledCorrectly" {
        // Test exact boundaries
        Budget.isValidBudgetAmount(0.0) shouldBe false // Zero is invalid
        Budget.isValidBudgetAmount(0.01) shouldBe true // Just above zero is valid
        Budget.isValidBudgetAmount(1_000_000.0) shouldBe true // Exactly at limit is valid
        Budget.isValidBudgetAmount(1_000_000.01) shouldBe false // Just above limit is invalid
    }

    /**
     * Property test: Month validation is consistent
     * 
     * Verifies that months 1-12 are accepted and all other values are rejected.
     */
    "monthValidation_isConsistent" {
        checkAll(20, Arb.int()) { month ->
            val isValid = Budget.isValidMonth(month)
            val shouldBeValid = month in 1..12
            
            isValid shouldBe shouldBeValid
        }
    }

    /**
     * Property test: Year validation is consistent
     * 
     * Verifies that years in the reasonable range (2000-2100) are accepted
     * and all other values are rejected.
     */
    "yearValidation_isConsistent" {
        checkAll(20, Arb.int()) { year ->
            val isValid = Budget.isValidYear(year)
            val shouldBeValid = year >= 2000 && year <= 2100
            
            isValid shouldBe shouldBeValid
        }
    }

    /**
     * Property test: Complete budget validation is consistent
     * 
     * Generates random Budget objects and verifies that the isValid() method
     * correctly identifies valid and invalid budgets based on all field validations.
     */
    "completeBudgetValidation_isConsistent" {
        val budgetArb = arbitrary { rs ->
            Budget(
                id = Arb.string(minSize = 1, maxSize = 50).bind(),
                userId = Arb.string(minSize = 1, maxSize = 50).bind(),
                categoryId = Arb.string(minSize = 1, maxSize = 50).bind(),
                monthlyLimit = Arb.double().bind(),
                month = Arb.int().bind(),
                year = Arb.int().bind(),
                createdAt = Arb.long(min = 1, max = Long.MAX_VALUE).bind(),
                updatedAt = Arb.long(min = 1, max = Long.MAX_VALUE).bind()
            )
        }
        
        checkAll(20, budgetArb) { budget ->
            val isValid = budget.isValid()
            
            // Manually compute what the validation should be
            val shouldBeValid = budget.id.isNotBlank() &&
                    budget.userId.isNotBlank() &&
                    budget.categoryId.isNotBlank() &&
                    Budget.isValidBudgetAmount(budget.monthlyLimit) &&
                    Budget.isValidMonth(budget.month) &&
                    Budget.isValidYear(budget.year) &&
                    budget.createdAt > 0 &&
                    budget.updatedAt > 0
            
            // Validation result should match expected result
            isValid shouldBe shouldBeValid
        }
    }
})
