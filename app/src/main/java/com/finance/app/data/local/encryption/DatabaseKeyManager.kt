package com.finance.app.data.local.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages database encryption key using Android Keystore and EncryptedSharedPreferences
 */
@Singleton
class DatabaseKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEY_ALIAS = "finance_app_db_key"
        private const val PREFS_NAME = "database_key_prefs"
        private const val KEY_PREF_NAME = "encrypted_db_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Gets or generates the database encryption key
     */
    fun getDatabaseKey(): ByteArray {
        // First try to get existing key from encrypted preferences
        val existingKey = encryptedPrefs.getString(KEY_PREF_NAME, null)
        if (existingKey != null) {
            return android.util.Base64.decode(existingKey, android.util.Base64.DEFAULT)
        }

        // Generate new key if none exists
        return generateAndStoreKey()
    }

    /**
     * Generates a new encryption key using Android Keystore and stores it securely
     */
    private fun generateAndStoreKey(): ByteArray {
        try {
            // Generate key in Android Keystore
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            val secretKey = keyGenerator.generateKey()

            // Get the raw key bytes
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val keyBytes = key.encoded

            // Store the key in encrypted preferences as backup
            val encodedKey = android.util.Base64.encodeToString(keyBytes, android.util.Base64.DEFAULT)
            encryptedPrefs.edit().putString(KEY_PREF_NAME, encodedKey).apply()

            return keyBytes
        } catch (e: Exception) {
            // Fallback: generate a random key if Keystore fails
            val fallbackKey = ByteArray(32)
            java.security.SecureRandom().nextBytes(fallbackKey)
            
            val encodedKey = android.util.Base64.encodeToString(fallbackKey, android.util.Base64.DEFAULT)
            encryptedPrefs.edit().putString(KEY_PREF_NAME, encodedKey).apply()
            
            return fallbackKey
        }
    }

    /**
     * Clears the stored encryption key (for testing or reset purposes)
     */
    fun clearKey() {
        encryptedPrefs.edit().remove(KEY_PREF_NAME).apply()
        
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            // Ignore errors when clearing
        }
    }
}