package com.finance.app.domain.usecase

import com.finance.app.domain.sync.SyncScheduler
import javax.inject.Inject

/**
 * Use case for managing sync scheduling operations
 * Provides a clean interface for UI components to interact with sync scheduling
 */
class SyncSchedulingUseCase @Inject constructor(
    private val syncScheduler: SyncScheduler
) {
    
    /**
     * Schedule periodic background sync (24-hour intervals)
     */
    fun schedulePeriodicSync() {
        syncScheduler.schedulePeriodicSync()
    }
    
    /**
     * Schedule immediate foreground sync
     */
    fun scheduleForegroundSync() {
        syncScheduler.scheduleForegroundSync()
    }
    
    /**
     * Schedule sync after transaction operations (within 30 seconds)
     */
    fun schedulePostOperationSync() {
        syncScheduler.schedulePostOperationSync()
    }
    
    /**
     * Cancel all scheduled sync work
     */
    fun cancelAllSync() {
        syncScheduler.cancelAllSync()
    }
    
    /**
     * Cancel periodic sync only
     */
    fun cancelPeriodicSync() {
        syncScheduler.cancelPeriodicSync()
    }
    
    /**
     * Check if periodic sync is currently scheduled
     */
    suspend fun isPeriodicSyncScheduled(): Boolean {
        return syncScheduler.isPeriodicSyncScheduled()
    }
}