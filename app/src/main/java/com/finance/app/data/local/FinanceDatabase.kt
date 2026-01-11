package com.finance.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finance.app.data.local.dao.BudgetDao
import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.local.dao.RecurringTransactionDao
import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.local.dao.UserDao
import com.finance.app.data.local.entity.BudgetEntity
import com.finance.app.data.local.entity.CategoryEntity
import com.finance.app.data.local.entity.RecurringTransactionEntity
import com.finance.app.data.local.entity.TransactionEntity
import com.finance.app.data.local.entity.UserEntity

/**
 * Room database class with SQLCipher encryption
 */
@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

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
         * Migration from version 2 to 3: Add budgets table
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create budgets table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS budgets (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        categoryId TEXT NOT NULL,
                        monthlyLimit REAL NOT NULL,
                        month INTEGER NOT NULL,
                        year INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create indexes for performance optimization
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_userId ON budgets(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_month_year ON budgets(month, year)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_userId_month_year ON budgets(userId, month, year)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_userId_categoryId ON budgets(userId, categoryId)")
            }
        }

        /**
         * Migration from version 3 to 4: Add recurring_transactions table
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create recurring_transactions table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recurring_transactions (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        templateTransactionId TEXT NOT NULL,
                        templateType TEXT NOT NULL,
                        templateAmount REAL NOT NULL,
                        templateCategoryId TEXT NOT NULL,
                        templatePaymentMethod TEXT,
                        templateNotes TEXT,
                        recurrencePattern TEXT NOT NULL,
                        nextDueDate INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create indexes for performance optimization
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_userId ON recurring_transactions(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_nextDueDate ON recurring_transactions(nextDueDate)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_isActive ON recurring_transactions(isActive)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_userId_isActive ON recurring_transactions(userId, isActive)")
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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