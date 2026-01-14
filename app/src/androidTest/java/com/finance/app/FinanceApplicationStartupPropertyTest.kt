package com.finance.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.app.data.local.FinanceDatabase
import com.finance.app.data.local.dao.BudgetDao
import com.finance.app.data.local.dao.RecurringTransactionDao
import com.finance.app.data.local.entity.BudgetEntity
import com.finance.app.data.local.entity.RecurringTransactionEntity
import com.finance.app.data.mapper.toDomain
import com.finance.app.domain.model.RecurrencePattern
import com.finance.app.domain.model.SyncStatus
import com.finance.app.domain.model.TransactionType
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Property-based tests for application startup data restoration.
 * 
 * These tests verify that when the application starts, all persisted budget and recurring
 * transaction data is correctly restored from local storage without data loss.
 */
@RunWith(AndroidJUnit4::class)
class FinanceApplicationStartupPropertyTest {

    private lateinit var database: FinanceDatabase
    private lateinit var budgetDao: BudgetDao
    private lateinit var recurringTransactionDao: RecurringTransactionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinanceDatabase::class.java
        ).allowMainThreadQueries().build()
        budgetDao = database.budgetDao()
        recurringTransactionDao = database.recurringTransactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * **Feature: budget-and-recurring-transactions, Property 11: Application startup data restoration**
     * **Validates: Requirements 7.3, 7.4**
     * 
     * *For any* persisted budget and recurring transaction data, application restart should restore 
     * all configurations and resume monitoring without data loss.
     * 
     * This test simulates application startup by:
     * 1. Creating and persisting random budget configurations
     * 2. Creating and persisting random recurring transaction configurations
     * 3. Simulating application restart by closing and reopening the database
     * 4. Verifying all data is restored exactly as it was persisted
     * 
     * This test runs 3 iterations with randomly generated data to verify that the startup
     * restoration process is consistent and reliable across different data scenarios.
     */
    @Test
    fun property11_applicationStartupDataRestoration_allPersistedDataIsRestoredWithoutLoss() {
        runBlocking {
            // Run 3 iterations for comprehensive testing
            repeat(3) { iteration ->
                // Step 1: Create and persist random budget configurations
                val numberOfBudgets = kotlin.random.Random.nextInt(2, 6) // 2 to 5 budgets
                val persistedBudgets = mutableListOf<BudgetEntity>()
                
                for (i in 0 until numberOfBudgets) {
                    val budget = generateRandomBudgetEntity()
                    budgetDao.insertBudget(budget)
                    persistedBudgets.add(budget)
                }
                
                // Step 2: Create and persist random recurring transaction configurations
                val numberOfRecurringTransactions = kotlin.random.Random.nextInt(2, 6) // 2 to 5 recurring transactions
                val persistedRecurringTransactions = mutableListOf<RecurringTransactionEntity>()
                
                for (i in 0 until numberOfRecurringTransactions) {
                    val recurringTransaction = generateRandomRecurringTransactionEntity()
                    recurringTransactionDao.insertRecurringTransaction(recurringTransaction)
                    persistedRecurringTransactions.add(recurringTransaction)
                }
                
                // Step 3: Simulate application restart by closing and reopening the database
                database.close()
                
                val newDatabase = Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FinanceDatabase::class.java
                ).allowMainThreadQueries().build()
                
                val newBudgetDao = newDatabase.budgetDao()
                val newRecurringTransactionDao = newDatabase.recurringTransactionDao()
                
                // Step 4: Verify all budget data is restored
                val restoredBudgets = newBudgetDao.getAllBudgets()
                
                assertEquals(
                    "All persisted budgets should be restored (iteration $iteration)",
                    persistedBudgets.size,
                    restoredBudgets.size
                )
                
                persistedBudgets.forEach { originalBudget ->
                    val restoredBudget = restoredBudgets.find { it.id == originalBudget.id }
                    assertNotNull(
                        "Budget ${originalBudget.id} should be restored (iteration $iteration)",
                        restoredBudget
                    )
                    
                    restoredBudget?.let { budget ->
                        // Verify all budget fields match
                        assertEquals(
                            "Budget ID should match (iteration $iteration)",
                            originalBudget.id,
                            budget.id
                        )
                        assertEquals(
                            "Budget userId should match (iteration $iteration)",
                            originalBudget.userId,
                            budget.userId
                        )
                        assertEquals(
                            "Budget categoryId should match (iteration $iteration)",
                            originalBudget.categoryId,
                            budget.categoryId
                        )
                        assertEquals(
                            "Budget monthlyLimit should match (iteration $iteration)",
                            originalBudget.monthlyLimit,
                            budget.monthlyLimit,
                            0.001
                        )
                        assertEquals(
                            "Budget month should match (iteration $iteration)",
                            originalBudget.month,
                            budget.month
                        )
                        assertEquals(
                            "Budget year should match (iteration $iteration)",
                            originalBudget.year,
                            budget.year
                        )
                        assertEquals(
                            "Budget createdAt should match (iteration $iteration)",
                            originalBudget.createdAt,
                            budget.createdAt
                        )
                        assertEquals(
                            "Budget updatedAt should match (iteration $iteration)",
                            originalBudget.updatedAt,
                            budget.updatedAt
                        )
                    }
                }
                
                // Step 5: Verify all recurring transaction data is restored
                val restoredRecurringTransactions = newRecurringTransactionDao.getAllRecurringTransactions()
                
                assertEquals(
                    "All persisted recurring transactions should be restored (iteration $iteration)",
                    persistedRecurringTransactions.size,
                    restoredRecurringTransactions.size
                )
                
                persistedRecurringTransactions.forEach { originalRecurringTransaction ->
                    val restoredRecurringTransaction = restoredRecurringTransactions.find { 
                        it.id == originalRecurringTransaction.id 
                    }
                    assertNotNull(
                        "Recurring transaction ${originalRecurringTransaction.id} should be restored (iteration $iteration)",
                        restoredRecurringTransaction
                    )
                    
                    // Verify all recurring transaction fields match
                    assertEquals(
                        "Recurring transaction ID should match (iteration $iteration)",
                        originalRecurringTransaction.id,
                        restoredRecurringTransaction?.id
                    )
                    assertEquals(
                        "Recurring transaction userId should match (iteration $iteration)",
                        originalRecurringTransaction.userId,
                        restoredRecurringTransaction?.userId
                    )
                    assertEquals(
                        "Recurring transaction templateType should match (iteration $iteration)",
                        originalRecurringTransaction.templateType,
                        restoredRecurringTransaction?.templateType
                    )
                    assertEquals(
                        "Recurring transaction templateAmount should match (iteration $iteration)",
                        originalRecurringTransaction.templateAmount,
                        restoredRecurringTransaction?.templateAmount ?: 0.0,
                        0.001
                    )
                    assertEquals(
                        "Recurring transaction templateCategoryId should match (iteration $iteration)",
                        originalRecurringTransaction.templateCategoryId,
                        restoredRecurringTransaction?.templateCategoryId
                    )
                    assertEquals(
                        "Recurring transaction recurrencePattern should match (iteration $iteration)",
                        originalRecurringTransaction.recurrencePattern,
                        restoredRecurringTransaction?.recurrencePattern
                    )
                    assertEquals(
                        "Recurring transaction nextDueDate should match (iteration $iteration)",
                        originalRecurringTransaction.nextDueDate,
                        restoredRecurringTransaction?.nextDueDate
                    )
                    assertEquals(
                        "Recurring transaction isActive should match (iteration $iteration)",
                        originalRecurringTransaction.isActive,
                        restoredRecurringTransaction?.isActive
                    )
                    assertEquals(
                        "Recurring transaction createdAt should match (iteration $iteration)",
                        originalRecurringTransaction.createdAt,
                        restoredRecurringTransaction?.createdAt
                    )
                    assertEquals(
                        "Recurring transaction updatedAt should match (iteration $iteration)",
                        originalRecurringTransaction.updatedAt,
                        restoredRecurringTransaction?.updatedAt
                    )
                }
                
                // Clean up
                newDatabase.close()
                
                // Reopen database for next iteration
                database = Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FinanceDatabase::class.java
                ).allowMainThreadQueries().build()
                budgetDao = database.budgetDao()
                recurringTransactionDao = database.recurringTransactionDao()
            }
        }
    }

    /**
     * Test: Budget data restoration with multiple users
     * 
     * Verifies that budget data for multiple users is correctly restored and isolated
     * during application startup.
     */
    @Test
    fun applicationStartup_budgetDataRestoration_multipleUsersDataIsIsolatedAndRestored() {
        runBlocking {
            repeat(2) { iteration ->
                // Create budgets for multiple users
                val user1Id = "user_${UUID.randomUUID()}"
                val user2Id = "user_${UUID.randomUUID()}"
                
                val user1Budgets = (1..3).map { 
                    generateRandomBudgetEntity().copy(userId = user1Id)
                }
                val user2Budgets = (1..3).map { 
                    generateRandomBudgetEntity().copy(userId = user2Id)
                }
                
                user1Budgets.forEach { budgetDao.insertBudget(it) }
                user2Budgets.forEach { budgetDao.insertBudget(it) }
                
                // Simulate restart
                database.close()
                
                val newDatabase = Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FinanceDatabase::class.java
                ).allowMainThreadQueries().build()
                
                val newBudgetDao = newDatabase.budgetDao()
                
                // Verify user1 budgets are restored
                val restoredUser1Budgets = mutableListOf<BudgetEntity>()
                newBudgetDao.getBudgetsForUser(user1Id).collect { budgets ->
                    restoredUser1Budgets.addAll(budgets)
                }
                
                assertEquals(
                    "User 1 budgets should be restored (iteration $iteration)",
                    user1Budgets.size,
                    restoredUser1Budgets.size
                )
                
                // Verify user2 budgets are restored
                val restoredUser2Budgets = mutableListOf<BudgetEntity>()
                newBudgetDao.getBudgetsForUser(user2Id).collect { budgets ->
                    restoredUser2Budgets.addAll(budgets)
                }
                
                assertEquals(
                    "User 2 budgets should be restored (iteration $iteration)",
                    user2Budgets.size,
                    restoredUser2Budgets.size
                )
                
                // Verify data isolation
                restoredUser1Budgets.forEach { budget ->
                    assertEquals(
                        "User 1 budget should have correct userId (iteration $iteration)",
                        user1Id,
                        budget.userId
                    )
                }
                
                restoredUser2Budgets.forEach { budget ->
                    assertEquals(
                        "User 2 budget should have correct userId (iteration $iteration)",
                        user2Id,
                        budget.userId
                    )
                }
                
                // Clean up
                newDatabase.close()
                
                database = Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FinanceDatabase::class.java
                ).allowMainThreadQueries().build()
                budgetDao = database.budgetDao()
                recurringTransactionDao = database.recurringTransactionDao()
            }
        }
    }

    /**
     * Test: Recurring transaction data restoration with active status
     * 
     * Verifies that recurring transaction active status is correctly restored,
     * allowing the scheduler to resume monitoring for due transactions.
     */
    @Test
    fun applicationStartup_recurringTransactionRestoration_activeStatusIsPreservedForSchedulerResumption() {
        runBlocking {
            repeat(2) { iteration ->
                // Create mix of active and inactive recurring transactions
                val activeRecurringTransactions = (1..3).map { 
                    generateRandomRecurringTransactionEntity().copy(isActive = true)
                }
                val inactiveRecurringTransactions = (1..2).map { 
                    generateRandomRecurringTransactionEntity().copy(isActive = false)
                }
                
                (activeRecurringTransactions + inactiveRecurringTransactions).forEach { 
                    recurringTransactionDao.insertRecurringTransaction(it) 
                }
                
                // Simulate restart
                database.close()
                
                val newDatabase = Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FinanceDatabase::class.java
                ).allowMainThreadQueries().build()
                
                val newRecurringTransactionDao = newDatabase.recurringTransactionDao()
                
                // Verify active recurring transactions are restored
                val restoredActiveRecurringTransactions = newRecurringTransactionDao.getAllRecurringTransactions()
                    .filter { it.isActive }
                
                assertEquals(
                    "Active recurring transactions should be restored (iteration $iteration)",
                    activeRecurringTransactions.size,
                    restoredActiveRecurringTransactions.size
                )
                
                // Verify inactive recurring transactions are restored
                val restoredInactiveRecurringTransactions = newRecurringTransactionDao.getAllRecurringTransactions()
                    .filter { !it.isActive }
                
                assertEquals(
                    "Inactive recurring transactions should be restored (iteration $iteration)",
                    inactiveRecurringTransactions.size,
                    restoredInactiveRecurringTransactions.size
                )
                
                // Verify scheduler can identify due transactions
                val currentTime = System.currentTimeMillis()
                val dueRecurringTransactions = newRecurringTransactionDao.getDueRecurringTransactions(currentTime)
                
                // All active recurring transactions with nextDueDate <= currentTime should be due
                val expectedDueCount = restoredActiveRecurringTransactions.count { 
                    it.nextDueDate <= currentTime 
                }
                
                assertEquals(
                    "Due recurring transactions should be correctly identified (iteration $iteration)",
                    expectedDueCount,
                    dueRecurringTransactions.size
                )
                
                // Clean up
                newDatabase.close()
                
                database = Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FinanceDatabase::class.java
                ).allowMainThreadQueries().build()
                budgetDao = database.budgetDao()
                recurringTransactionDao = database.recurringTransactionDao()
            }
        }
    }

    // Helper functions for generating test data

    private fun generateRandomBudgetEntity(): BudgetEntity {
        val random = kotlin.random.Random.Default
        val currentTime = System.currentTimeMillis()
        
        return BudgetEntity(
            id = "budget_${UUID.randomUUID()}",
            userId = "user_${random.nextInt(1, 101)}",
            categoryId = "category_${random.nextInt(1, 51)}",
            monthlyLimit = random.nextDouble(50.0, 5000.0),
            month = random.nextInt(1, 13),
            year = 2024,
            createdAt = currentTime - random.nextLong(0, 1000001),
            updatedAt = currentTime
        )
    }

    private fun generateRandomRecurringTransactionEntity(): RecurringTransactionEntity {
        val random = kotlin.random.Random.Default
        val patterns = listOf("DAILY", "WEEKLY", "MONTHLY")
        val types = listOf("INCOME", "EXPENSE")
        val paymentMethods = listOf("Cash", "Credit Card", "Debit Card", "Bank Transfer", null)
        val notesList = listOf("Test note", "Recurring payment", "Monthly subscription", null)
        val currentTime = System.currentTimeMillis()
        
        return RecurringTransactionEntity(
            id = "recurring_${UUID.randomUUID()}",
            userId = "user_${random.nextInt(1, 101)}",
            templateTransactionId = UUID.randomUUID().toString(),
            templateType = types.random(),
            templateAmount = random.nextDouble(10.0, 1000.0),
            templateCategoryId = "category_${random.nextInt(1, 51)}",
            templatePaymentMethod = paymentMethods.random(),
            templateNotes = notesList.random(),
            recurrencePattern = patterns.random(),
            nextDueDate = currentTime + random.nextLong(1, 30L * 24 * 60 * 60 * 1000),
            isActive = random.nextBoolean(),
            createdAt = currentTime - random.nextLong(0, 1000001),
            updatedAt = currentTime
        )
    }
}
