package com.finance.app.data.repository

import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.mapper.toDomain
import com.finance.app.data.mapper.toEntity
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType
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

    override suspend fun insertCategory(category: Category): Result<Unit> {
        return try {
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
            categoryDao.update(category.toEntity())
            
            // Schedule post-operation sync for custom categories
            if (!category.isDefault) {
                syncScheduler.schedulePostOperationSync()
            }
            
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
            
            categoryDao.delete(id)
            
            // Schedule post-operation sync for all category deletions
            // The sync will handle the case appropriately
            syncScheduler.schedulePostOperationSync()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDefaultCategories(): List<Category> {
        val currentTime = System.currentTimeMillis()
        
        return listOf(
            Category(
                id = UUID.randomUUID().toString(),
                userId = null,
                name = "Food",
                color = "#FF6B6B",
                iconName = "F",
                categoryType = CategoryType.EXPENSE,
                isDefault = true,
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            Category(
                id = UUID.randomUUID().toString(),
                userId = null,
                name = "Rent",
                color = "#4ECDC4",
                iconName = "R",
                categoryType = CategoryType.EXPENSE,
                isDefault = true,
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            Category(
                id = UUID.randomUUID().toString(),
                userId = null,
                name = "Transport",
                color = "#45B7D1",
                iconName = "T",
                categoryType = CategoryType.EXPENSE,
                isDefault = true,
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            Category(
                id = UUID.randomUUID().toString(),
                userId = null,
                name = "Entertainment",
                color = "#FFA07A",
                iconName = "E",
                categoryType = CategoryType.EXPENSE,
                isDefault = true,
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            Category(
                id = UUID.randomUUID().toString(),
                userId = null,
                name = "Utilities",
                color = "#98D8C8",
                iconName = "U",
                categoryType = CategoryType.EXPENSE,
                isDefault = true,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        )
    }
}
