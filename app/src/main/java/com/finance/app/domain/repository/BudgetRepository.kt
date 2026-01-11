package com.finance.app.domain.repository

import com.finance.app.domain.model.Budget
import com.finance.app.domain.model.BudgetUtilization
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for budget operations
 */
interface BudgetRepository {
    /**
     * Get all budgets for a specific user
     * @param userId The user ID to filter by
     * @return Flow of budget list for reactive updates
     */
    fun getBudgetsForUser(userId: String): Flow<List<Budget>>
    
    /**
     * Get budget for a specific category and user
     * @param userId The user ID
     * @param categoryId The category ID
     * @return Flow of budget or null if not found
     */
    fun getBudgetByCategory(userId: String, categoryId: String): Flow<Budget?>
    
    /**
     * Get budget utilization data for a specific month and year
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of budget utilization list
     */
    fun getBudgetUtilization(userId: String, month: Int, year: Int): Flow<List<BudgetUtilization>>
    
    /**
     * Set or update a budget for a category
     * @param budget The budget to save
     * @return Result indicating success or failure
     */
    suspend fun setBudget(budget: Budget): Result<Unit>
    
    /**
     * Delete a budget for a specific category
     * @param userId The user ID
     * @param categoryId The category ID
     * @return Result indicating success or failure
     */
    suspend fun deleteBudget(userId: String, categoryId: String): Result<Unit>
    
    /**
     * Get budgets for a specific month and year
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of budget list for the specified period
     */
    fun getBudgetsForMonth(userId: String, month: Int, year: Int): Flow<List<Budget>>
}