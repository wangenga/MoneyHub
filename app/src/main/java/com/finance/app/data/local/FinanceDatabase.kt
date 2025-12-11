package com.finance.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.local.dao.UserDao
import com.finance.app.data.local.entity.CategoryEntity
import com.finance.app.data.local.entity.TransactionEntity
import com.finance.app.data.local.entity.UserEntity

/**
 * Room database for the Finance App
 * Manages local data persistence with SQLite
 */
@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        CategoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "finance_database"
    }
}
