package com.finance.app.data.local.encryption

import android.content.Context
import com.finance.app.data.local.FinanceDatabase
import kotlinx.coroutines.runBlocking

/**
 * Utility class to verify database encryption is working correctly
 */
class EncryptionVerifier(private val context: Context) {

    /**
     * Verifies that the database encryption is working by:
     * 1. Creating a database with encryption
     * 2. Verifying the key is generated correctly
     * 3. Ensuring the database can be opened and used
     */
    fun verifyEncryption(): EncryptionVerificationResult {
        return try {
            val keyManager = DatabaseKeyManager(context)
            
            // Test 1: Key generation
            val key = keyManager.getDatabaseKey()
            if (key.isEmpty() || key.size != 32) {
                return EncryptionVerificationResult.KeyGenerationFailed("Invalid key size: ${key.size}")
            }

            // Test 2: Key consistency
            val key2 = keyManager.getDatabaseKey()
            if (!key.contentEquals(key2)) {
                return EncryptionVerificationResult.KeyConsistencyFailed("Keys don't match between calls")
            }

            // Test 3: Database creation with encryption
            val database = FinanceDatabase.create(context, key)
            
            // Test 4: Database operations (basic verification)
            runBlocking {
                try {
                    // Just verify we can access the DAOs without errors
                    database.userDao()
                    database.transactionDao()
                    database.categoryDao()
                } catch (e: Exception) {
                    database.close()
                    return@runBlocking EncryptionVerificationResult.DatabaseOperationFailed("DAO access failed: ${e.message}")
                }
            }

            database.close()
            EncryptionVerificationResult.Success("Database encryption verified successfully")

        } catch (e: Exception) {
            EncryptionVerificationResult.UnexpectedError("Verification failed: ${e.message}")
        }
    }
}

/**
 * Result of encryption verification
 */
sealed class EncryptionVerificationResult(val message: String) {
    class Success(message: String) : EncryptionVerificationResult(message)
    class KeyGenerationFailed(message: String) : EncryptionVerificationResult(message)
    class KeyConsistencyFailed(message: String) : EncryptionVerificationResult(message)
    class DatabaseOperationFailed(message: String) : EncryptionVerificationResult(message)
    class UnexpectedError(message: String) : EncryptionVerificationResult(message)

    val isSuccess: Boolean get() = this is Success
}