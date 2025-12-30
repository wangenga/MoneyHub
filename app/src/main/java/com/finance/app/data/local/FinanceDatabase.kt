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
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

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
         * Creates an encrypted Room database instance using SQLCipher
         */
        fun create(
            context: android.content.Context,
            encryptionKey: ByteArray
        ): FinanceDatabase {
            // Convert byte array to passphrase for SQLCipher
            val passphrase = net.zetetic.database.sqlcipher.SQLiteDatabase.getBytes(
                android.util.Base64.encodeToString(encryptionKey, android.util.Base64.DEFAULT).toCharArray()
            )

            // Create SQLCipher support factory
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(
                context,
                FinanceDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Database created successfully with encryption
                    }
                })
                .build()
        }
    }
}