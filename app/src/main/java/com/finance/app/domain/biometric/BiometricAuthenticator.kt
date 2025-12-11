package com.finance.app.domain.biometric

/**
 * Interface for biometric authentication operations
 */
interface BiometricAuthenticator {
    /**
     * Authenticates the user using biometric credentials
     * @return Result with true if authentication successful, false otherwise
     */
    suspend fun authenticate(): Result<Boolean>
    
    /**
     * Checks if biometric authentication is available on the device
     * @return true if biometric hardware is available and enrolled, false otherwise
     */
    fun isBiometricAvailable(): Boolean
    
    /**
     * Checks if biometric authentication is enabled in app settings
     * @return true if user has enabled biometric lock, false otherwise
     */
    fun isBiometricEnabled(): Boolean
    
    /**
     * Enables or disables biometric authentication in app settings
     * @param enabled true to enable, false to disable
     */
    suspend fun setBiometricEnabled(enabled: Boolean)
}
