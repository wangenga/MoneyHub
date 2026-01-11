package com.finance.app.domain.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Budget domain model validation
 */
class BudgetTest {

    @Test
    fun `isValidBudgetAmount should return true for positive amounts within limits`() {
        assertTrue(Budget.isValidBudgetAmount(100.0))
        assertTrue(Budget.isValidBudgetAmount(1000.0))
        assertTrue(Budget.isValidBudgetAmount(999999.0))
    }

    @Test
    fun `isValidBudgetAmount should return false for invalid amounts`() {
        assertFalse(Budget.isValidBudgetAmount(0.0))
        assertFalse(Budget.isValidBudgetAmount(-100.0))
        assertFalse(Budget.isValidBudgetAmount(1_000_001.0))
    }

    @Test
    fun `isValidMonth should return true for valid months`() {
        for (month in 1..12) {
            assertTrue("Month $month should be valid", Budget.isValidMonth(month))
        }
    }

    @Test
    fun `isValidMonth should return false for invalid months`() {
        assertFalse(Budget.isValidMonth(0))
        assertFalse(Budget.isValidMonth(13))
        assertFalse(Budget.isValidMonth(-1))
    }

    @Test
    fun `isValidYear should return true for reasonable years`() {
        assertTrue(Budget.isValidYear(2024))
        assertTrue(Budget.isValidYear(2000))
        assertTrue(Budget.isValidYear(2100))
    }

    @Test
    fun `isValidYear should return false for unreasonable years`() {
        assertFalse(Budget.isValidYear(1999))
        assertFalse(Budget.isValidYear(2101))
    }

    @Test
    fun `isValid should return true for valid budget`() {
        val budget = Budget(
            id = "budget-1",
            userId = "user-1",
            categoryId = "category-1",
            monthlyLimit = 500.0,
            month = 6,
            year = 2024,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        assertTrue(budget.isValid())
    }

    @Test
    fun `isValid should return false for budget with invalid amount`() {
        val budget = Budget(
            id = "budget-1",
            userId = "user-1",
            categoryId = "category-1",
            monthlyLimit = -100.0, // Invalid amount
            month = 6,
            year = 2024,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        assertFalse(budget.isValid())
    }

    @Test
    fun `isValid should return false for budget with blank fields`() {
        val budget = Budget(
            id = "", // Blank ID
            userId = "user-1",
            categoryId = "category-1",
            monthlyLimit = 500.0,
            month = 6,
            year = 2024,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        assertFalse(budget.isValid())
    }
}