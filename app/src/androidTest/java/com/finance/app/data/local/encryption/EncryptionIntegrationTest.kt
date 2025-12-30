package com.finance.app.data.local.encryption

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.app.data.local.entity.UserEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptionIntegrationTest {

    private lateinit var context: Context
    private lateinit var verifier: EncryptionVerifier
    private lateinit var keyManager: DatabaseKeyManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        verifier = EncryptionVerifier(context)
        keyManager = DatabaseKeyManager(context)
    }

    @After
    fun cleanup() {
        keyManager.clearKey()
    }

    @Test
    fun testCompleteEncryptionFlow() {
        // When
        val result = verifier.verifyEncryption()

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.message).contains("successfully")
    }

    @Test
    fun testEncryptedDatabaseOperations() = runTest {
        // Given
        val key = keyManager.getDatabaseKey()
        val database = com.finance.app.data.local.FinanceDatabase.create(context, key)
        val userDao = database.userDao()

        val testUser = UserEntity(
            id = "test-encrypted-user",
            email = "encrypted@test.com",
            displayName = "Encrypted Test User",
            photoUrl = null,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        // When
        userDao.insert(testUser)
        val retrievedUser = userDao.getUserById("test-encrypted-user").first()

        // Then
        assertThat(retrievedUser).isNotNull()
        assertThat(retrievedUser?.email).isEqualTo("encrypted@test.com")
        assertThat(retrievedUser?.displayName).isEqualTo("Encrypted Test User")

        database.close()
    }

    @Test
    fun testKeyPersistence() {
        // Given
        val originalKey = keyManager.getDatabaseKey()

        // When - Create new key manager instance (simulating app restart)
        val newKeyManager = DatabaseKeyManager(context)
        val persistedKey = newKeyManager.getDatabaseKey()

        // Then
        assertThat(persistedKey).isEqualTo(originalKey)
    }
}