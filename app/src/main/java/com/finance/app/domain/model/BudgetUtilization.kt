package com.finance.app.domain.model

/**
 * Domain model representing budget utilization analytics for a specific category.
 * 
 * This model provides a comprehensive view of how much of a budget has been used,
 * including calculated fields for progress tracking and visual indicators.
 * 
 * @property categoryId The category this utilization data applies to
 * @property categoryName Display name of the category
 * @property budgetLimit The monthly spending limit for this category
 * @property currentSpending The amount spent so far in the current period
 * @property utilizationPercentage Percentage of budget used (0.0 to 100.0+)
 * @property remainingAmount Amount remaining in the budget (can be negative if over budget)
 * @property isOverBudget Whether spending has exceeded the budget limit
 */
data class BudgetUtilization(
    val categoryId: String,
    val categoryName: String,
    val budgetLimit: Double,
    val currentSpending: Double,
    val utilizationPercentage: Double,
    val remainingAmount: Double,
    val isOverBudget: Boolean
) {
    companion object {
        /**
         * Creates a BudgetUtilization instance from budget and spending data
         * @param categoryId The category ID
         * @param categoryName The category display name
         * @param budgetLimit The budget limit
         * @param currentSpending The current spending amount
         * @return BudgetUtilization instance with calculated fields
         */
        fun create(
            categoryId: String,
            categoryName: String,
            budgetLimit: Double,
            currentSpending: Double
        ): BudgetUtilization {
            val utilizationPercentage = if (budgetLimit > 0) {
                (currentSpending / budgetLimit) * 100.0
            } else {
                0.0
            }
            
            val remainingAmount = budgetLimit - currentSpending
            val isOverBudget = currentSpending > budgetLimit
            
            return BudgetUtilization(
                categoryId = categoryId,
                categoryName = categoryName,
                budgetLimit = budgetLimit,
                currentSpending = currentSpending,
                utilizationPercentage = utilizationPercentage,
                remainingAmount = remainingAmount,
                isOverBudget = isOverBudget
            )
        }
    }
    
    /**
     * Gets the alert level based on utilization percentage
     * @return AlertLevel indicating the severity of budget usage
     */
    fun getAlertLevel(): AlertLevel {
        return when {
            utilizationPercentage >= 100.0 -> AlertLevel.OVER_BUDGET
            utilizationPercentage >= 90.0 -> AlertLevel.CRITICAL
            utilizationPercentage >= 75.0 -> AlertLevel.WARNING
            else -> AlertLevel.NORMAL
        }
    }
}

/**
 * Enum representing different alert levels for budget utilization
 */
enum class AlertLevel {
    NORMAL,     // Under 75% utilization
    WARNING,    // 75-89% utilization
    CRITICAL,   // 90-99% utilization
    OVER_BUDGET // 100%+ utilization
}