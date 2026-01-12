package com.finance.app.domain.usecase

import com.finance.app.domain.model.AlertLevel
import com.finance.app.domain.model.BudgetUtilization
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first

class BudgetAlertManagerTest : FunSpec({
    
    val budgetCalculationUseCase = mockk<BudgetCalculationUseCase>()
    val alertManager = BudgetAlertManager(budgetCalculationUseCase)
    
    test("should generate warning alert at 75% utilization") {
        // Given
        val userId = "user1"
        val month = 12
        val year = 2024
        
        val utilization = BudgetUtilization.create(
            categoryId = "category1",
            categoryName = "Food",
            budgetLimit = 100.0,
            currentSpending = 75.0 // 75% utilization
        )
        
        every { 
            budgetCalculationUseCase.calculateBudgetUtilization(userId, month, year)
        } returns flowOf(listOf(utilization))
        
        // When
        val alerts = alertManager.monitorBudgetAlertsForMonth(userId, month, year).first()
        
        // Then
        alerts shouldHaveSize 1
        alerts[0].alertLevel shouldBe AlertLevel.WARNING
        alerts[0].categoryId shouldBe "category1"
        alerts[0].utilizationPercentage shouldBe 75.0
    }
    
    test("should generate critical alert at 90% utilization") {
        // Given
        val userId = "user1"
        val month = 12
        val year = 2024
        
        val utilization = BudgetUtilization.create(
            categoryId = "category1",
            categoryName = "Food",
            budgetLimit = 100.0,
            currentSpending = 90.0 // 90% utilization
        )
        
        every { 
            budgetCalculationUseCase.calculateBudgetUtilization(userId, month, year)
        } returns flowOf(listOf(utilization))
        
        // When
        val alerts = alertManager.monitorBudgetAlertsForMonth(userId, month, year).first()
        
        // Then
        alerts shouldHaveSize 1
        alerts[0].alertLevel shouldBe AlertLevel.CRITICAL
        alerts[0].categoryId shouldBe "category1"
        alerts[0].utilizationPercentage shouldBe 90.0
    }
    
    test("should generate over budget alert at 100%+ utilization") {
        // Given
        val userId = "user1"
        val month = 12
        val year = 2024
        
        val utilization = BudgetUtilization.create(
            categoryId = "category1",
            categoryName = "Food",
            budgetLimit = 100.0,
            currentSpending = 120.0 // 120% utilization (over budget)
        )
        
        every { 
            budgetCalculationUseCase.calculateBudgetUtilization(userId, month, year)
        } returns flowOf(listOf(utilization))
        
        // When
        val alerts = alertManager.monitorBudgetAlertsForMonth(userId, month, year).first()
        
        // Then
        alerts shouldHaveSize 1
        alerts[0].alertLevel shouldBe AlertLevel.OVER_BUDGET
        alerts[0].categoryId shouldBe "category1"
        alerts[0].utilizationPercentage shouldBe 120.0
    }
    
    test("should not generate alert for normal utilization below 75%") {
        // Given
        val userId = "user1"
        val month = 12
        val year = 2024
        
        val utilization = BudgetUtilization.create(
            categoryId = "category1",
            categoryName = "Food",
            budgetLimit = 100.0,
            currentSpending = 50.0 // 50% utilization (normal)
        )
        
        every { 
            budgetCalculationUseCase.calculateBudgetUtilization(userId, month, year)
        } returns flowOf(listOf(utilization))
        
        // When
        val alerts = alertManager.monitorBudgetAlertsForMonth(userId, month, year).first()
        
        // Then
        alerts shouldHaveSize 0
    }
    
    test("should filter over budget categories correctly") {
        // Given
        val userId = "user1"
        
        val utilizations = listOf(
            BudgetUtilization.create("cat1", "Food", 100.0, 120.0), // Over budget
            BudgetUtilization.create("cat2", "Transport", 200.0, 90.0), // Normal
            BudgetUtilization.create("cat3", "Entertainment", 150.0, 180.0) // Over budget
        )
        
        every { 
            budgetCalculationUseCase.calculateBudgetUtilization(userId, any(), any())
        } returns flowOf(utilizations)
        
        // When
        val overBudgetAlerts = alertManager.getOverBudgetCategories(userId).first()
        
        // Then
        overBudgetAlerts shouldHaveSize 2
        overBudgetAlerts.all { it.alertLevel == AlertLevel.OVER_BUDGET } shouldBe true
    }
    
    test("alert messages should contain relevant information") {
        // Given
        val userId = "user1"
        val month = 12
        val year = 2024
        
        val utilization = BudgetUtilization.create(
            categoryId = "category1",
            categoryName = "Food",
            budgetLimit = 100.0,
            currentSpending = 75.0
        )
        
        every { 
            budgetCalculationUseCase.calculateBudgetUtilization(userId, month, year)
        } returns flowOf(listOf(utilization))
        
        // When
        val alerts = alertManager.monitorBudgetAlertsForMonth(userId, month, year).first()
        
        // Then
        val alert = alerts[0]
        alert.message shouldNotBe null
        alert.message.contains("75.0%") shouldBe true
        alert.message.contains("Food") shouldBe true
        alert.message.contains("25.00") shouldBe true // Remaining amount
    }
})