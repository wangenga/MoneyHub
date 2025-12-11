package com.finance.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for synchronization operations
 * Handles syncing data between local database and Firestore
 */
interface SyncRepository {
    
    /**
     * Sync all data (transactions and categories) for the current user
     * @return Result indicating success or failure
     */
    suspend fun syncAll(): Result<Unit>
    
    /**
     * Sync transactions for the current user
     * Uploads pending local changes and downloads remote changes
     * @return Result indicating success or failure
     */
    suspend fun syncTransactions(): Result<Unit>
    
    /**
     * Sync categories for the current user
     * Uploads pending local changes and downloads remote changes
     * @return Result indicating success or failure
     */
    suspend fun syncCategories(): Result<Unit>
    
    /**
     * Get the current sync status as a Flow
     * @return Flow emitting current sync state
     */
    fun getSyncStatus(): Flow<SyncState>
    
    /**
     * Get the last successful sync timestamp
     * @return timestamp in milliseconds, or null if never synced
     */
    suspend fun getLastSyncTimestamp(): Long?
    
    /**
     * Force a full sync (ignores last sync timestamp)
     * @return Result indicating success or failure
     */
    suspend fun forceSyncAll(): Result<Unit>
}

/**
 * Represents the current state of synchronization
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val timestamp: Long) : SyncState()
    data class Error(val message: String, val retryCount: Int = 0) : SyncState()
}