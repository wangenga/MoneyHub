package com.finance.app.di

import com.finance.app.data.sync.SyncSchedulerImpl
import com.finance.app.domain.sync.SyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for sync-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(
        syncSchedulerImpl: SyncSchedulerImpl
    ): SyncScheduler
}