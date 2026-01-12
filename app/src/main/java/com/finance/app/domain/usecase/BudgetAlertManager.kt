package com.finance.app.domain.usecase

import com.finance.app.domain.model.AlertLevel
import com.finance.app.domain.model.BudgetUtilization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject

/**
 * Manager for budget alert generation and threshold monitoring
 */
class BudgetAlertManager @Inject constructor(
    private val budgetCalculationUseCase: BudgetCalculationUseCase
) {
    
    /**
     * Data class representing a budget alert
     */
    data class BudgetAlert(
        val categoryId: String,
        val categoryName: String,
        val alertLevel: AlertLevel,
        val currentSpending: Double,
        val budgetLimit: Double,
        val utilizationPercentage: Double,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Monitor budget alerts for all categories for the current month
     * @param userId The user ID
     * @return Flow of budget alerts that need to be shown
     */
    fun monitorBudgetAlerts(userId: String): Flow<List<BudgetAlert>> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        
        return budgetCalculationUseCase.calculateBudgetUtilization(userId, currentMonth, currentYear)
            .map { utilizations ->
                utilizations.mapNotNull { utilization ->
                    generateAlertIfNeeded(utilization)
                }
            }
    }
    
    /**
     * Monitor budget alerts for a specific month
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of budget alerts that need to be shown
     */
    fun monitorBudgetAlertsForMonth(
        userId: String,
        month: Int,
        year: Int
    ): Flow<List<BudgetAlert>> {
        return budgetCalculationUseCase.calculateBudgetUtilization(userId, month, year)
            .map { utilizations ->
                utilizations.mapNotNull { utilization ->
                    generateAlertIfNeeded(utilization)
                }
            }
    }
    
    /**
     * Check if a specific category needs an alert
     * @param categoryId The category ID
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of budget alert or null if no alert needed
     */
    fun checkCategoryAlert(
        categoryId: String,
        userId: String,
        month: Int,
        year: Int
    ): Flow<BudgetAlert?> {
        return budgetCalculationUseCase.calculateBudgetUtilization(userId, month, year)
            .map { utilizations ->
                utilizations.find { it.categoryId == categoryId }
                    ?.let { utilization ->
                        generateAlertIfNeeded(utilization)
                    }
            }
    }
    
    /**
     * Get all categories that are currently over budget
     * @param userId The user ID
     * @return Flow of budget alerts for over-budget categories
     */
    fun getOverBudgetCategories(userId: String): Flow<List<BudgetAlert>> {
        return monitorBudgetAlerts(userId).map { alerts ->
            alerts.filter { it.alertLevel == AlertLevel.OVER_BUDGET }
        }
    }
    
    /**
     * Get all categories with critical alerts (90%+ usage)
     * @param userId The user ID
     * @return Flow of budget alerts for critical categories
     */
    fun getCriticalAlerts(userId: String): Flow<List<BudgetAlert>> {
        return monitorBudgetAlerts(userId).map { alerts ->
            alerts.filter { it.alertLevel == AlertLevel.CRITICAL }
        }
    }
    
    /**
     * Get all categories with warning alerts (75%+ usage)
     * @param userId The user ID
     * @return Flow of budget alerts for warning categories
     */
    fun getWarningAlerts(userId: String): Flow<List<BudgetAlert>> {
        return monitorBudgetAlerts(userId).map { alerts ->
            alerts.filter { it.alertLevel == AlertLevel.WARNING }
        }
    }
    
    /**
     * Generate an alert if the budget utilization meets threshold criteria
     */
    private fun generateAlertIfNeeded(utilization: BudgetUtilization): BudgetAlert? {
        val alertLevel = utilization.getAlertLevel()
        
        return when (alertLevel) {
            AlertLevel.WARNING -> BudgetAlert(
                categoryId = utilization.categoryId,
                categoryName = utilization.categoryName,
                alertLevel = alertLevel,
                currentSpending = utilization.currentSpending,
                budgetLimit = utilization.budgetLimit,
                utilizationPercentage = utilization.utilizationPercentage,
                message = generateWarningMessage(utilization)
            )
            AlertLevel.CRITICAL -> BudgetAlert(
                categoryId = utilization.categoryId,
                categoryName = utilization.categoryName,
                alertLevel = alertLevel,
                currentSpending = utilization.currentSpending,
                budgetLimit = utilization.budgetLimit,
                utilizationPercentage = utilization.utilizationPercentage,
                message = generateCriticalMessage(utilization)
            )
            AlertLevel.OVER_BUDGET -> BudgetAlert(
                categoryId = utilization.categoryId,
                categoryName = utilization.categoryName,
                alertLevel = alertLevel,
                currentSpending = utilization.currentSpending,
                budgetLimit = utilization.budgetLimit,
                utilizationPercentage = utilization.utilizationPercentage,
                message = generateOverBudgetMessage(utilization)
            )
            AlertLevel.NORMAL -> null // No alert needed
        }
    }
    
    /**
     * Generate warning message for 75% threshold
     */
    private fun generateWarningMessage(utilization: BudgetUtilization): String {
        val percentage = String.format("%.1f", utilization.utilizationPercentage)
        return "Warning: You've used $percentage% of your ${utilization.categoryName} budget. " +
                "Remaining: $${String.format("%.2f", utilization.remainingAmount)}"
    }
    
    /**
     * Generate critical message for 90% threshold
     */
    private fun generateCriticalMessage(utilization: BudgetUtilization): String {
        val percentage = String.format("%.1f", utilization.utilizationPercentage)
        return "Critical: You've used $percentage% of your ${utilization.categoryName} budget! " +
                "Only $${String.format("%.2f", utilization.remainingAmount)} remaining."
    }
    
    /**
     * Generate over-budget message for 100%+ threshold
     */
    private fun generateOverBudgetMessage(utilization: BudgetUtilization): String {
        val percentage = String.format("%.1f", utilization.utilizationPercentage)
        val overage = Math.abs(utilization.remainingAmount)
        return "Over Budget: You've exceeded your ${utilization.categoryName} budget by " +
                "$${String.format("%.2f", overage)} ($percentage% used)."
    }
}