package com.finance.app.domain.biometric

/**
 * Interface for biometric authentication operations
 */
interface BiometricAuthenticator {
    
    /**
     * Check if biometric authentication is available on the device
     */
    fun isBiometricAvailable(): Boolean
    
    /**
     * Check if biometric authentication is enabled by the user
     */
    fun isBiometricEnabled(): Boolean
    
    /**
     * Set biometric authentication enabled/disabled
     */
    suspend fun setBiometricEnabled(enabled: Boolean)
    
    /**
     * Authenticate using biometric
     * @return Result<Boolean> - true if authentication successful, false if cancelled
     */
    suspend fun authenticate(): Result<Boolean>
}