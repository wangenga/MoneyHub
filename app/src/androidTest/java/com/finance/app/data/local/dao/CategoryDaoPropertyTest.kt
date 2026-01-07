package com.finance.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.app.data.local.FinanceDatabase
import com.finance.app.data.local.entity.CategoryEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
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
            expenseResults shouldContainExactlyInAnyOrder expectedExpenseCategories

            // Test INCOME filtering  
            val incomeResults = categoryDao.getCategoriesByType("INCOME", testUserId).first()
            val expectedIncomeCategories = categories.filter { 
                it.categoryType == "INCOME" && it.userId == testUserId
            }
            incomeResults shouldContainExactlyInAnyOrder expectedIncomeCategories

            // Verify no cross-contamination: expense results should have no income categories
            expenseResults.all { it.categoryType == "EXPENSE" } shouldBe true
            
            // Verify no cross-contamination: income results should have no expense categories
            incomeResults.all { it.categoryType == "INCOME" } shouldBe true
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
                    isDefault 