package com.finance.app.data.repository

import com.finance.app.data.local.dao.BudgetDao
import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.mapper.toDomain
import com.finance.app.data.mapper.toEntity
import com.finance.app.data.remote.FirestoreDataSource
import com.finance.app.domain.model.Budget
import com.finance.app.domain.model.BudgetUtilization
import com.finance.app.domain.repository.BudgetRepository
import com.finance.app.domain.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

/**
 * Implementation of BudgetRepository using Room database and Firestore
 */
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncScheduler: SyncScheduler
) : BudgetRepository {

    override fun getBudgetsForUser(userId: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsForUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBudgetByCategory(userId: String, categoryId: String): Flow<Budget?> {
        return budgetDao.getBudgetByCategory(userId, categoryId).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getBudgetsForMonth(userId: String, month: Int, year: Int): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(userId, month, year).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBudgetUtilization(userId: String, month: Int, year: Int): Flow<List<BudgetUtilization>> {
        // Get budgets for the specified month/year
        val budgetsFlow = getBudgetsForMonth(userId, month, year)
        
        // Calculate date range for the month
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0) // month is 0-based in Calendar
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis
        
        // Get spending by category for the month
        val spendingFlow = transactionDao.getSpendingByCategory(userId, startDate, endDate)
        
        // Get all categories for name lookup
        val categoriesFlow = categoryDao.getAllCategoriesForUser(userId)
        
        // Combine budgets, spending data, and categories
        return combine(budgetsFlow, spendingFlow, categoriesFlow) { budgets, spendingList, categories ->
            budgets.map { budget ->
                // Find spending for this category
                val spending = spendingList.find { it.categoryId == budget.categoryId }
                val currentSpending = spending?.total ?: 0.0
                
                // Find category name
                val category = categories.find { it.id == budget.categoryId }
                val categoryName = category?.name ?: "Unknown Category"
                
                BudgetUtilization.create(
                    categoryId = budget.categoryId,
                    categoryName = categoryName,
                    budgetLimit = budget.monthlyLimit,
                    currentSpending = currentSpending
                )
            }
        }
    }

    override suspend fun setBudget(budget: Budget): Result<Unit> {
        return try {
            // Validate budget
            if (!budget.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid budget data"))
            }
            
            // Save to local database
            budgetDao.insertBudget(budget.toEntity())
            
            // Sync to remote storage
            val syncResult = firestoreDataSource.saveBudget(budget.userId, budget)
            if (syncResult.isFailure) {
                // If remote sync fails, schedule for later sync
                syncScheduler.schedulePostOperationSync()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBudget(userId: String, categoryId: String): Result<Unit> {
        return try {
            // Delete from local database
            budgetDao.deleteBudgetByCategory(userId, categoryId)
            
            // Find the budget ID for remote deletion
            val existingBudget = budgetDao.getBudgetById(generateBudgetId(userId, categoryId))
            existingBudget?.let { budget ->
                // Delete from remote storage
                val syncResult = firestoreDataSource.deleteBudget(userId, budget.id)
                if (syncResult.isFailure) {
                    // If remote sync fails, schedule for later sync
                    syncScheduler.schedulePostOperationSync()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Synchronize budgets from remote storage
     * This method is called by the sync system
     */
    suspend fun syncBudgetsFromRemote(userId: String): Result<Unit> {
        return try {
            val remoteBudgetsResult = firestoreDataSource.getBudgets(userId)
            if (remoteBudgetsResult.isSuccess) {
                val remoteBudgets = remoteBudgetsResult.getOrNull() ?: emptyList()
                
                // Insert/update local budgets with remote data
                val budgetEntities = remoteBudgets.map { it.toEntity() }
                budgetEntities.forEach { entity ->
                    budgetDao.insertBudget(entity)
                }
                
                Result.success(Unit)
            } else {
                Result.failure(remoteBudgetsResult.exceptionOrNull() ?: Exception("Failed to sync budgets"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Synchronize budgets to remote storage
     * This method is called by the sync system
     */
    suspend fun syncBudgetsToRemote(userId: String): Result<Unit> {
        return try {
            // TODO: Implement proper sync to remote when needed for user: $userId
            // This would involve collecting local budgets and syncing them to Firestore
            // For now, individual budget operations handle remote sync
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate a consistent budget ID based on user and category
     */
    private fun generateBudgetId(userId: String, categoryId: String): String {
        return "${userId}_${categoryId}_budget"
    }
}