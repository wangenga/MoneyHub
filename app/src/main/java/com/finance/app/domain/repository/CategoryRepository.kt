package com.finance.app.domain.repository

import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for category operations
 */
interface CategoryRepository {
    fun getAllCategories(userId: String): Flow<List<Category>>
    fun getCategoryById(id: String): Flow<Category?>
    fun getCategoriesByType(type: CategoryType, userId: String): Flow<List<Category>>
    fun getExpenseCategories(userId: String): Flow<List<Category>>
    fun getIncomeCategories(userId: String): Flow<List<Category>>
    suspend fun insertCategory(category: Category): Result<Unit>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(id: String): Result<Unit>
    fun getDefaultCategories(): List<Category>
    suspend fun initializeDefaultCategories(): Result<Unit>
}
