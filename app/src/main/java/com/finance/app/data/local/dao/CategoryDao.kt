package com.finance.app.data.local.dao

import androidx.room.*
import com.finance.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for category operations
 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId OR isDefault = 1")
    fun getAllCategoriesForUser(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: String): Flow<CategoryEntity?>

    @Query("""
        SELECT * FROM categories 
        WHERE categoryType = :type 
        AND (userId = :userId OR isDefault = 1)
    """)
    fun getCategoriesByType(type: String, userId: String): Flow<List<CategoryEntity>>

    @Query("""
        SELECT * FROM categories 
        WHERE categoryType = 'EXPENSE' 
        AND (userId = :userId OR isDefault = 1)
    """)
    fun getExpenseCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("""
        SELECT * FROM categories 
        WHERE categoryType = 'INCOME' 
        AND userId = :userId
    """)
    fun getIncomeCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isDefault = 1")
    suspend fun getDefaultCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM categories WHERE id = :id AND isDefault = 0")
    suspend fun deleteCustomCategory(id: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun getTransactionCountForCategory(categoryId: String): Int

    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getCategoriesByUserId(userId: String): List<CategoryEntity>
}
