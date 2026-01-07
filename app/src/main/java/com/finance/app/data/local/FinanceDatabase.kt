package com.finance.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
    version = 2,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "finance_database"

        /**
         * Migration from version 1 to 2: Add categoryType column to categories table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add categoryType column with default value "EXPENSE"
                database.execSQL("ALTER TABLE categories ADD COLUMN categoryType TEXT NOT NULL DEFAULT 'EXPENSE'")
                
                // Create index for categoryType queries
                database.execSQL("CREATE INDEX IF NOT EXISTS index_categories_categoryType ON categories(categoryType)")
                
                // Update existing categories to have EXPENSE type (backward compatibility)
                database.execSQL("UPDATE categories SET categoryType = 'EXPENSE' WHERE categoryType IS NULL OR categoryType = ''")
            }
        }

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
                .addMigrations(MIGRATION_1_2)
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