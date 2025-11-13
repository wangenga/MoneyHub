package com.finance.app.data.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.finance.app.domain.biometric.BiometricAuthenticator
import com.finance.app.util.ActivityProvider
import com.finance.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Implementation of BiometricAuthenticator using AndroidX Biometric library
 */
@Singleton
class BiometricAuthenticatorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BiometricAuthenticator {

    private val biometricManager = BiometricManager.from(context)
    
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        EncryptedSharedPreferences.create(
            context,
            Constants.PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    private var failedAttempts = 0

    override suspend fun authenticate(): Result<Boolean> = suspendCancellableCoroutine { continuation ->
        val activity = ActivityProvider.getActivity()
        
        if (activity == null) {
            continuation.resume(Result.failure(IllegalStateException("Activity not available")))
            return@suspendCancellableCoroutine
        }

        val executor = ContextCompat.getMainExecutor(context)
        
        // Determine authenticators based on failed attempts
        val authenticators = if (failedAttempts >= Constants.MAX_BIOMETRIC_ATTEMPTS) {
            // After max attempts, require device credential
            DEVICE_CREDENTIAL
        } else {
            // Allow biometric or device credential
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        }
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Finance App")
            .setSubtitle("Authenticate to access your financial data")
            .setAllowedAuthenticators(authenticators)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED) {
                        continuation.resume(Result.success(false))
                    } else {
                        continuation.resume(
                            Result.failure(Exception("Authentication error: $errString"))
                        )
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    failedAttempts = 0 // Reset failed attempts on success
                    continuation.resume(Result.success(true))
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    failedAttempts++
                    // Don't resume here, let user try again or cancel
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
        
        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }
    }

    override fun isBiometricAvailable(): Boolean {
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    override fun isBiometricEnabled(): Boolean {
        return encryptedPrefs.getBoolean(Constants.KEY_BIOMETRIC_ENABLED, false)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(Constants.KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }
}
