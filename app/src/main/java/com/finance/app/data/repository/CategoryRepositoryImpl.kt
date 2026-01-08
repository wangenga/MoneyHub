package com.finance.app.data.repository

import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.mapper.toDomain
import com.finance.app.data.mapper.toEntity
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType
import com.finance.app.domain.model.DefaultCategoriesProvider
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.domain.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * Implementation of CategoryRepository using Room database
 */
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val syncScheduler: SyncScheduler
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCategoryById(id: String): Flow<Category?> {
        return categoryDao.getCategoryById(id).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getCategoriesByType(type: CategoryType, userId: String): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type.name, userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExpenseCategories(userId: String): Flow<List<Category>> {
        return categoryDao.getExpenseCategories(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getIncomeCategories(userId: String): Flow<List<Category>> {
        return categoryDao.getIncomeCategories(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertCategory(category: Category): Result<Unit> {
        return try {
            // Validate category fields
            if (category.name.isBlank() || category.color.isBlank() || category.iconName.isBlank()) {
                return Result.failure(IllegalArgumentException("Category name, color, and icon cannot be empty"))
            }
            
            categoryDao.insert(category.toEntity())
            
            // Schedule post-operation sync for custom categories
            if (!category.isDefault) {
                syncScheduler.schedulePostOperationSync()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            // Prevent updating default categories
            if (category.isDefault) {
                return Result.failure(IllegalOperationException("Default categories cannot be modified"))
            }
            
            // Validate category fields
            if (category.name.isBlank() || category.color.isBlank() || category.iconName.isBlank()) {
                return Result.failure(IllegalArgumentException("Category name, color, and icon cannot be empty"))
            }
            
            categoryDao.update(category.toEntity())
            
            // Schedule post-operation sync for custom categories
            syncScheduler.schedulePostOperationSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(id: String): Result<Unit> {
        return try {
            // Check if category has existing transactions
            val transactionCount = categoryDao.getTransactionCountForCategory(id)
            if (transactionCount > 0) {
                return Result.failure(
                    IllegalStateException("Cannot delete category with existing transactions")
                )
            }
            
            // Use deleteCustomCategory which only deletes non-default categories
            val deletedRows = categoryDao.deleteCustomCategory(id)
            if (deletedRows == 0) {
                return Result.failure(IllegalOperationException("Default categories cannot be deleted"))
            }
            
            // Schedule post-operation sync for all category deletions
            syncScheduler.schedulePostOperationSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDefaultCategories(): List<Category> {
        return DefaultCategoriesProvider.getDefaultExpenseCategories()
    }

    override suspend fun initializeDefaultCategories(): Result<Unit> {
        return try {
            val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
            val defaultEntities = defaultCategories.map { it.toEntity() }
            categoryDao.insertAll(defaultEntities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Custom exception for illegal operations
class IllegalOperationException(message: String) : Exception(message)
