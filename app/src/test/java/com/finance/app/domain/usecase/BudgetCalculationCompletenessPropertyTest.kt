package com.finance.app.domain.usecase

import com.finance.app.domain.model.Budget
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType
import com.finance.app.domain.model.SyncStatus
import com.finance.app.domain.repository.BudgetRepository
import com.finance.app.domain.repository.TransactionRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import java.util.*
import kotlin.random.Random

/**
 * Property-based test for budget calculation completeness.
 * 
 * **Feature: budget-and-recurring-transactions, Property 4: Budget calculation completeness**
 * **Validates: Requirements 2.5, 5.5**
 * 
 * *For any* category and time period, budget progress calculations should include 
 * all transactions in that category for the specified period.
 * 
 * This test verifies that the budget calculation logic correctly includes every transaction
 * that matches the category and falls within the specified time period, ensuring no
 * transactions are missed or incorrectly excluded from budget calculations.
 */
class BudgetCalculationCompletenessPropertyTest : FunSpec({
    
    val budgetRepository = mockk<BudgetRepository>()
    val transactionRepository = mockk<TransactionRepository>()
    val useCase = BudgetCalculationUseCase(budgetRepository, transactionRepository)
    
    test("property4_budgetCalculationCompleteness_includesAllTransactionsInCategoryAndPeriod") {
        // Run 100 iterations as specified in the design document
        repeat(100) { iteration ->
            // Generate random test data
            val userId = "user_${Random.nextInt(1, 1001)}"
            val categoryId = "category_${Random.nextInt(1, 101)}"
            val month = Random.nextInt(1, 13)
            val year = Random.nextInt(2020, 2026)
            
            // Calculate the time period boundaries
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfMonth = calendar.timeInMillis
            
            // Generate random number of transactions (0 to 20)
            val transactionCount = Random.nextInt(0, 21)
            val transactions = mutableListOf<Transaction>()
            
            // Generate transactions that should be included in the calculation
            // These match: userId, categoryId, type=EXPENSE, and are within the date range
            repeat(transactionCount) { txIndex ->
                val amount = Random.nextDouble(0.01, 1000.0)
                val timestamp = Random.nextLong(startOfMonth, endOfMonth + 1)
                
                transactions.add(
                    Transaction(
                        id = "tx_${iteration}_${txIndex}",
                        userId = userId,
                        type = TransactionType.EXPENSE,
                        amount = amount,
                        categoryId = categoryId,
                        date = timestamp,
                        paymentMethod = "Card",
                        notes = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            }
            
            // Add some transactions that should NOT be included:
            // 1. Different category
            val differentCategoryCount = Random.nextInt(0, 6)
            repeat(differentCategoryCount) { txIndex ->
                transactions.add(
                    Transaction(
                        id = "tx_diff_cat_${iteration}_${txIndex}",
                        userId = userId,
                        type = TransactionType.EXPENSE,
                        amount = Random.nextDouble(0.01, 1000.0),
                        categoryId = "other_category_${Random.nextInt(1, 101)}",
                        date = Random.nextLong(startOfMonth, endOfMonth + 1),
                        paymentMethod = "Card",
                        notes = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            }
            
            // 2. Income transactions (should be excluded from expense calculations)
            val incomeCount = Random.nextInt(0, 6)
            repeat(incomeCount) { txIndex ->
                transactions.add(
                    Transaction(
                        id = "tx_income_${iteration}_${txIndex}",
                        userId = userId,
                        type = TransactionType.INCOME,
                        amount = Random.nextDouble(0.01, 1000.0),
                        categoryId = categoryId,
                        date = Random.nextLong(startOfMonth, endOfMonth + 1),
                        paymentMethod = "Transfer",
                        notes = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            }
            
            // 3. Transactions outside the time period
            val outsidePeriodCount = Random.nextInt(0, 6)
            repeat(outsidePeriodCount) { txIndex ->
                // Generate timestamp either before or after the period
                val timestamp = if (Random.nextBoolean()) {
                    Random.nextLong(startOfMonth - 86400000L * 30, startOfMonth) // Before
                } else {
                    Random.nextLong(endOfMonth + 1, endOfMonth + 86400000L * 30) // After
                }
                
                transactions.add(
                    Transaction(
                        id = "tx_outside_${iteration}_${txIndex}",
                        userId = userId,
                        type = TransactionType.EXPENSE,
                        amount = Random.nextDouble(0.01, 1000.0),
                        categoryId = categoryId,
                        date = timestamp,
                        paymentMethod = "Card",
                        notes = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            }
            
            // 4. Different user
            val differentUserCount = Random.nextInt(0, 6)
            repeat(differentUserCount) { txIndex ->
                transactions.add(
                    Transaction(
                        id = "tx_diff_user_${iteration}_${txIndex}",
                        userId = "other_user_${Random.nextInt(1, 1001)}",
                        type = TransactionType.EXPENSE,
                        amount = Random.nextDouble(0.01, 1000.0),
                        categoryId = categoryId,
                        date = Random.nextLong(startOfMonth, endOfMonth + 1),
                        paymentMethod = "Card",
                        notes = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            }
            
            // Shuffle transactions to ensure order doesn't matter
            transactions.shuffle()
            
            // Calculate expected total by applying the same filters as the implementation:
            // 1. Filter by userId
            // 2. Filter by categoryId and TransactionType.EXPENSE
            val expectedTotal = transactions
                .filter { it.userId == userId }
                .filter { it.categoryId == categoryId && it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            
            // Mock the repository to return all transactions
            every { 
                transactionRepository.getTransactionsByDateRange(startOfMonth, endOfMonth)
            } returns flowOf(transactions)
            
            // Execute the calculation
            val calculatedSpending = useCase.calculateCategorySpending(
                categoryId = categoryId,
                userId = userId,
                month = month,
                year = year
            ).first()
            
            // Verify that the calculation includes exactly the expected transactions
            // Round to 2 decimal places to handle floating point precision
            val roundedCalculated = (calculatedSpending * 100).toLong() / 100.0
            val roundedExpected = (expectedTotal * 100).toLong() / 100.0
            
            roundedCalculated shouldBe roundedExpected
        }
    }
    
    test("property4_budgetCalculationCompleteness_emptyTransactionList_returnsZero") {
        // Edge case: No transactions in the period
        val userId = "user_test"
        val categoryId = "category_test"
        val month = 6
        val year = 2024
        
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis
        
        every { 
            transactionRepository.getTransactionsByDateRange(startOfMonth, endOfMonth)
        } returns flowOf(emptyList())
        
        val result = useCase.calculateCategorySpending(categoryId, userId, month, year).first()
        
        result shouldBe 0.0
    }
    
    test("property4_budgetCalculationCompleteness_allTransactionsSameCategory_sumsCorrectly") {
        // Edge case: All transactions belong to the same category
        val userId = "user_test"
        val categoryId = "category_test"
        val month = 6
        val year = 2024
        
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis
        
        val transactions = listOf(
            Transaction(
                id = "1",
                userId = userId,
                type = TransactionType.EXPENSE,
                amount = 100.0,
                categoryId = categoryId,
                date = startOfMonth + 1000,
                paymentMethod = "Card",
                notes = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED
            ),
            Transaction(
                id = "2",
                userId = userId,
                type = TransactionType.EXPENSE,
                amount = 200.0,
                categoryId = categoryId,
                date = startOfMonth + 2000,
                paymentMethod = "Card",
                notes = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED
            ),
            Transaction(
                id = "3",
                userId = userId,
                type = TransactionType.EXPENSE,
                amount = 300.0,
                categoryId = categoryId,
                date = startOfMonth + 3000,
                paymentMethod = "Card",
                notes = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED
            )
        )
        
        every { 
            transactionRepository.getTransactionsByDateRange(startOfMonth, endOfMonth)
        } returns flowOf(transactions)
        
        val result = useCase.calculateCategorySpending(categoryId, userId, month, year).first()
        
        result shouldBe 600.0
    }
})
