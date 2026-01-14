package com.finance.app.domain.usecase

import com.finance.app.domain.model.AlertLevel
import com.finance.app.domain.model.BudgetUtilization
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.checkAll
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
    
    /**
     * **Feature: budget-and-recurring-transactions, Property 3: Budget alert threshold accuracy**
     * **Validates: Requirements 2.1, 2.2, 2.3**
     * 
     * *For any* category budget and spending amount, alerts should be generated if and only if 
     * spending reaches the correct thresholds (75% warning, 90% critical, 100% over-budget).
     * 
     * This property test runs 20 iterations with randomly generated budget limits and spending
     * amounts to verify that the alert system consistently generates the correct alert level
     * based on utilization percentage thresholds.
     */
    test("property3_budgetAlertThresholdAccuracy_alertsGeneratedAtCorrectThresholds") {
        // Generate random budget limits (positive values)
        val budgetLimits = Arb.double(min = 1.0, max = 10_000.0)
        // Generate random spending amounts (non-negative values)
        val spendingAmounts = Arb.double(min = 0.0, max = 15_000.0)
        
        checkAll(20, budgetLimits, spendingAmounts) { budgetLimit, spending ->
            // Given
            val userId = "user_test"
            val month = 12
            val year = 2024
            
            val utilization = BudgetUtilization.create(
                categoryId = "test_category",
                categoryName = "Test Category",
                budgetLimit = budgetLimit,
                currentSpending = spending
            )
            
            every { 
                budgetCalculationUseCase.calculateBudgetUtilization(userId, month, year)
            } returns flowOf(listOf(utilization))
            
            // When
            val alerts = alertManager.monitorBudgetAlertsForMonth(userId, month, year).first()
            
            // Then - verify alert generation matches threshold rules
            val utilizationPercentage = (spending / budgetLimit) * 100.0
            
            when {
                utilizationPercentage >= 100.0 -> {
                    // Should generate OVER_BUDGET alert
                    alerts shouldHaveSize 1
                    alerts[0].alertLevel shouldBe AlertLevel.OVER_BUDGET
                    alerts[0].utilizationPercentage shouldBe utilizationPercentage
                }
                utilizationPercentage >= 90.0 -> {
                    // Should generate CRITICAL alert
                    alerts shouldHaveSize 1
                    alerts[0].alertLevel shouldBe AlertLevel.CRITICAL
                    alerts[0].utilizationPercentage shouldBe utilizationPercentage
                }
                utilizationPercentage >= 75.0 -> {
                    // Should generate WARNING alert
                    alerts shouldHaveSize 1
                    alerts[0].alertLevel shouldBe AlertLevel.WARNING
                    alerts[0].utilizationPercentage shouldBe utilizationPercentage
                }
                else -> {
                    // Should NOT generate any alert (NORMAL level)
                    alerts shouldHaveSize 0
                }
            }
        }
    }
})