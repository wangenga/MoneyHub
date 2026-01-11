package com.finance.app.domain.model

/**
 * Domain model representing a monthly budget for a specific category.
 * 
 * Budgets allow users to set spending limits for categories and track their progress
 * throughout the month. Each budget is tied to a specific user, category, and month/year.
 * 
 * @property id Unique identifier for the budget
 * @property userId Owner's user ID
 * @property categoryId The category this budget applies to
 * @property monthlyLimit The spending limit for this category in the specified month
 * @property month The month this budget applies to (1-12)
 * @property year The year this budget applies to
 * @property createdAt Timestamp when the budget was created
 * @property updatedAt Timestamp when the budget was last updated
 */
data class Budget(
    val id: String,
    val userId: String,
    val categoryId: String,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        /**
         * Validates a budget amount according to business rules
         * @param amount The budget amount to validate
         * @return true if the amount is valid, false otherwise
         */
        fun isValidBudgetAmount(amount: Double): Boolean {
            return amount > 0.0 && amount <= 1_000_000.0 // Reasonable upper limit
        }
        
        /**
         * Validates month value
         * @param month The month to validate (1-12)
         * @return true if the month is valid, false otherwise
         */
        fun isValidMonth(month: Int): Boolean {
            return month in 1..12
        }
        
        /**
         * Validates year value
         * @param year The year to validate
         * @return true if the year is valid, false otherwise
         */
        fun isValidYear(year: Int): Boolean {
            return year >= 2000 && year <= 2100 // Reasonable range
        }
    }
    
    /**
     * Validates this budget instance
     * @return true if all fields are valid, false otherwise
     */
    fun isValid(): Boolean {
        return id.isNotBlank() &&
                userId.isNotBlank() &&
                categoryId.isNotBlank() &&
                isValidBudgetAmount(monthlyLimit) &&
                isValidMonth(month) &&
                isValidYear(year) &&
                createdAt > 0 &&
                updatedAt > 0
    }
}