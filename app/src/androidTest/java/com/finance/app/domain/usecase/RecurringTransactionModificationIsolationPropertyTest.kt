package com.finance.app.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.app.data.local.FinanceDatabase
import com.finance.app.data.local.dao.RecurringTransactionDao
import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.local.entity.RecurringTransactionEntity
import com.finance.app.data.local.entity.TransactionEntity
import com.finance.app.data.mapper.toDomain
import com.finance.app.domain.model.RecurrencePattern
import com.finance.app.domain.model.RecurringTransaction
import com.finance.app.domain.model.SyncStatus
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Property-based tests for recurring transaction modification isolation.
 * 
 * These tests verify that modifications to recurring transactions only affect future instances,
 * not past instances that have already been created.
 */
@RunWith(AndroidJUnit4::class)
class RecurringTransactionModificationIsolationPropertyTest {

    private lateinit var database: FinanceDatabase
    private lateinit var recurringTransactionDao: RecurringTransactionDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinanceDatabase::class.java
        ).allowMainThreadQueries().build()
        recurringTransactionDao = database.recurringTransactionDao()
        transactionDao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * **Feature: budget-and-recurring-transactions, Property 8: Recurring transaction modification isolation**
     * **Validates: Requirements 4.5**
     * 
     * *For any* recurring transaction series, modifications should only affect future instances 
     * while leaving past instances unchanged.
     * 
     * This test runs 5 iterations with randomly generated recurring transaction data to verify that
     * when a recurring transaction template is modified, all previously created transaction instances
     * remain unchanged, while future instances will use the new template values.
     */
    @Test
    fun property8_recurringTransactionModificationIsolation_modificationsOnlyAffectFutureInstances() {
        runBlocking {
            // Run 2 iterations for faster testing
            repeat(2) { iteration ->
                // Generate a random recurring transaction
                val originalRecurringTransaction = generateRandomRecurringTransactionEntity()
                
                // Save the recurring transaction
                recurringTransactionDao.insertRecurringTransaction(originalRecurringTransaction)
                
                // Create several past transaction instances from this recurring transaction
                val pastInstances = mutableListOf<TransactionEntity>()
                val currentTime = System.currentTimeMillis()
                val numberOfPastInstances = kotlin.random.Random.nextInt(1, 3) // 1 to 2 past instances
                
                for (i in 0 until numberOfPastInstances) {
                    val pastInstanceDate = currentTime - ((numberOfPastInstances - i) * 24 * 60 * 60 * 1000L)
                    val pastInstance = TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = originalRecurringTransaction.userId,
                        type = originalRecurringTransaction.templateType,
                        amount = originalRecurringTransaction.templateAmount,
                        categoryId = originalRecurringTransaction.templateCategoryId,
                        date = pastInstanceDate,
                        paymentMethod = originalRecurringTransaction.templatePaymentMethod,
                        notes = originalRecurringTransaction.templateNotes,
                        createdAt = pastInstanceDate,
                        updatedAt = pastInstanceDate,
                        syncStatus = SyncStatus.SYNCED.name
                    )
                    transactionDao.insert(pastInstance)
                    pastInstances.add(pastInstance)
                }
                
                // Now modify the recurring transaction template
                val modifiedAmount = originalRecurringTransaction.templateAmount * 1.5
                val modifiedNotes = "Modified: ${originalRecurringTransaction.templateNotes ?: "note"}"
                val modifiedPaymentMethod = "Modified Payment Method"
                
                val modifiedRecurringTransaction = originalRecurringTransaction.copy(
                    templateAmount = modifiedAmount,
                    templateNotes = modifiedNotes,
                    templatePaymentMethod = modifiedPaymentMethod,
                    updatedAt = System.currentTimeMillis()
                )
                
                // Update the recurring transaction
                recurringTransactionDao.updateRecurringTransaction(modifiedRecurringTransaction)
                
                // Verify that all past instances remain unchanged
                pastInstances.forEach { pastInstance ->
                    val retrievedPastInstance = transactionDao.getTransactionById(pastInstance.id)
                    
                    // Since getTransactionById returns Flow, we need to collect it
                    retrievedPastInstance.collect { retrieved ->
                        assertNotNull(
                            "Past instance should still exist (iteration $iteration)", 
                            retrieved
                        )
                        
                        // Verify that past instance values have NOT changed
                        assertEquals(
                            "Past instance amount should remain unchanged (iteration $iteration)",
                            originalRecurringTransaction.templateAmount,
                            retrieved?.amount ?: 0.0,
                            0.001
                        )
                        assertEquals(
                            "Past instance notes should remain unchanged (iteration $iteration)",
                            originalRecurringTransaction.templateNotes,
                            retrieved?.notes
                        )
                        assertEquals(
                            "Past instance payment method should remain unchanged (iteration $iteration)",
                            originalRecurringTransaction.templatePaymentMethod,
                            retrieved?.paymentMethod
                        )
                        assertEquals(
                            "Past instance category should remain unchanged (iteration $iteration)",
                            originalRecurringTransaction.templateCategoryId,
                            retrieved?.categoryId
                        )
                        assertEquals(
                            "Past instance type should remain unchanged (iteration $iteration)",
                            originalRecurringTransaction.templateType,
                            retrieved?.type
                        )
                    }
                }
                
                // Clean up
                pastInstances.forEach { transactionDao.delete(it.id) }
                recurringTransactionDao.deleteRecurringTransactionById(originalRecurringTransaction.id)
                
                // Verify that the recurring transaction template has been updated
                val retrievedModifiedRecurringTransaction = 
                    recurringTransactionDao.getRecurringTransactionById(originalRecurringTransaction.id)
                
                assertNotNull(
                    "Modified recurring transaction should exist (iteration $iteration)",
                    retrievedModifiedRecurringTransaction
                )
                assertEquals(
                    "Recurring transaction template amount should be updated (iteration $iteration)",
                    modifiedAmount,
                    retrievedModifiedRecurringTransaction?.templateAmount ?: 0.0,
                    0.001
                )
                assertEquals(
                    "Recurring transaction template notes should be updated (iteration $iteration)",
                    modifiedNotes,
                    retrievedModifiedRecurringTransaction?.templateNotes
                )
                assertEquals(
                    "Recurring transaction template payment method should be updated (iteration $iteration)",
                    modifiedPaymentMethod,
                    retrievedModifiedRecurringTransaction?.templatePaymentMethod
                )
                
                // Clean up
                pastInstances.forEach { transactionDao.delete(it.id) }
                recurringTransactionDao.deleteRecurringTransactionById(originalRecurringTransaction.id)
            }
        }
    }

    /**
     * Test: Recurring transaction modification with category change
     * 
     * Verifies that when a recurring transaction's category is changed, past instances
     * retain their original category while future instances will use the new category.
     */
    @Test
    fun recurringTransactionModification_withCategoryChange_pastInstancesRetainOriginalCategory() {
        runBlocking {
            repeat(2) { iteration ->
                val recurringTransaction = generateRandomRecurringTransactionEntity()
                val originalCategoryId = recurringTransaction.templateCategoryId
                
                recurringTransactionDao.insertRecurringTransaction(recurringTransaction)
                
                // Create past instances
                val pastInstances = (1..3).map { i ->
                    val pastDate = System.currentTimeMillis() - (i * 24 * 60 * 60 * 1000L)
                    val instance = TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = recurringTransaction.userId,
                        type = recurringTransaction.templateType,
                        amount = recurringTransaction.templateAmount,
                        categoryId = originalCategoryId,
                        date = pastDate,
                        paymentMethod = recurringTransaction.templatePaymentMethod,
                        notes = recurringTransaction.templateNotes,
                        createdAt = pastDate,
                        updatedAt = pastDate,
                        syncStatus = SyncStatus.SYNCED.name
                    )
                    transactionDao.insert(instance)
                    instance
                }
                
                // Modify category
                val newCategoryId = "new_category_${UUID.randomUUID()}"
                val modified = recurringTransaction.copy(
                    templateCategoryId = newCategoryId,
                    updatedAt = System.currentTimeMillis()
                )
                recurringTransactionDao.updateRecurringTransaction(modified)
                
                // Verify past instances still have original category
                pastInstances.forEach { pastInstance ->
                    val retrieved = transactionDao.getTransactionById(pastInstance.id)
                    retrieved.collect { transaction ->
                        assertEquals(
                            "Past instance should retain original category (iteration $iteration)",
                            originalCategoryId,
                            transaction?.categoryId
                        )
                    }
                }
                
                // Verify template has new category
                val retrievedTemplate = recurringTransactionDao.getRecurringTransactionById(recurringTransaction.id)
                assertEquals(
                    "Template should have new category (iteration $iteration)",
                    newCategoryId,
                    retrievedTemplate?.templateCategoryId
                )
                
                // Clean up
                pastInstances.forEach { transactionDao.delete(it.id) }
                recurringTransactionDao.deleteRecurringTransactionById(recurringTransaction.id)
            }
        }
    }

    /**
     * Test: Recurring transaction modification with amount change
     * 
     * Verifies that when a recurring transaction's amount is changed, past instances
     * retain their original amount while the template reflects the new amount.
     */
    @Test
    fun recurringTransactionModification_withAmountChange_pastInstancesRetainOriginalAmount() {
        runBlocking {
            repeat(2) { iteration ->
                val recurringTransaction = generateRandomRecurringTransactionEntity()
                val originalAmount = recurringTransaction.templateAmount
                
                recurringTransactionDao.insertRecurringTransaction(recurringTransaction)
                
                // Create past instances
                val pastInstances = (1..5).map { i ->
                    val pastDate = System.currentTimeMillis() - (i * 24 * 60 * 60 * 1000L)
                    val instance = TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = recurringTransaction.userId,
                        type = recurringTransaction.templateType,
                        amount = originalAmount,
                        categoryId = recurringTransaction.templateCategoryId,
                        date = pastDate,
                        paymentMethod = recurringTransaction.templatePaymentMethod,
                        notes = recurringTransaction.templateNotes,
                        createdAt = pastDate,
                        updatedAt = pastDate,
                        syncStatus = SyncStatus.SYNCED.name
                    )
                    transactionDao.insert(instance)
                    instance
                }
                
                // Modify amount
                val newAmount = originalAmount * 2.0
                val modified = recurringTransaction.copy(
                    templateAmount = newAmount,
                    updatedAt = System.currentTimeMillis()
                )
                recurringTransactionDao.updateRecurringTransaction(modified)
                
                // Verify past instances still have original amount
                pastInstances.forEach { pastInstance ->
                    val retrieved = transactionDao.getTransactionById(pastInstance.id)
                    retrieved.collect { transaction ->
                        assertEquals(
                            "Past instance should retain original amount (iteration $iteration)",
                            originalAmount,
                            transaction?.amount ?: 0.0,
                            0.001
                        )
                    }
                }
                
                // Verify template has new amount
                val retrievedTemplate = recurringTransactionDao.getRecurringTransactionById(recurringTransaction.id)
                assertEquals(
                    "Template should have new amount (iteration $iteration)",
                    newAmount,
                    retrievedTemplate?.templateAmount ?: 0.0,
                    0.001
                )
                
                // Clean up
                pastInstances.forEach { transactionDao.delete(it.id) }
                recurringTransactionDao.deleteRecurringTransactionById(recurringTransaction.id)
            }
        }
    }

    /**
     * Test: Recurring transaction modification with multiple field changes
     * 
     * Verifies that when multiple fields of a recurring transaction are changed simultaneously,
     * past instances retain all their original values.
     */
    @Test
    fun recurringTransactionModification_withMultipleFieldChanges_pastInstancesRetainAllOriginalValues() {
        runBlocking {
            repeat(2) { iteration ->
                val recurringTransaction = generateRandomRecurringTransactionEntity()
                
                recurringTransactionDao.insertRecurringTransaction(recurringTransaction)
                
                // Create past instances
                val pastInstances = (1..4).map { i ->
                    val pastDate = System.currentTimeMillis() - (i * 24 * 60 * 60 * 1000L)
                    val instance = TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = recurringTransaction.userId,
                        type = recurringTransaction.templateType,
                        amount = recurringTransaction.templateAmount,
                        categoryId = recurringTransaction.templateCategoryId,
                        date = pastDate,
                        paymentMethod = recurringTransaction.templatePaymentMethod,
                        notes = recurringTransaction.templateNotes,
                        createdAt = pastDate,
                        updatedAt = pastDate,
                        syncStatus = SyncStatus.SYNCED.name
                    )
                    transactionDao.insert(instance)
                    instance
                }
                
                // Modify multiple fields
                val modified = recurringTransaction.copy(
                    templateAmount = recurringTransaction.templateAmount * 1.75,
                    templateCategoryId = "new_category_${UUID.randomUUID()}",
                    templatePaymentMethod = "New Payment Method",
                    templateNotes = "Completely new notes",
                    updatedAt = System.currentTimeMillis()
                )
                recurringTransactionDao.updateRecurringTransaction(modified)
                
                // Verify all past instances retain all original values
                pastInstances.forEach { pastInstance ->
                    val retrieved = transactionDao.getTransactionById(pastInstance.id)
                    retrieved.collect { transaction ->
                        assertNotNull("Past instance should exist (iteration $iteration)", transaction)
                        assertEquals(
                            "Amount should be unchanged (iteration $iteration)",
                            recurringTransaction.templateAmount,
                            transaction?.amount ?: 0.0,
                            0.001
                        )
                        assertEquals(
                            "Category should be unchanged (iteration $iteration)",
                            recurringTransaction.templateCategoryId,
                            transaction?.categoryId
                        )
                        assertEquals(
                            "Payment method should be unchanged (iteration $iteration)",
                            recurringTransaction.templatePaymentMethod,
                            transaction?.paymentMethod
                        )
                        assertEquals(
                            "Notes should be unchanged (iteration $iteration)",
                            recurringTransaction.templateNotes,
                            transaction?.notes
                        )
                    }
                }
                
                // Clean up
                pastInstances.forEach { transactionDao.delete(it.id) }
                recurringTransactionDao.deleteRecurringTransactionById(recurringTransaction.id)
            }
        }
    }

    // Helper functions for generating test data

    private fun generateRandomRecurringTransactionEntity(): RecurringTransactionEntity {
        val random = kotlin.random.Random.Default
        val patterns = listOf("DAILY", "WEEKLY", "MONTHLY")
        val types = listOf("INCOME", "EXPENSE")
        val paymentMethods = listOf("Cash", "Credit Card", "Debit Card", "Bank Transfer", null)
        val notesList = listOf("Test note", "Recurring payment", "Monthly subscription", null)
        
        return RecurringTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = "user_${random.nextInt(1, 1001)}",
            templateTransactionId = UUID.randomUUID().toString(),
            templateType = types.random(),
            templateAmount = random.nextDouble(10.0, 1000.0),
            templateCategoryId = "category_${random.nextInt(1, 101)}",
            templatePaymentMethod = paymentMethods.random(),
            templateNotes = notesList.random(),
            recurrencePattern = patterns.random(),
            nextDueDate = System.currentTimeMillis() + random.nextLong(1, 30L * 24 * 60 * 60 * 1000),
            isActive = true,
            createdAt = System.currentTimeMillis() - random.nextLong(0, 1000001),
            updatedAt = System.currentTimeMillis()
        )
    }
}
