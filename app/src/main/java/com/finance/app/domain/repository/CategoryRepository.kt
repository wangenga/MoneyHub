package com.finance.app.domain.repository

import com.finance.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for category operations
 */
interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoryById(id: String): Flow<Category?>
    suspend fun insertCategory(category: Category): Result<Unit>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(id: String): Result<Unit>
    fun getDefaultCategories(): List<Category>
}
