package com.finance.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.app.data.local.FinanceDatabase
import com.finance.app.data.local.entity.RecurringTransactionEntity
import com.finance.app.data.mapper.toDomain
import com.finance.app.data.mapper.toEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Property-based tests for RecurringTransactionDao persistence functionality.
 * 
 * These tests verify the correctness properties defined in the design document
 * for the budget-and-recurring-transactions feature.
 */
@RunWith(AndroidJUnit4::class)
class RecurringTransactionDaoPropertyTest {

    private lateinit var database: FinanceDatabase
    private lateinit var recurringTransactionDao: RecurringTransactionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinanceDatabase::class.java
        ).allowMainThreadQueries().build()
        recurringTransactionDao = database.recurringTransactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * **Feature: budget-and-recurring-transactions, Property 7: Recurring transaction persistence round trip**
     * **Validates: Requirements 3.4, 7.2**
     * 
     * *For any* valid recurring transaction configuration, saving then retrieving the configuration 
     * should produce an equivalent recurring transaction object.
     * 
     * This test runs 20 iterations with randomly generated recurring transaction data to verify that
     * the persistence layer correctly stores and retrieves recurring transaction information without data loss.
     */
    @Test
    fun property7_recurringTransactionPersistenceRoundTrip_savingAndRetrievingProducesEquivalentObject() {
        runBlocking {
            // Run 20 iterations for faster test execution
            repeat(20) { iteration ->
                // Generate random valid recurring transaction data
                val recurringTransaction = generateRandomRecurringTransactionEntity()
                
                // Save the recurring transaction
                recurringTransactionDao.insertRecurringTransaction(recurringTransaction)
                
                // Retrieve the recurring transaction
                val retrievedEntity = recurringTransactionDao.getRecurringTransactionById(recurringTransaction.id)
                
                // Verify the retrieved recurring transaction is equivalent to the original
                assertNotNull(
                    "Recurring transaction should be retrieved successfully (iteration $iteration)", 
                    retrievedEntity
                )
                
                // Verify all fields match
                assertEquals(
                    "Recurring transaction ID should match (iteration $iteration)", 
                    recurringTransaction.id, 
                    retrievedEntity?.id
                )
                assertEquals(
                    "User ID should match (iteration $iteration)", 
                    recurringTransaction.userId, 
                    retrievedEntity?.userId
                )
                assertEquals(
                    "Template transaction ID should match (iteration $iteration)", 
                    recurringTransaction.templateTransactionId, 
                    retrievedEntity?.templateTransactionId
                )
                assertEquals(
                    "Template type should match (iteration $iteration)", 
                    recurringTransaction.templateType, 
                    retrievedEntity?.templateType
                )
                assertEquals(
                    "Template amount should match (iteration $iteration)", 
                    recurringTransaction.templateAmount, 
                    retrievedEntity?.templateAmount ?: 0.0, 
                    0.001
                )
                assertEquals(
                    "Template category ID should match (iteration $iteration)", 
                    recurringTransaction.templateCategoryId, 
                    retrievedEntity?.templateCategoryId
                )
                assertEquals(
                    "Template payment method should match (iteration $iteration)", 
                    recurringTransaction.templatePaymentMethod, 
                    retrievedEntity?.templatePaymentMethod
                )
                assertEquals(
                    "Template notes should match (iteration $iteration)", 
                    recurringTransaction.templateNotes, 
                    retrievedEntity?.templateNotes
                )
                assertEquals(
                    "Recurrence pattern should match (iteration $iteration)", 
                    recurringTransaction.recurrencePattern, 
                    retrievedEntity?.recurrencePattern
                )
                assertEquals(
                    "Next due date should match (iteration $iteration)", 
                    recurringTransaction.nextDueDate, 
                    retrievedEntity?.nextDueDate
                )
                assertEquals(
                    "Is active should match (iteration $iteration)", 
                    recurringTransaction.isActive, 
                    retrievedEntity?.isActive
                )
                assertEquals(
                    "Created at should match (iteration $iteration)", 
                    recurringTransaction.createdAt, 
                    retrievedEntity?.createdAt
                )
                assertEquals(
                    "Updated at should match (iteration $iteration)", 
                    recurringTransaction.updatedAt, 
                    retrievedEntity?.updatedAt
                )
                
                // Clean up for next iteration
                recurringTransactionDao.deleteRecurringTransactionById(recurringTransaction.id)
            }
        }
    }

    /**
     * Test: Recurring transaction persistence with domain model round trip
     * 
     * Verifies that converting from domain model to entity and back preserves all data.
     */
    @Test
    fun recurringTransactionPersistence_withDomainModelRoundTrip_preservesAllData() {
        runBlocking {
            repeat(10) { iteration ->
                // Generate random recurring transaction entity
                val entity = generateRandomRecurringTransactionEntity()
                
                // Convert to domain model
                val domainModel = entity.toDomain()
                
                // Convert back to entity
                val roundTrippedEntity = domainModel.toEntity()
                
                // Save the round-tripped entity
                recurringTransactionDao.insertRecurringTransaction(roundTrippedEntity)
                
                // Retrieve it
                val retrieved = recurringTransactionDao.getRecurringTransactionById(roundTrippedEntity.id)
                
                // Verify all fields match the original
                assertNotNull("Round-tripped entity should be retrieved (iteration $iteration)", retrieved)
                assertEquals("ID should match (iteration $iteration)", entity.id, retrieved?.id)
                assertEquals("User ID should match (iteration $iteration)", entity.userId, retrieved?.userId)
                assertEquals("Template transaction ID should match (iteration $iteration)", 
                    entity.templateTransactionId, retrieved?.templateTransactionId)
                assertEquals("Template type should match (iteration $iteration)", 
                    entity.templateType, retrieved?.templateType)
                assertEquals("Template amount should match (iteration $iteration)", 
                    entity.templateAmount, retrieved?.templateAmount ?: 0.0, 0.001)
                assertEquals("Template category ID should match (iteration $iteration)", 
                    entity.templateCategoryId, retrieved?.templateCategoryId)
                assertEquals("Recurrence pattern should match (iteration $iteration)", 
                    entity.recurrencePattern, retrieved?.recurrencePattern)
                assertEquals("Next due date should match (iteration $iteration)", 
                    entity.nextDueDate, retrieved?.nextDueDate)
                assertEquals("Is active should match (iteration $iteration)", 
                    entity.isActive, retrieved?.isActive)
                
                // Clean up
                recurringTransactionDao.deleteRecurringTransactionById(roundTrippedEntity.id)
            }
        }
    }

    /**
     * Test: Recurring transaction persistence with edge case values
     * 
     * Verifies that recurring transactions with boundary values are correctly persisted and retrieved.
     */
    @Test
    fun recurringTransactionPersistence_withEdgeCaseValues_correctlyStoresAndRetrieves() {
        runBlocking {
            val edgeCases = listOf(
                // Minimum valid amount
                generateRecurringTransactionWithAmount(0.01),
                // Maximum valid amount
                generateRecurringTransactionWithAmount(1_000_000.0),
                // Each recurrence pattern
                generateRecurringTransactionWithPattern("DAILY"),
                generateRecurringTransactionWithPattern("WEEKLY"),
                generateRecurringTransactionWithPattern("MONTHLY"),
                // Active and inactive states
                generateRecurringTransactionWithActiveState(true),
                generateRecurringTransactionWithActiveState(false),
                // With and without optional fields
                generateRecurringTransactionWithOptionalFields(null, null),
                generateRecurringTransactionWithOptionalFields("Credit Card", "Monthly rent payment")
            )
            
            edgeCases.forEachIndexed { index, recurringTransaction ->
                recurringTransactionDao.insertRecurringTransaction(recurringTransaction)
                val retrieved = recurringTransactionDao.getRecurringTransactionById(recurringTransaction.id)
                
                assertNotNull("Edge case recurring transaction $index should be retrieved", retrieved)
                assertEquals("Edge case ID should match (case $index)", 
                    recurringTransaction.id, retrieved?.id)
                assertEquals("Edge case amount should match (case $index)", 
                    recurringTransaction.templateAmount, retrieved?.templateAmount ?: 0.0, 0.001)
                assertEquals("Edge case pattern should match (case $index)", 
                    recurringTransaction.recurrencePattern, retrieved?.recurrencePattern)
                assertEquals("Edge case active state should match (case $index)", 
                    recurringTransaction.isActive, retrieved?.isActive)
                
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
            templateAmount = random.nextDouble(0.01, 10_000.0),
            templateCategoryId = "category_${random.nextInt(1, 101)}",
            templatePaymentMethod = paymentMethods.random(),
            templateNotes = notesList.random(),
            recurrencePattern = patterns.random(),
            nextDueDate = System.currentTimeMillis() + random.nextLong(1, 365L * 24 * 60 * 60 * 1000),
            isActive = random.nextBoolean(),
            createdAt = System.currentTimeMillis() - random.nextLong(0, 1000001),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun generateRecurringTransactionWithAmount(amount: Double): RecurringTransactionEntity {
        return generateRandomRecurringTransactionEntity().copy(templateAmount = amount)
    }

    private fun generateRecurringTransactionWithPattern(pattern: String): RecurringTransactionEntity {
        return generateRandomRecurringTransactionEntity().copy(recurrencePattern = pattern)
    }

    private fun generateRecurringTransactionWithActiveState(isActive: Boolean): RecurringTransactionEntity {
        return generateRandomRecurringTransactionEntity().copy(isActive = isActive)
    }

    private fun generateRecurringTransactionWithOptionalFields(
        paymentMethod: String?, 
        notes: String?
    ): RecurringTransactionEntity {
        return generateRandomRecurringTransactionEntity().copy(
            templatePaymentMethod = paymentMethod,
            templateNotes = notes
        )
    }
}
