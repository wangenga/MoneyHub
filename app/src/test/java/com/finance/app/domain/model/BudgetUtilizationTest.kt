package com.finance.app.domain.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for BudgetUtilization domain model
 */
class BudgetUtilizationTest {

    @Test
    fun `create should calculate utilization percentage correctly`() {
        val utilization = BudgetUtilization.create(
            categoryId = "cat-1",
            categoryName = "Food",
            budgetLimit = 1000.0,
            currentSpending = 750.0
        )
        
        assertEquals(75.0, utilization.utilizationPercentage, 0.01)
        assertEquals(250.0, utilization.remainingAmount, 0.01)
        assertFalse(utilization.isOverBudget)
    }

    @Test
    fun `create should handle over-budget scenario`() {
        val utilization = BudgetUtilization.create(
            categoryId = "cat-1",
            categoryName = "Food",
            budgetLimit = 1000.0,
            currentSpending = 1200.0
        )
        
        assertEquals(120.0, utilization.utilizationPercentage, 0.01)
        assertEquals(-200.0, utilization.remainingAmount, 0.01)
        assertTrue(utilization.isOverBudget)
    }

    @Test
    fun `create should handle zero budget limit`() {
        val utilization = BudgetUtilization.create(
            categoryId = "cat-1",
            categoryName = "Food",
            budgetLimit = 0.0,
            currentSpending = 100.0
        )
        
        assertEquals(0.0, utilization.utilizationPercentage, 0.01)
        assertEquals(-100.0, utilization.remainingAmount, 0.01)
        assertTrue(utilization.isOverBudget)
    }

    @Test
    fun `getAlertLevel should return correct alert levels`() {
        // Normal level (under 75%)
        val normal = BudgetUtilization.create("cat-1", "Food", 1000.0, 500.0)
        assertEquals(AlertLevel.NORMAL, normal.getAlertLevel())
        
        // Warning level (75-89%)
        val warning = BudgetUtilization.create("cat-1", "Food", 1000.0, 800.0)
        assertEquals(AlertLevel.WARNING, warning.getAlertLevel())
        
        // Critical level (90-99%)
        val critical = BudgetUtilization.create("cat-1", "Food", 1000.0, 950.0)
        assertEquals(AlertLevel.CRITICAL, critical.getAlertLevel())
        
        // Over budget (100%+)
        val overBudget = BudgetUtilization.create("cat-1", "Food", 1000.0, 1100.0)
        assertEquals(AlertLevel.OVER_BUDGET, overBudget.getAlertLevel())
    }

    @Test
    fun `getAlertLevel should handle exact threshold values`() {
        // Exactly 75%
        val warning75 = BudgetUtilization.create("cat-1", "Food", 1000.0, 750.0)
        assertEquals(AlertLevel.WARNING, warning75.getAlertLevel())
        
        // Exactly 90%
        val critical90 = BudgetUtilization.create("cat-1", "Food", 1000.0, 900.0)
        assertEquals(AlertLevel.CRITICAL, critical90.getAlertLevel())
        
        // Exactly 100%
        val overBudget100 = BudgetUtilization.create("cat-1", "Food", 1000.0, 1000.0)
        assertEquals(AlertLevel.OVER_BUDGET, overBudget100.getAlertLevel())
    }
}