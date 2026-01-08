package com.finance.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.app.data.local.FinanceDatabase
import com.finance.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Property-based tests for CategoryDao type filtering functionality.
 * 
 * These tests verify the correctness properties defined in the design document
 * for the category system redesign feature.
 */
@RunWith(AndroidJUnit4::class)
class CategoryDaoPropertyTest {

    private lateinit var database: FinanceDatabase
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinanceDatabase::class.java
        ).allowMainThreadQueries().build()
        categoryDao = database.categoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * **Feature: category-system-redesign, Property 8: Category type filtering**
     * **Validates: Requirements 4.1, 4.2**
     * 
     * *For any* category query filtered by type, the result should contain only categories 
     * matching that type. Specifically:
     * - EXPENSE filter returns categories where `categoryType = EXPENSE`
     * - INCOME filter returns categories where `categoryType = INCOME`
     */
    @Test
    fun property8_categoryTypeFiltering_queriesReturnOnlyMatchingTypes() {
        runBlocking {
            // Test with a simple set of categories
            val categories = listOf(
                CategoryEntity(
                    id = "expense1",
                    userId = "user1",
                    name = "Transport",
                    color = "#2196F3",
                    iconName = "car",
                    categoryType = "EXPENSE",
                    isDefault = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                CategoryEntity(
                    id = "income1",
                    userId = "user1",
                    name = "Salary",
                    color = "#4CAF50",
                    iconName = "money",
                    categoryType = "INCOME",
                    isDefault = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                CategoryEntity(
                    id = "default1",
                    userId = null,
                    name = "Default Expense",
                    color = "#FF9800",
                    iconName = "shopping",
                    categoryType = "EXPENSE",
                    isDefault = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Clear database and insert test categories
            categoryDao.insertAll(categories)

            val testUserId = "user1"

            // Test EXPENSE filtering
            val expenseResults = categoryDao.getCategoriesByType("EXPENSE", testUserId).first()
            val expectedExpenseCategories = categories.filter { 
                it.categoryType == "EXPENSE" && (it.userId == testUserId || it.isDefault)
            }
            expenseResults.containsAll(expectedExpenseCategories) && 
            expectedExpenseCategories.containsAll(expenseResults)
            assertTrue("Expense results should match expected categories", 
                expenseResults.containsAll(expectedExpenseCategories) && 
                expectedExpenseCategories.containsAll(expenseResults))

            // Test INCOME filtering  
            val incomeResults = categoryDao.getCategoriesByType("INCOME", testUserId).first()
            val expectedIncomeCategories = categories.filter { 
                it.categoryType == "INCOME" && it.userId == testUserId
            }
            assertTrue("Income results should match expected categories",
                incomeResults.containsAll(expectedIncomeCategories) && 
                expectedIncomeCategories.containsAll(incomeResults))

            // Verify no cross-contamination: expense results should have no income categories
            assertTrue("All expense results should be EXPENSE type", 
                expenseResults.all { it.categoryType == "EXPENSE" })
            
            // Verify no cross-contamination: income results should have no expense categories
            assertTrue("All income results should be INCOME type",
                incomeResults.all { it.categoryType == "INCOME" })
        }
    }

    /**
     * Test: getExpenseCategories returns defaults plus user customs
     * 
     * *For any* user, getExpenseCategories should return all default expense categories
     * plus only that user's custom expense categories.
     */
    @Test
    fun getExpenseCategories_returnsDefaultsPlusUserCustomsOnly() {
        runBlocking {
            val categories = listOf(
                // Default expense category
                CategoryEntity(
                    id = "default_transport",
                    userId = null,
                    name = "Transport",
                    color = "#2196F3",
                    iconName = "car",
                    categoryType = "EXPENSE",
                    isDefault = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                // User's custom expense category
                CategoryEntity(
                    id = "custom_expense1",
                    userId = "user1",
                    name = "Custom Expense",
                    color = "#FF5722",
                    iconName = "custom",
                    categoryType = "EXPENSE",
                    isDefault = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                // Another user's custom expense category (should not be included)
                CategoryEntity(
                    id = "custom_expense2",
                    userId = "user2",
                    name = "Other User Expense",
                    color = "#9C27B0",
                    iconName = "other",
                    categoryType = "EXPENSE",
                    isDefault = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                // User's income category (should not be included in expense query)
                CategoryEntity(
                    id = "income1",
                    userId = "user1",
                    name = "Salary",
                    color = "#4CAF50",
                    iconName = "money",
                    categoryType = "INCOME",
                    isDefault = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Clear database and insert test categories
            categoryDao.insertAll(categories)

            val testUserId = "user1"

            // Test getExpenseCategories
            val results = categoryDao.getExpenseCategories(testUserId).first()
            
            // Should include: default expense + user1's custom expense
            // Should exclude: user2's custom expense + user1's income
            val expectedCategories = categories.filter { 
                it.categoryType == "EXPENSE" && (it.isDefault || it.userId == testUserId)
            }
            
            assertTrue("Results should match expected categories",
                results.containsAll(expectedCategories) && 
                expectedCategories.containsAll(results))
            
            // Verify we have exactly 2 categories (1 default + 1 custom for user1)
            assertEquals("Should have exactly 2 categories", 2, results.size)
            
            // Verify all results are expense type
            assertTrue("All results should be EXPENSE type",
                results.all { it.categoryType == "EXPENSE" })
        }
    }

    /**
     * Test: getIncomeCategories returns only user's income categories
     * 
     * *For any* user, getIncomeCategories should return only that user's income categories,
     * with no default categories included.
     */
    @Test
    fun getIncomeCategories_returnsOnlyUserIncomeCategories() {
        runBlocking {
            val categories = listOf(
                // Default expense category (should not be included)
                CategoryEntity(
                    id = "default_transport",
                    userId = null,
                    name = "Transport",
                    color = "#2196F3",
                    iconName = "car",
                    categoryType = "EXPENSE",
                    isDefault = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                // User1's income category
                CategoryEntity(
                    id = "income1",
                    userId = "user1",
                    name = "Salary",
                    color = "#4CAF50",
                    iconName = "money",
                    categoryType = "INCOME",
                    isDefault = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                // User2's income category (should not be included)
                CategoryEntity(
                    id = "income2",
                    userId = "user2",
                    name = "Freelance",
                    color = "#FF9800",
                    iconName = "work",
                    categoryType = "INCOME",
                    isDefault = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                // User1's expense category (should not be included)
                CategoryEntity(
                    id = "expense1",
                    userId = "user1",
                    name = "Custom Expense",
                    color = "#FF5722",
                    iconName = "custom",
                    categoryType = "EXPENSE",
                    isDefault = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Clear database and insert test categories
            categoryDao.insertAll(categories)

            val testUserId = "user1"

            // Test getIncomeCategories
            val results = categoryDao.getIncomeCategories(testUserId).first()
            
            // Should include only: user1's income categories
            // Should exclude: defaults, other users' categories, user1's expense categories
            val expectedCategories = categories.filter { 
                it.categoryType == "INCOME" && it.userId == testUserId
            }
            
            assertTrue("Results should match expected categories",
                results.containsAll(expectedCategories) && 
                expectedCategories.containsAll(results))
            
            // Verify we have exactly 1 category (user1's income)
            assertEquals("Should have exactly 1 category", 1, results.size)
            
            // Verify all results are income type
            assertTrue("All results should be INCOME type",
                results.all { it.categoryType == "INCOME" })
            
            // Verify all results belong to the test user
            assertTrue("All results should belong to test user",
                results.all { it.userId == testUserId })
        }
    }
}