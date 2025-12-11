package com.finance.app.di

import com.finance.app.data.repository.AnalyticsRepositoryImpl
import com.finance.app.data.repository.AuthRepositoryImpl
import com.finance.app.data.repository.CategoryRepositoryImpl
import com.finance.app.data.repository.SyncRepositoryImpl
import com.finance.app.data.repository.TransactionRepositoryImpl
import com.finance.app.domain.repository.AnalyticsRepository
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.domain.repository.SyncRepository
import com.finance.app.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository
    
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository
    
    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        analyticsRepositoryImpl: AnalyticsRepositoryImpl
    ): AnalyticsRepository
    
    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        syncRepositoryImpl: SyncRepositoryImpl
    ): SyncRepository
}
