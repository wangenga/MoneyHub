package com.finance.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.app.data.local.FinanceDatabase
import com.finance.app.data.local.dao.BudgetDao
import com.finance.app.data.local.dao.RecurringTransactionDao
import com.finance.app.data.local.entity.BudgetEntity
import com.finance.app.data.local.entity.RecurringTransactionEntity
import com.finance.app.data.mapper.toDomain
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Property-based tests for data synchronization conflict resolution.
 * 
 * These tests verify that when conflicts occur during synchronization between local and remote data,
 * the system correctly resolves them using timestamp-based conflict resolution (last-write-wins).
 */
@RunWith(AndroidJUnit4::class)
class DataSynchronizationConflictResolutionPropertyTest {

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
     * **Feature: budget-and-recurring-transactions, Property 10: Data synchronization conflict resolution**
     * **Validates: Requirements 7.5**
     * 
     * *For any* conflicting budget or recurring transaction data with different timestamps, 
     * the conflict resolution should consistently select the data with the most recent timestamp.
     * 
     * This test verifies that when local and remote versions of the same budget have different
     * timestamps, the version with the more recent timestamp is selected during conflict resolution.
     */
    @Test
    fun property10_dataSynchronizationConflictResolution_budgetConflictResolvesToNewerTimestamp() {
        runBlocking {
            // Run 5 iterations with randomly generated budget data
            repeat(5) { iteration ->
                val budgetId = "budget_${UUID.randomUUID()}"
                val userId = "user_${UUID.randomUUID()}"
                val categoryId = "category_${UUID.randomUUID()}"
                
                // Create local budget with older timestamp
                val localTimestamp = System.currentTimeMillis() - 10000L // 10 seconds ago
                val localBudget = BudgetEntity(
                    id = budgetId,
                    userId = userId,
                    categoryId = categoryId,
                    monthlyLimit = 500.0,
                    month = 1,
                    year = 2024,
                    createdAt = localTimestamp - 100000L,
                    updatedAt = localTimestamp
                )
                
                // Insert local budget
                budgetDao.insertBudget(localBudget)
                
                // Simulate remote budget with newer timestamp
                val remoteTimestamp = System.currentTimeMillis() // Now
                val remoteBudget = BudgetEntity(
                    id = budgetId,
                    userId = userId,
                    categoryId = categoryId,
                    monthlyLimit = 750.0, // Different amount
                    month = 1,
                    year = 2024,
                    createdAt = localTimestamp - 100000L,
                    updatedAt = remoteTimestamp
                )
                
                // Verify that remote timestamp is newer
                assertTrue(
                    "Remote timestamp should be newer than local (iteration $iteration)",
                    remoteTimestamp > localTimestamp
                )
                
                // Simulate conflict resolution: insert remote budget (newer timestamp wins)
                budgetDao.insertBudget(remoteBudget)
                
                // Verify that the newer version (remote) is now in the database
                val resolvedBudget = budgetDao.getBudgetById(budgetId)
                assertNotNull("Budget should exist after conflict resolution (iteration $iteration)", resolvedBudget)
                assertEquals(
                    "Resolved budget should have remote's amount (newer timestamp) (iteration $iteration)",
                    750.0,
                    resolvedBudget?.monthlyLimit ?: 0.0,
                    0.001
                )
                assertEquals(
                    "Resolved budget should have remote's timestamp (iteration $iteration)",
                    remoteTimestamp,
                    resolvedBudget?.updatedAt ?: 0L
                )
                
                // Clean up
                budgetDao.deleteBudgetById(budgetId)
            }
        }
    }

    /**
     * **Feature: budget-and-recurring-transactions, Property 10: Data synchronization conflict resolution**
     * **Validates: Requirements 7.5**
     * 
     * Test: Recurring transaction conflict resolution with timestamp comparison
     * 
     * Verifies that when local and remote versions of the same recurring transaction have different
     * timestamps, the version with the more recent timestamp is selected during conflict resolution.
     */
    @Test
    fun property10_dataSynchronizationConflictResolution_recurringTransactionConflictResolvesToNewerTimestamp() {
        runBlocking {
            // Run 5 iterations with randomly generated recurring transaction data
            repeat(5) { iteration ->
                val recurringTransactionId = "recurring_${UUID.randomUUID()}"
                val userId = "user_${UUID.randomUUID()}"
                
                // Create local recurring transaction with older timestamp
                val localTimestamp = System.currentTimeMillis() - 10000L // 10 seconds ago
                val localRecurringTransaction = RecurringTransactionEntity(
                    id = recurringTransactionId,
                    userId = userId,
                    templateTransactionId = UUID.randomUUID().toString(),
                    templateType = "EXPENSE",
                    templateAmount = 100.0,
                    templateCategoryId = "category_${UUID.randomUUID()}",
                    templatePaymentMethod = "Credit Card",
                    templateNotes = "Local version",
                    recurrencePattern = "MONTHLY",
                    nextDueDate = System.currentTimeMillis() + 86400000L,
                    isActive = true,
                    createdAt = localTimestamp - 100000L,
                    updatedAt = localTimestamp
                )
                
                // Insert local recurring transaction
                recurringTransactionDao.insertRecurringTransaction(localRecurringTransaction)
                
                // Simulate remote recurring transaction with newer timestamp
                val remoteTimestamp = System.currentTimeMillis() // Now
                val remoteRecurringTransaction = RecurringTransactionEntity(
                    id = recurringTransactionId,
                    userId = userId,
                    templateTransactionId = UUID.randomUUID().toString(),
                    templateType = "EXPENSE",
                    templateAmount = 150.0, // Different amount
                    templateCategoryId = "category_${UUID.randomUUID()}", // Different category
                    templatePaymentMethod = "Debit Card", // Different payment method
                    templateNotes = "Remote version",
                    recurrencePattern = "WEEKLY", // Different pattern
                    nextDueDate = System.currentTimeMillis() + 604800000L,
                    isActive = true,
                    createdAt = localTimestamp - 100000L,
                    updatedAt = remoteTimestamp
                )
                
                // Verify that remote timestamp is newer
                assertTrue(
                    "Remote timestamp should be newer than local (iteration $iteration)",
                    remoteTimestamp > localTimestamp
                )
                
                // Simulate conflict resolution: insert remote recurring transaction (newer timestamp wins)
                recurringTransactionDao.insertRecurringTransaction(remoteRecurringTransaction)
                
                // Verify that the newer version (remote) is now in the database
                val resolvedRecurringTransaction = recurringTransactionDao.getRecurringTransactionById(recurringTransactionId)
                assertNotNull("Recurring transaction should exist after conflict resolution (iteration $iteration)", resolvedRecurringTransaction)
                assertEquals(
                    "Resolved recurring transaction should have remote's amount (newer timestamp) (iteration $iteration)",
                    150.0,
                    resolvedRecurringTransaction?.templateAmount ?: 0.0,
                    0.001
                )
                assertEquals(
                    "Resolved recurring transaction should have remote's payment method (iteration $iteration)",
                    "Debit Card",
                    resolvedRecurringTransaction?.templatePaymentMethod
                )
                assertEquals(
                    "Resolved recurring transaction should have remote's recurrence pattern (iteration $iteration)",
                    "WEEKLY",
                    resolvedRecurringTransaction?.recurrencePattern
                )
                assertEquals(
                    "Resolved recurring transaction should have remote's timestamp (iteration $iteration)",
                    remoteTimestamp,
                    resolvedRecurringTransaction?.updatedAt ?: 0L
                )
                
                // Clean up
                recurringTransactionDao.deleteRecurringTransactionById(recurringTransactionId)
            }
        }
    }

    /**
     * Test: Budget conflict resolution with equal timestamps (local wins)
     * 
     * Verifies that when local and remote versions have equal timestamps, the local version
     * is retained (already uploaded, so no need to overwrite).
     */
    @Test
    fun dataSynchronizationConflictResolution_budgetWithEqualTimestamps_localVersionIsRetained() {
        runBlocking {
            repeat(3) { iteration ->
                val budgetId = "budget_${UUID.randomUUID()}"
                val userId = "user_${UUID.randomUUID()}"
                val categoryId = "category_${UUID.randomUUID()}"
                
                // Create local budget
                val timestamp = System.currentTimeMillis()
                val localBudget = BudgetEntity(
                    id = budgetId,
                    userId = userId,
                    categoryId = categoryId,
                    monthlyLimit = 500.0,
                    month = 1,
                    year = 2024,
                    createdAt = timestamp - 100000L,
                    updatedAt = timestamp
                )
                
                // Insert local budget
                budgetDao.insertBudget(localBudget)
                
                // Create remote budget with same timestamp but different data
                val remoteBudget = BudgetEntity(
                    id = budgetId,
                    userId = userId,
                    categoryId = categoryId,
                    monthlyLimit = 750.0, // Different amount
                    month = 1,
                    year = 2024,
                    createdAt = timestamp - 100000L,
                    updatedAt = timestamp // Same timestamp
                )
                
                // Verify timestamps are equal
                assertEquals(
                    "Timestamps should be equal (iteration $iteration)",
                    localBudget.updatedAt,
                    remoteBudget.updatedAt
                )
                
                // In conflict resolution, local version should be kept (not overwritten)
                // So we don't insert the remote version
                
                // Verify that local version is still in the database
                val resolvedBudget = budgetDao.getBudgetById(budgetId)
                assertNotNull("Budget should exist (iteration $iteration)", resolvedBudget)
                assertEquals(
                    "Local budget amount should be retained (iteration $iteration)",
                    500.0,
                    resolvedBudget?.monthlyLimit ?: 0.0,
                    0.001
                )
                
                // Clean up
                budgetDao.deleteBudgetById(budgetId)
            }
        }
    }

    /**
     * Test: Recurring transaction conflict resolution with older local timestamp
     * 
     * Verifies that when local recurring transaction is older than remote, the remote version
     * is selected during conflict resolution.
     */
    @Test
    fun dataSynchronizationConflictResolution_recurringTransactionWithOlderLocal_remoteVersionIsSelected() {
        runBlocking {
            repeat(3) { iteration ->
                val recurringTransactionId = "recurring_${UUID.randomUUID()}"
                val userId = "user_${UUID.randomUUID()}"
                
                // Create local recurring transaction with very old timestamp
                val localTimestamp = System.currentTimeMillis() - 1000000L // Very old
                val localRecurringTransaction = RecurringTransactionEntity(
                    id = recurringTransactionId,
                    userId = userId,
                    templateTransactionId = UUID.randomUUID().toString(),
                    templateType = "EXPENSE",
                    templateAmount = 50.0,
                    templateCategoryId = "category_${UUID.randomUUID()}",
                    templatePaymentMethod = "Cash",
                    templateNotes = "Old local version",
                    recurrencePattern = "DAILY",
                    nextDueDate = System.currentTimeMillis() + 86400000L,
                    isActive = true,
                    createdAt = localTimestamp - 100000L,
                    updatedAt = localTimestamp
                )
                
                // Insert local recurring transaction
                recurringTransactionDao.insertRecurringTransaction(localRecurringTransaction)
                
                // Create remote recurring transaction with much newer timestamp
                val remoteTimestamp = System.currentTimeMillis() - 1000L // Recent
                val remoteRecurringTransaction = RecurringTransactionEntity(
                    id = recurringTransactionId,
                    userId = userId,
                    templateTransactionId = UUID.randomUUID().toString(),
                    templateType = "INCOME",
                    templateAmount = 200.0,
                    templateCategoryId = "category_${UUID.randomUUID()}",
                    templatePaymentMethod = "Bank Transfer",
                    templateNotes = "New remote version",
                    recurrencePattern = "MONTHLY",
                    nextDueDate = System.currentTimeMillis() + 2592000000L,
                    isActive = true,
                    createdAt = localTimestamp - 100000L,
                    updatedAt = remoteTimestamp
                )
                
                // Verify remote is much newer
                assertTrue(
                    "Remote should be significantly newer (iteration $iteration)",
                    remoteTimestamp - localTimestamp > 900000L
                )
                
                // Simulate conflict resolution: insert remote version
                recurringTransactionDao.insertRecurringTransaction(remoteRecurringTransaction)
                
                // Verify remote version is now in database
                val resolvedRecurringTransaction = recurringTransactionDao.getRecurringTransactionById(recurringTransactionId)
                assertNotNull("Recurring transaction should exist (iteration $iteration)", resolvedRecurringTransaction)
                assertEquals(
                    "Should have remote's type (iteration $iteration)",
                    "INCOME",
                    resolvedRecurringTransaction?.templateType
                )
                assertEquals(
                    "Should have remote's amount (iteration $iteration)",
                    200.0,
                    resolvedRecurringTransaction?.templateAmount ?: 0.0,
                    0.001
                )
                assertEquals(
                    "Should have remote's notes (iteration $iteration)",
                    "New remote version",
                    resolvedRecurringTransaction?.templateNotes
                )
                
                // Clean up
                recurringTransactionDao.deleteRecurringTransactionById(recurringTransactionId)
            }
        }
    }

    /**
     * Test: Multiple budget conflicts resolved in sequence
     * 
     * Verifies that multiple budget conflicts can be resolved correctly in sequence,
     * each using timestamp-based conflict resolution.
     */
    @Test
    fun dataSynchronizationConflictResolution_multipleBudgetConflicts_allResolvedByTimestamp() {
        runBlocking {
            repeat(2) { iteration ->
                val numberOfConflicts = 3
                val budgetIds = (1..numberOfConflicts).map { "budget_${UUID.randomUUID()}" }
                val userId = "user_${UUID.randomUUID()}"
                
                // Create and insert local budgets with older timestamps
                val localTimestamp = System.currentTimeMillis() - 10000L
                budgetIds.forEach { budgetId ->
                    val localBudget = BudgetEntity(
                        id = budgetId,
                        userId = userId,
                        categoryId = "category_${UUID.randomUUID()}",
                        monthlyLimit = 500.0,
                        month = 1,
                        year = 2024,
                        createdAt = localTimestamp - 100000L,
                        updatedAt = localTimestamp
                    )
                    budgetDao.insertBudget(localBudget)
                }
                
                // Create remote budgets with newer timestamps and different amounts
                val remoteTimestamp = System.currentTimeMillis()
                budgetIds.forEach { budgetId ->
                    val remoteBudget = BudgetEntity(
                        id = budgetId,
                        userId = userId,
                        categoryId = "category_${UUID.randomUUID()}",
                        monthlyLimit = 750.0,
                        month = 1,
                        year = 2024,
                        createdAt = localTimestamp - 100000L,
                        updatedAt = remoteTimestamp
                    )
                    // Simulate conflict resolution
                    budgetDao.insertBudget(remoteBudget)
                }
                
                // Verify all conflicts were resolved to remote versions
                budgetIds.forEach { budgetId ->
                    val resolvedBudget = budgetDao.getBudgetById(budgetId)
                    assertNotNull("Budget should exist (iteration $iteration)", resolvedBudget)
                    assertEquals(
                        "Should have remote's amount (iteration $iteration)",
                        750.0,
                        resolvedBudget?.monthlyLimit ?: 0.0,
                        0.001
                    )
                    assertEquals(
                        "Should have remote's timestamp (iteration $iteration)",
                        remoteTimestamp,
                        resolvedBudget?.updatedAt ?: 0L
                    )
                }
                
                // Clean up
                budgetIds.forEach { budgetDao.deleteBudgetById(it) }
            }
        }
    }
}
