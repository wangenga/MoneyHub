package com.finance.app.data.local.dao

import androidx.room.*
import com.finance.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for budget operations in Room database
 */
@Dao
interface BudgetDao {
    
    /**
     * Get all budgets for a specific user
     * @param userId The user ID to filter by
     * @return Flow of budget list for reactive updates
     */
    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY year DESC, month DESC")
    fun getBudgetsForUser(userId: String): Flow<List<BudgetEntity>>
    
    /**
     * Get budget for a specific category and user
     * @param userId The user ID
     * @param categoryId The category ID
     * @return Flow of budget entity or null if not found
     */
    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId LIMIT 1")
    fun getBudgetByCategory(userId: String, categoryId: String): Flow<BudgetEntity?>
    
    /**
     * Get budgets for a specific month and year
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of budget list for the specified period
     */
    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month AND year = :year")
    fun getBudgetsForMonth(userId: String, month: Int, year: Int): Flow<List<BudgetEntity>>
    
    /**
     * Get budget by ID
     * @param budgetId The budget ID
     * @return Budget entity or null if not found
     */
    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    suspend fun getBudgetById(budgetId: String): BudgetEntity?
    
    /**
     * Insert a new budget
     * @param budget The budget entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)
    
    /**
     * Update an existing budget
     * @param budget The budget entity to update
     */
    @Update
    suspend fun updateBudget(budget: BudgetEntity)
    
    /**
     * Delete a budget by ID
     * @param budgetId The budget ID to delete
     */
    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudgetById(budgetId: String)
    
    /**
     * Delete budget for a specific category and user
     * @param userId The user ID
     * @param categoryId The category ID
     */
    @Query("DELETE FROM budgets WHERE userId = :userId AND categoryId = :categoryId")
    suspend fun deleteBudgetByCategory(userId: String, categoryId: String)
    
    /**
     * Delete all budgets for a user
     * @param userId The user ID
     */
    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteAllBudgetsForUser(userId: String)
    
    /**
     * Check if a budget exists for a category and user
     * @param userId The user ID
     * @param categoryId The category ID
     * @return True if budget exists, false otherwise
     */
    @Query("SELECT COUNT(*) > 0 FROM budgets WHERE userId = :userId AND categoryId = :categoryId")
    suspend fun budgetExistsForCategory(userId: String, categoryId: String): Boolean
}