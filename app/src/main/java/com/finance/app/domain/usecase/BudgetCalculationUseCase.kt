package com.finance.app.domain.usecase

import com.finance.app.domain.model.Budget
import com.finance.app.domain.model.BudgetUtilization
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType
import com.finance.app.domain.repository.BudgetRepository
import com.finance.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject

/**
 * Use case for calculating budget utilization and spending progress
 */
class BudgetCalculationUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    
    /**
     * Calculate budget utilization for all categories in a specific month
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of budget utilization list
     */
    fun calculateBudgetUtilization(
        userId: String,
        month: Int,
        year: Int
    ): Flow<List<BudgetUtilization>> {
        val budgetsFlow = budgetRepository.getBudgetsForMonth(userId, month, year)
        val transactionsFlow = getTransactionsForMonth(userId, month, year)
        
        return combine(budgetsFlow, transactionsFlow) { budgets, transactions ->
            budgets.map { budget ->
                val categorySpending = calculateCategorySpending(budget.categoryId, transactions)
                BudgetUtilization.create(
                    categoryId = budget.categoryId,
                    categoryName = "", // Will be populated by the repository layer
                    budgetLimit = budget.monthlyLimit,
                    currentSpending = categorySpending
                )
            }
        }
    }
    
    /**
     * Calculate spending for a specific category
     * @param categoryId The category ID
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of current spending amount
     */
    fun calculateCategorySpending(
        categoryId: String,
        userId: String,
        month: Int,
        year: Int
    ): Flow<Double> {
        return getTransactionsForMonth(userId, month, year).map { transactions ->
            calculateCategorySpending(categoryId, transactions)
        }
    }
    
    /**
     * Calculate budget progress percentage for a category
     * @param categoryId The category ID
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of progress percentage (0.0 to 100.0+)
     */
    fun calculateBudgetProgress(
        categoryId: String,
        userId: String,
        month: Int,
        year: Int
    ): Flow<Double> {
        val budgetFlow = budgetRepository.getBudgetByCategory(userId, categoryId)
        val spendingFlow = calculateCategorySpending(categoryId, userId, month, year)
        
        return combine(budgetFlow, spendingFlow) { budget, spending ->
            if (budget != null && budget.monthlyLimit > 0) {
                (spending / budget.monthlyLimit) * 100.0
            } else {
                0.0
            }
        }
    }
    
    /**
     * Check if a category is over budget
     * @param categoryId The category ID
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of boolean indicating if over budget
     */
    fun isOverBudget(
        categoryId: String,
        userId: String,
        month: Int,
        year: Int
    ): Flow<Boolean> {
        return calculateBudgetProgress(categoryId, userId, month, year).map { progress ->
            progress >= 100.0
        }
    }
    
    /**
     * Get remaining budget amount for a category
     * @param categoryId The category ID
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of remaining budget amount (can be negative if over budget)
     */
    fun getRemainingBudget(
        categoryId: String,
        userId: String,
        month: Int,
        year: Int
    ): Flow<Double> {
        val budgetFlow = budgetRepository.getBudgetByCategory(userId, categoryId)
        val spendingFlow = calculateCategorySpending(categoryId, userId, month, year)
        
        return combine(budgetFlow, spendingFlow) { budget, spending ->
            if (budget != null) {
                budget.monthlyLimit - spending
            } else {
                0.0
            }
        }
    }
    
    /**
     * Get transactions for a specific month and year
     */
    private fun getTransactionsForMonth(
        userId: String,
        month: Int,
        year: Int
    ): Flow<List<Transaction>> {
        val calendar = Calendar.getInstance()
        
        // Start of month
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        // End of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis
        
        return transactionRepository.getTransactionsByDateRange(startOfMonth, endOfMonth)
            .map { transactions ->
                transactions.filter { it.userId == userId }
            }
    }
    
    /**
     * Calculate total spending for a specific category from a list of transactions
     */
    private fun calculateCategorySpending(
        categoryId: String,
        transactions: List<Transaction>
    ): Double {
        return transactions
            .filter { it.categoryId == categoryId && it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }
}