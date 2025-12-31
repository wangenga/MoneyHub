package com.finance.app.data.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.finance.app.domain.biometric.BiometricAuthenticator
import com.finance.app.util.ActivityProvider
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
    @ApplicationContext private val context: Context,
    private val activityProvider: ActivityProvider
) : BiometricAuthenticator {
    
    companion object {
        private const val PREFS_NAME = "biometric_prefs"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
    
    private val biometricManager = BiometricManager.from(context)
    
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    override fun isBiometricAvailable(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    override fun isBiometricEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    override suspend fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }
    
    override suspend fun authenticate(): Result<Boolean> {
        if (!isBiometricAvailable()) {
            return Result.failure(Exception("Biometric authentication not available"))
        }
        
        val activity = activityProvider.getCurrentActivity() as? FragmentActivity
            ?: return Result.failure(Exception("Activity not available"))
        
        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(context)
            
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            continuation.resume(Result.success(false))
                        } else {
                            continuation.resume(Result.failure(Exception(errString.toString())))
                        }
                    }
                    
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        continuation.resume(Result.success(true))
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // Don't complete here, let user try again
                    }
                }
            )
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Use your fingerprint or face to authenticate")
                .setNegativeButtonText("Cancel")
                .build()
            
            biometricPrompt.authenticate(promptInfo)
            
            continuation.invokeOnCancellation {
                // Handle cancellation if needed
            }
        }
    }
}