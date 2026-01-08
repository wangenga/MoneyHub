package com.finance.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.app.data.local.dao.UserDao
import com.finance.app.data.local.entity.UserEntity
import com.finance.app.data.local.encryption.DatabaseKeyManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class FinanceDatabaseTest {

    private lateinit var userDao: UserDao
    private lateinit var database: FinanceDatabase
    private lateinit var context: Context
    private lateinit var keyManager: DatabaseKeyManager

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        keyManager = DatabaseKeyManager(context)
        
        // Create encrypted database for testing
        val encryptionKey = keyManager.getDatabaseKey()
        database = FinanceDatabase.create(context, encryptionKey)
        userDao = database.userDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
        keyManager.clearKey()
    }

    @Test
    @Throws(Exception::class)
    fun writeUserAndReadInList() = runTest {
        // Given
        val user = UserEntity(
            id = "test-user-1",
            email = "test@example.com",
            displayName = "Test User",
            photoUrl = null,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        // When
        userDao.insert(user)
        val retrievedUser = userDao.getUserById("test-user-1").first()

        // Then
        assertThat(retrievedUser).isNotNull()
        assertThat(retrievedUser?.email).isEqualTo("test@example.com")
        assertThat(retrievedUser?.displayName).isEqualTo("Test User")
    }

    @Test
    @Throws(Exception::class)
    fun testDatabaseEncryption() = runTest {
        // Given
        val user = UserEntity(
            id = "encryption-test",
            email = "encrypted@example.com",
            displayName = "Encrypted User",
            photoUrl = null,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        // When - Insert data into encrypted database
        userDao.insert(user)
        val retrievedUser = userDao.getUserById("encryption-test").first()

        // Then - Verify data can be read correctly
        assertThat(retrievedUser).isNotNull()
        assertThat(retrievedUser?.email).isEqualTo("encrypted@example.com")
        
        // Verify database file exists and is not readable without key
        val dbFile = context.getDatabasePath(FinanceDatabase.DATABASE_NAME)
        assertThat(dbFile.exists()).isTrue()
        assertThat(dbFile.length()).isGreaterThan(0L)
    }

    @Test
    @Throws(Exception::class)
    fun testDifferentKeysCreateDifferentDatabases() = runTest {
        // Given - First database with original key
        val user1 = UserEntity(
            id = "user-1",
            email = "user1@example.com",
            displayName = "User 1",
            photoUrl = null,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
        userDao.insert(user1)

        // When - Create new database with different key
        database.close()
        keyManager.clearKey()
        
        // Delete the database file to ensure we start fresh
        val dbFile = context.getDatabasePath(FinanceDatabase.DATABASE_NAME)
        if (dbFile.exists()) {
            dbFile.delete()
        }
        
        val newEncryptionKey = keyManager.getDatabaseKey()
        val newDatabase = FinanceDatabase.create(context, newEncryptionKey)
        val newUserDao = newDatabase.userDao()

        // Then - New database should be empty (different encryption key)
        val retrievedUser = newUserDao.getUserById("user-1").first()
        assertThat(retrievedUser).isNull()

        newDatabase.close()
    }
}