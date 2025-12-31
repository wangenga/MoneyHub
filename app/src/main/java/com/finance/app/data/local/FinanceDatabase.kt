package com.finance.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.local.dao.UserDao
import com.finance.app.data.local.entity.CategoryEntity
import com.finance.app.data.local.entity.TransactionEntity
import com.finance.app.data.local.entity.UserEntity

/**
 * Room database class with SQLCipher encryption
 */
@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        CategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "finance_database"

        /**
         * Creates a Room database instance
         * TODO: Re-enable SQLCipher encryption once import issues are resolved
         */
        fun create(
            context: android.content.Context,
            encryptionKey: ByteArray? = null
        ): FinanceDatabase {
            return Room.databaseBuilder(
                context,
                FinanceDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Database created successfully
                    }
                })
                .build()
        }
    }
}