package com.finance.app.domain.usecase

import com.finance.app.domain.model.Budget
import com.finance.app.domain.model.BudgetUtilization
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

class BudgetCalculationUseCaseTest : FunSpec({
    
    val budgetRepository = mockk<BudgetRepository>()
    val transactionRepository = mockk<TransactionRepository>()
    val useCase = BudgetCalculationUseCase(budgetRepository, transactionRepository)
    
    test("calculateCategorySpending should sum expense transactions for category") {
        // Given
        val userId = "user1"
        val categoryId = "category1"
        val month = 12
        val year = 2024
        
        val calendar = Calendar.getInstance()
        calendar.set(2024, 11, 1, 0, 0, 0) // December 1, 2024
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, 31)
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
                amount = 50.0,
                categoryId = categoryId,
                date = startOfMonth + 2000,
                paymentMethod = "Cash",
                notes = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED
            ),
            Transaction(
                id = "3",
                userId = userId,
                type = TransactionType.INCOME, // Should be ignored
                amount = 200.0,
                categoryId = categoryId,
                date = startOfMonth + 3000,
                paymentMethod = "Transfer",
                notes = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED
            ),
            Transaction(
                id = "4",
                userId = userId,
                type = TransactionType.EXPENSE,
                amount = 25.0,
                categoryId = "other_category", // Different category, should be ignored
                date = startOfMonth + 4000,
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
        
        // When
        val result = useCase.calculateCategorySpending(categoryId, userId, month, year).first()
        
        // Then
        result shouldBe 150.0 // 100.0 + 50.0 (only expenses for the specific category)
    }
    
    test("calculateBudgetProgress should return correct percentage") {
        // Given
        val userId = "user1"
        val categoryId = "category1"
        val month = 12
        val year = 2024
        
        val budget = Budget(
            id = "budget1",
            userId = userId,
            categoryId = categoryId,
            monthlyLimit = 200.0,
            month = month,
            year = year,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        every { budgetRepository.getBudgetByCategory(userId, categoryId) } returns flowOf(budget)
        
        // Mock spending calculation to return 150.0
        val calendar = Calendar.getInstance()
        calendar.set(2024, 11, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, 31)
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
                amount = 150.0,
                categoryId = categoryId,
                date = startOfMonth + 1000,
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
        
        // When
        val result = useCase.calculateBudgetProgress(categoryId, userId, month, year).first()
        
        // Then
        result shouldBe 75.0 // 150.0 / 200.0 * 100 = 75%
    }
    
    test("isOverBudget should return true when spending exceeds budget") {
        // Given
        val userId = "user1"
        val categoryId = "category1"
        val month = 12
        val year = 2024
        
        val budget = Budget(
            id = "budget1",
            userId = userId,
            categoryId = categoryId,
            monthlyLimit = 100.0,
            month = month,
            year = year,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        every { budgetRepository.getBudgetByCategory(userId, categoryId) } returns flowOf(budget)
        
        // Mock spending to be over budget
        val calendar = Calendar.getInstance()
        calendar.set(2024, 11, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, 31)
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
                amount = 150.0, // Over the 100.0 budget
                categoryId = categoryId,
                date = startOfMonth + 1000,
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
        
        // When
        val result = useCase.isOverBudget(categoryId, userId, month, year).first()
        
        // Then
        result shouldBe true
    }
})