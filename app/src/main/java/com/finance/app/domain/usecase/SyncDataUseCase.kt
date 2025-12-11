package com.finance.app.domain.usecase

import com.finance.app.domain.repository.SyncRepository
import com.finance.app.domain.repository.SyncState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for synchronizing data between local database and Firestore
 */
class SyncDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    
    /**
     * Sync all data (transactions and categories)
     */
    suspend fun syncAll(): Result<Unit> {
        return syncRepository.syncAll()
    }
    
    /**
     * Force a complete sync ignoring last sync timestamp
     */
    suspend fun forceSyncAll(): Result<Unit> {
        return syncRepository.forceSyncAll()
    }
    
    /**
     * Sync only transactions
     */
    suspend fun syncTransactions(): Result<Unit> {
        return syncRepository.syncTransactions()
    }
    
    /**
     * Sync only categories
     */
    suspend fun syncCategories(): Result<Unit> {
        return syncRepository.syncCategories()
    }
    
    /**
     * Observe sync status
     */
    fun observeSyncStatus(): Flow<SyncState> {
        return syncRepository.getSyncStatus()
    }
    
    /**
     * Get last successful sync timestamp
     */
    suspend fun getLastSyncTimestamp(): Long? {
        return syncRepository.getLastSyncTimestamp()
    }
}