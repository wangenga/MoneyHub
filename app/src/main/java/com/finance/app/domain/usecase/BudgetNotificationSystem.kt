package com.finance.app.domain.usecase

import com.finance.app.domain.model.AlertLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System for managing budget alert notifications
 */
@Singleton
class BudgetNotificationSystem @Inject constructor() {
    
    /**
     * Data class representing a budget notification
     */
    data class BudgetNotification(
        val id: String,
        val title: String,
        val message: String,
        val alertLevel: AlertLevel,
        val categoryId: String,
        val categoryName: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isRead: Boolean = false,
        val isDismissed: Boolean = false
    )
    
    private val _notifications = MutableSharedFlow<BudgetNotification>()
    val notifications: Flow<BudgetNotification> = _notifications.asSharedFlow()
    
    private val _activeNotifications = mutableListOf<BudgetNotification>()
    
    /**
     * Send a budget alert notification
     * @param alert The budget alert to send as notification
     */
    suspend fun sendBudgetAlert(alert: BudgetAlertManager.BudgetAlert) {
        val notification = BudgetNotification(
            id = generateNotificationId(alert),
            title = generateNotificationTitle(alert.alertLevel, alert.categoryName),
            message = alert.message,
            alertLevel = alert.alertLevel,
            categoryId = alert.categoryId,
            categoryName = alert.categoryName,
            timestamp = alert.timestamp
        )
        
        // Avoid duplicate notifications for the same category and alert level
        val existingNotification = _activeNotifications.find { 
            it.categoryId == alert.categoryId && 
            it.alertLevel == alert.alertLevel &&
            !it.isDismissed
        }
        
        if (existingNotification == null) {
            _activeNotifications.add(notification)
            _notifications.emit(notification)
        }
    }
    
    /**
     * Send multiple budget alerts as notifications
     * @param alerts List of budget alerts to send
     */
    suspend fun sendBudgetAlerts(alerts: List<BudgetAlertManager.BudgetAlert>) {
        alerts.forEach { alert ->
            sendBudgetAlert(alert)
        }
    }
    
    /**
     * Mark a notification as read
     * @param notificationId The notification ID
     */
    fun markAsRead(notificationId: String) {
        val notification = _activeNotifications.find { it.id == notificationId }
        notification?.let { 
            val index = _activeNotifications.indexOf(it)
            _activeNotifications[index] = it.copy(isRead = true)
        }
    }
    
    /**
     * Dismiss a notification
     * @param notificationId The notification ID
     */
    fun dismissNotification(notificationId: String) {
        val notification = _activeNotifications.find { it.id == notificationId }
        notification?.let { 
            val index = _activeNotifications.indexOf(it)
            _activeNotifications[index] = it.copy(isDismissed = true)
        }
    }
    
    /**
     * Get all active (non-dismissed) notifications
     * @return List of active notifications
     */
    fun getActiveNotifications(): List<BudgetNotification> {
        return _activeNotifications.filter { !it.isDismissed }
    }
    
    /**
     * Get unread notifications
     * @return List of unread notifications
     */
    fun getUnreadNotifications(): List<BudgetNotification> {
        return _activeNotifications.filter { !it.isRead && !it.isDismissed }
    }
    
    /**
     * Clear all notifications for a specific category
     * @param categoryId The category ID
     */
    fun clearCategoryNotifications(categoryId: String) {
        _activeNotifications.removeAll { it.categoryId == categoryId }
    }
    
    /**
     * Clear all notifications
     */
    fun clearAllNotifications() {
        _activeNotifications.clear()
    }
    
    /**
     * Generate a unique notification ID
     */
    private fun generateNotificationId(alert: BudgetAlertManager.BudgetAlert): String {
        return "budget_alert_${alert.categoryId}_${alert.alertLevel.name}_${System.currentTimeMillis()}"
    }
    
    /**
     * Generate notification title based on alert level
     */
    private fun generateNotificationTitle(alertLevel: AlertLevel, categoryName: String): String {
        return when (alertLevel) {
            AlertLevel.WARNING -> "Budget Warning: $categoryName"
            AlertLevel.CRITICAL -> "Budget Critical: $categoryName"
            AlertLevel.OVER_BUDGET -> "Over Budget: $categoryName"
            AlertLevel.NORMAL -> "Budget Update: $categoryName"
        }
    }
}