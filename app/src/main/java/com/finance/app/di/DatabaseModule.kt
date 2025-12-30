package com.finance.app.di

import android.content.Context
import com.finance.app.data.local.FinanceDatabase
import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.local.dao.UserDao
import com.finance.app.data.local.encryption.DatabaseKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies with SQLCipher encryption
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFinanceDatabase(
        @ApplicationContext context: Context,
        keyManager: DatabaseKeyManager
    ): FinanceDatabase {
        val encryptionKey = keyManager.getDatabaseKey()
        return FinanceDatabase.create(context, encryptionKey)
    }

    @Provides
    fun provideUserDao(database: FinanceDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideTransactionDao(database: FinanceDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: FinanceDatabase): CategoryDao {
        return database.categoryDao()
    }
}