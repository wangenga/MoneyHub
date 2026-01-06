package com.finance.app.data.local

import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.mapper.toEntity
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for category operations and default category initialization
 */
@Singleton
class CategoryDataSource @Inject constructor(
    private val categoryDao: CategoryDao
) {
    
    /**
     * Initializes default categories if the database is empty
     * Should be called when a new user signs up or on first app launch
     */
    suspend fun initializeDefaultCategories(userId: String? = null) {
        val existingCategories = categoryDao.getAllCategories().first()
        
        // Only initialize if no categories exist
        if (existingCategories.isEmpty()) {
            val defaultCategories = getDefaultCategories(userId)
            categoryDao.insertAll(defaultCategories.map { it.toEntity() })
        }
    }
    
    /**
     * Returns the list of default categories
     */
    private fun getDefaultCategories(userId: String?): List<Category> {
        val currentTime = System.currentTimeMillis()
        
        return listOf(
            Category(
                id = UUID.randomUUID().toString(),
                userId = userId,
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
                userId = userId,
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
                userId = userId,
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
                userId = userId,
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
                userId = userId,
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
