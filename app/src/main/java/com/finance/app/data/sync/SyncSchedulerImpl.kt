package com.finance.app.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.finance.app.domain.sync.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SyncScheduler using WorkManager
 * Handles all sync scheduling with appropriate constraints and policies
 */
@Singleton
class SyncSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncScheduler {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        // Work constraints
        private const val PERIODIC_SYNC_INTERVAL_HOURS = 24L
        private const val POST_OPERATION_SYNC_DELAY_SECONDS = 30L
        private const val FOREGROUND_SYNC_DELAY_SECONDS = 5L // Small delay to allow UI to settle
    }

    override fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val inputData = Data.Builder()
            .putString(SyncWorker.SYNC_TYPE_KEY, SyncWorker.SYNC_TYPE_ALL)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_SYNC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            periodicSyncRequest
        )
    }

    override fun scheduleForegroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString(SyncWorker.SYNC_TYPE_KEY, SyncWorker.SYNC_TYPE_ALL)
            .build()

        val foregroundSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setInitialDelay(FOREGROUND_SYNC_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.FOREGROUND_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace any existing foreground sync
            foregroundSyncRequest
        )
    }

    override fun schedulePostOperationSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString(SyncWorker.SYNC_TYPE_KEY, SyncWorker.SYNC_TYPE_ALL)
            .build()

        val postOperationSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setInitialDelay(POST_OPERATION_SYNC_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.POST_OPERATION_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace any existing post-operation sync
            postOperationSyncRequest
        )
    }

    override fun cancelAllSync() {
        workManager.cancelUniqueWork(SyncWorker.PERIODIC_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(SyncWorker.FOREGROUND_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(SyncWorker.POST_OPERATION_SYNC_WORK_NAME)
    }

    override fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(SyncWorker.PERIODIC_SYNC_WORK_NAME)
    }

    override suspend fun isPeriodicSyncScheduled(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.PERIODIC_SYNC_WORK_NAME).get()
        return workInfos.any { workInfo ->
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
        }
    }
}