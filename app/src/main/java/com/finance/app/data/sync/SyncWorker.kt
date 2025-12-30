package com.finance.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finance.app.domain.usecase.SyncDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for background synchronization
 * Performs sync operations in the background with proper error handling
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncDataUseCase: SyncDataUseCase,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "sync_work"
        const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"
        const val FOREGROUND_SYNC_WORK_NAME = "foreground_sync_work"
        const val POST_OPERATION_SYNC_WORK_NAME = "post_operation_sync_work"
        
        // Input data keys
        const val SYNC_TYPE_KEY = "sync_type"
        const val SYNC_TYPE_ALL = "all"
        const val SYNC_TYPE_TRANSACTIONS = "transactions"
        const val SYNC_TYPE_CATEGORIES = "categories"
    }

    override suspend fun doWork(): Result {
        return try {
            // Check network connectivity before attempting sync
            if (!networkConnectivityChecker.isNetworkAvailable()) {
                return Result.retry()
            }

            // Get sync type from input data, default to all
            val syncType = inputData.getString(SYNC_TYPE_KEY) ?: SYNC_TYPE_ALL

            // Perform sync based on type
            val syncResult = when (syncType) {
                SYNC_TYPE_TRANSACTIONS -> syncDataUseCase.syncTransactions()
                SYNC_TYPE_CATEGORIES -> syncDataUseCase.syncCategories()
                else -> syncDataUseCase.syncAll()
            }

            if (syncResult.isSuccess) {
                Result.success()
            } else {
                // Log the error for debugging
                val exception = syncResult.exceptionOrNull()
                android.util.Log.e("SyncWorker", "Sync failed", exception)
                
                // Retry on failure (WorkManager will handle exponential backoff)
                Result.retry()
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Unexpected error during sync", e)
            Result.retry()
        }
    }
}