package com.finance.app.di

import android.content.Context
import androidx.room.Room
import com.finance.app.data.local.FinanceDatabase
import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideFinanceDatabase(
        @ApplicationContext context: Context
    ): FinanceDatabase {
        return Room.databaseBuilder(
            context,
            FinanceDatabase::class.java,
            FinanceDatabase.DATABASE_NAME
        ).build()
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
