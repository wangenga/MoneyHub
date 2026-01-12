package com.finance.app.domain.usecase

import com.finance.app.domain.model.AlertLevel
import com.finance.app.domain.model.BudgetUtilization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for tracking budget progress and triggering alerts
 */
@Singleton
class BudgetProgressTracker @Inject constructor(
    private val budgetCalculationUseCase: BudgetCalculationUseCase,
    private val budgetAlertManager: BudgetAlertManager,
    private val budgetNotificationSystem: BudgetNotificationSystem
) {
    
    /**
     * Data class representing budget progress summary
     */
    data class BudgetProgressSummary(
        val totalCategories: Int,
        val categoriesWithBudgets: Int,
        val categoriesOverBudget: Int,
        val categoriesAtWarning: Int,
        val categoriesAtCritical: Int,
        val totalBudgetAmount: Double,
        val totalSpentAmount: Double,
        val overallUtilizationPercentage: Double
    )
    
    /**
     * Start monitoring budget progress for a user
     * This will continuously monitor and send alerts when thresholds are crossed
     * @param userId The user ID to monitor
     * @param scope The coroutine scope for the monitoring
     */
    fun startMonitoring(userId: String, scope: CoroutineScope) {
        budgetAlertManager.monitorBudgetAlerts(userId)
            .distinctUntilChanged()
            .onEach { alerts ->
                // Send notifications for new alerts
                budgetNotificationSystem.sendBudgetAlerts(alerts)
            }
            .launchIn(scope)
    }
    
    /**
     * Get budget progress summary for the current month
     * @param userId The user ID
     * @return Flow of budget progress summary
     */
    fun getBudgetProgressSummary(userId: String): Flow<BudgetProgressSummary> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        
        return getBudgetProgressSummaryForMonth(userId, currentMonth, currentYear)
    }
    
    /**
     * Get budget progress summary for a specific month
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of budget progress summary
     */
    fun getBudgetProgressSummaryForMonth(
        userId: String,
        month: Int,
        year: Int
    ): Flow<BudgetProgressSummary> {
        return budgetCalculationUseCase.calculateBudgetUtilization(userId, month, year)
            .combine(budgetAlertManager.monitorBudgetAlertsForMonth(userId, month, year)) { utilizations, alerts ->
                calculateProgressSummary(utilizations, alerts)
            }
    }
    
    /**
     * Get detailed budget progress for all categories
     * @param userId The user ID
     * @return Flow of budget utilization list with alert information
     */
    fun getDetailedBudgetProgress(userId: String): Flow<List<BudgetUtilizationWithAlert>> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        
        return getDetailedBudgetProgressForMonth(userId, currentMonth, currentYear)
    }
    
    /**
     * Get detailed budget progress for a specific month
     * @param userId The user ID
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of budget utilization list with alert information
     */
    fun getDetailedBudgetProgressForMonth(
        userId: String,
        month: Int,
        year: Int
    ): Flow<List<BudgetUtilizationWithAlert>> {
        return budgetCalculationUseCase.calculateBudgetUtilization(userId, month, year)
            .combine(budgetAlertManager.monitorBudgetAlertsForMonth(userId, month, year)) { utilizations, alerts ->
                utilizations.map { utilization ->
                    val alert = alerts.find { it.categoryId == utilization.categoryId }
                    BudgetUtilizationWithAlert(
                        utilization = utilization,
                        alert = alert,
                        hasAlert = alert != null,
                        alertLevel = utilization.getAlertLevel()
                    )
                }
            }
    }
    
    /**
     * Check if any budgets need immediate attention (critical or over-budget)
     * @param userId The user ID
     * @return Flow of boolean indicating if immediate attention is needed
     */
    fun needsImmediateAttention(userId: String): Flow<Boolean> {
        return budgetAlertManager.monitorBudgetAlerts(userId)
            .combine(budgetAlertManager.getCriticalAlerts(userId)) { allAlerts, criticalAlerts ->
                val overBudgetAlerts = allAlerts.filter { it.alertLevel == AlertLevel.OVER_BUDGET }
                criticalAlerts.isNotEmpty() || overBudgetAlerts.isNotEmpty()
            }
    }
    
    /**
     * Get categories that are approaching their budget limits (warning level)
     * @param userId The user ID
     * @return Flow of categories at warning level
     */
    fun getCategoriesApproachingLimit(userId: String): Flow<List<BudgetAlertManager.BudgetAlert>> {
        return budgetAlertManager.getWarningAlerts(userId)
    }
    
    /**
     * Calculate progress summary from utilizations and alerts
     */
    private fun calculateProgressSummary(
        utilizations: List<BudgetUtilization>,
        alerts: List<BudgetAlertManager.BudgetAlert>
    ): BudgetProgressSummary {
        val totalBudgetAmount = utilizations.sumOf { it.budgetLimit }
        val totalSpentAmount = utilizations.sumOf { it.currentSpending }
        val overallUtilizationPercentage = if (totalBudgetAmount > 0) {
            (totalSpentAmount / totalBudgetAmount) * 100.0
        } else {
            0.0
        }
        
        val categoriesOverBudget = alerts.count { it.alertLevel == AlertLevel.OVER_BUDGET }
        val categoriesAtWarning = alerts.count { it.alertLevel == AlertLevel.WARNING }
        val categoriesAtCritical = alerts.count { it.alertLevel == AlertLevel.CRITICAL }
        
        return BudgetProgressSummary(
            totalCategories = utilizations.size,
            categoriesWithBudgets = utilizations.size,
            categoriesOverBudget = categoriesOverBudget,
            categoriesAtWarning = categoriesAtWarning,
            categoriesAtCritical = categoriesAtCritical,
            totalBudgetAmount = totalBudgetAmount,
            totalSpentAmount = totalSpentAmount,
            overallUtilizationPercentage = overallUtilizationPercentage
        )
    }
    
    /**
     * Data class combining budget utilization with alert information
     */
    data class BudgetUtilizationWithAlert(
        val utilization: BudgetUtilization,
        val alert: BudgetAlertManager.BudgetAlert?,
        val hasAlert: Boolean,
        val alertLevel: AlertLevel
    )
}