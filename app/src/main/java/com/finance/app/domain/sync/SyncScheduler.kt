package com.finance.app.domain.sync

/**
 * Interface for scheduling synchronization operations
 * Handles periodic, foreground, and post-operation sync scheduling
 */
interface SyncScheduler {
    
    /**
     * Schedule periodic background sync (every 24 hours)
     * Uses WorkManager with network and battery optimization constraints
     */
    fun schedulePeriodicSync()
    
    /**
     * Schedule immediate foreground sync when app comes to foreground
     * Triggered when app becomes active and network is available
     */
    fun scheduleForegroundSync()
    
    /**
     * Schedule sync after transaction operations (within 30 seconds)
     * Used after create/update/delete operations to sync changes quickly
     */
    fun schedulePostOperationSync()
    
    /**
     * Cancel all scheduled sync work
     * Used when user logs out or app is being destroyed
     */
    fun cancelAllSync()
    
    /**
     * Cancel periodic sync only
     * Used when user disables background sync in settings
     */
    fun cancelPeriodicSync()
    
    /**
     * Check if periodic sync is currently scheduled
     * @return true if periodic sync is scheduled, false otherwise
     */
    suspend fun isPeriodicSyncScheduled(): Boolean
}