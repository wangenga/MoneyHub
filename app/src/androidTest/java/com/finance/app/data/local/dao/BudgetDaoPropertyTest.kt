package com.finance.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.app.data.local.FinanceDatabase
import com.finance.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Property-based tests for BudgetDao persistence functionality.
 * 
 * These tests verify the correctness properties defined in the design document
 * for the budget-and-recurring-transactions feature.
 */
@RunWith(AndroidJUnit4::class)
class BudgetDaoPropertyTest {

    private lateinit var database: FinanceDatabase
    private lateinit var budgetDao: BudgetDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinanceDatabase::class.java
        ).allowMainThreadQueries().build()
        budgetDao = database.budgetDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * **Feature: budget-and-recurring-transactions, Property 1: Budget persistence round trip**
     * **Validates: Requirements 1.3, 7.1**
     * 
     * *For any* valid budget configuration, saving then retrieving the budget should produce 
     * an equivalent budget object.
     * 
     * This test runs 20 iterations with randomly generated budget data to verify that
     * the persistence layer correctly stores and retrieves budget information without data loss.
     */
    @Test
    fun property1_budgetPersistenceRoundTrip_savingAndRetrievingProducesEquivalentBudget() {
        runBlocking {
            // Run 20 iterations for faster test execution
            repeat(20) { iteration ->
                // Generate random valid budget data
                val budget = generateRandomBudget()
                
                // Save the budget
                budgetDao.insertBudget(budget)
                
                // Retrieve the budget
                val retrievedBudget = budgetDao.getBudgetById(budget.id)
                
                // Verify the retrieved budget is equivalent to the original
                assertNotNull("Budget should be retrieved successfully (iteration $iteration)", retrievedBudget)
                assertEquals("Budget ID should match (iteration $iteration)", budget.id, retrievedBudget?.id)
                assertEquals("User ID should match (iteration $iteration)", budget.userId, retrievedBudget?.userId)
                assertEquals("Category ID should match (iteration $iteration)", budget.categoryId, retrievedBudget?.categoryId)
                assertEquals("Monthly limit should match (iteration $iteration)", budget.monthlyLimit, retrievedBudget?.monthlyLimit ?: 0.0, 0.001)
                assertEquals("Month should match (iteration $iteration)", budget.month, retrievedBudget?.month)
                assertEquals("Year should match (iteration $iteration)", budget.year, retrievedBudget?.year)
                assertEquals("Created at should match (iteration $iteration)", budget.createdAt, retrievedBudget?.createdAt)
                assertEquals("Updated at should match (iteration $iteration)", budget.updatedAt, retrievedBudget?.updatedAt)
                
                // Clean up for next iteration
                budgetDao.deleteBudgetById(budget.id)
            }
        }
    }

    /**
     * Test: Budget persistence with edge case values
     * 
     * Verifies that budgets with boundary values (minimum amounts, edge months, etc.)
     * are correctly persisted and retrieved.
     */
    @Test
    fun budgetPersistence_withEdgeCaseValues_correctlyStoresAndRetrieves() {
        runBlocking {
            val edgeCases = listOf(
                // Minimum valid amount
                generateBudgetWithAmount(0.01),
                // Maximum valid amount
                generateBudgetWithAmount(1_000_000.0),
                // January (first month)
                generateBudgetWithMonth(1),
                // December (last month)
                generateBudgetWithMonth(12),
                // Minimum year
                generateBudgetWithYear(2000),
                // Maximum year
                generateBudgetWithYear(2100)
            )
            
            edgeCases.forEachIndexed { index, budget ->
                budgetDao.insertBudget(budget)
                val retrieved = budgetDao.getBudgetById(budget.id)
                
                assertNotNull("Edge case budget $index should be retrieved", retrieved)
                assertEquals("Edge case budget $index should match", budget, retrieved)
                
                budgetDao.deleteBudgetById(budget.id)
            }
        }
    }

    /**
     * Test: Budget update persistence
     * 
     * Verifies that updating a budget correctly persists the changes.
     */
    @Test
    fun budgetUpdate_persistsChangesCorrectly() {
        runBlocking {
            val originalBudget = generateRandomBudget()
            budgetDao.insertBudget(originalBudget)
            
            // Update the budget with new values
            val updatedBudget = originalBudget.copy(
                monthlyLimit = originalBudget.monthlyLimit + 100.0,
                updatedAt = System.currentTimeMillis()
            )
            budgetDao.updateBudget(updatedBudget)
            
            // Retrieve and verify
            val retrieved = budgetDao.getBudgetById(originalBudget.id)
            assertNotNull("Updated budget should be retrieved", retrieved)
            assertEquals("Monthly limit should be updated", updatedBudget.monthlyLimit, retrieved?.monthlyLimit ?: 0.0, 0.001)
            assertEquals("Updated at should be updated", updatedBudget.updatedAt, retrieved?.updatedAt)
            
            budgetDao.deleteBudgetById(originalBudget.id)
        }
    }

    // Helper functions for generating test data

    private fun generateRandomBudget(): BudgetEntity {
        val random = kotlin.random.Random.Default
        return BudgetEntity(
            id = UUID.randomUUID().toString(),
            userId = "user_${random.nextInt(1, 1001)}",
            categoryId = "category_${random.nextInt(1, 101)}",
            monthlyLimit = random.nextDouble(0.01, 1_000_000.0),
            month = random.nextInt(1, 13),
            year = random.nextInt(2000, 2101),
            createdAt = System.currentTimeMillis() - random.nextLong(0, 1000001),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun generateBudgetWithAmount(amount: Double): BudgetEntity {
        return generateRandomBudget().copy(monthlyLimit = amount)
    }

    private fun generateBudgetWithMonth(month: Int): BudgetEntity {
        return generateRandomBudget().copy(month = month)
    }

    private fun generateBudgetWithYear(year: Int): BudgetEntity {
        return generateRandomBudget().copy(year = year)
    }
}
