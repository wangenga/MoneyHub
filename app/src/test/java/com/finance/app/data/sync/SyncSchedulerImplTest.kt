package com.finance.app.data.sync

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for SyncSchedulerImpl
 * Tests WorkManager integration and sync scheduling functionality
 */
@RunWith(AndroidJUnit4::class)
class SyncSchedulerImplTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var syncScheduler: SyncSchedulerImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Initialize WorkManager for testing
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
        
        syncScheduler = SyncSchedulerImpl(context)
    }

    @Test
    fun schedulePeriodicSync_schedulesWork() = runBlocking {
        // When
        syncScheduler.schedulePeriodicSync()

        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.PERIODIC_SYNC_WORK_NAME).get()
        assertThat(workInfos).isNotEmpty()
        assertThat(workInfos.first().state).isEqualTo(WorkInfo.State.ENQUEUED)
    }

    @Test
    fun scheduleForegroundSync_schedulesWork() {
        // When
        syncScheduler.scheduleForegroundSync()

        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.FOREGROUND_SYNC_WORK_NAME).get()
        assertThat(workInfos).isNotEmpty()
        assertThat(workInfos.first().state).isEqualTo(WorkInfo.State.ENQUEUED)
    }

    @Test
    fun schedulePostOperationSync_schedulesWork() {
        // When
        syncScheduler.schedulePostOperationSync()

        // Then
        val workInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.POST_OPERATION_SYNC_WORK_NAME).get()
        assertThat(workInfos).isNotEmpty()
        assertThat(workInfos.first().state).isEqualTo(WorkInfo.State.ENQUEUED)
    }

    @Test
    fun cancelAllSync_cancelsAllWork() {
        // Given
        syncScheduler.schedulePeriodicSync()
        syncScheduler.scheduleForegroundSync()
        syncScheduler.schedulePostOperationSync()

        // When
        syncScheduler.cancelAllSync()

        // Then
        val periodicWorkInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.PERIODIC_SYNC_WORK_NAME).get()
        val foregroundWorkInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.FOREGROUND_SYNC_WORK_NAME).get()
        val postOpWorkInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.POST_OPERATION_SYNC_WORK_NAME).get()

        // All work should be cancelled
        periodicWorkInfos.forEach { assertThat(it.state).isEqualTo(WorkInfo.State.CANCELLED) }
        foregroundWorkInfos.forEach { assertThat(it.state).isEqualTo(WorkInfo.State.CANCELLED) }
        postOpWorkInfos.forEach { assertThat(it.state).isEqualTo(WorkInfo.State.CANCELLED) }
    }

    @Test
    fun isPeriodicSyncScheduled_returnsTrueWhenScheduled() = runBlocking {
        // Given
        syncScheduler.schedulePeriodicSync()

        // When
        val isScheduled = syncScheduler.isPeriodicSyncScheduled()

        // Then
        assertThat(isScheduled).isTrue()
    }

    @Test
    fun isPeriodicSyncScheduled_returnsFalseWhenNotScheduled() = runBlocking {
        // When
        val isScheduled = syncScheduler.isPeriodicSyncScheduled()

        // Then
        assertThat(isScheduled).isFalse()
    }
}