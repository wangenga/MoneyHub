package com.finance.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.biometric.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for biometric settings
 */
@HiltViewModel
class BiometricSettingsViewModel @Inject constructor(
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {

    private val _isBiometricAvailable = MutableStateFlow(false)
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable.asStateFlow()
    
    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()
    
    private val _authenticationState = MutableStateFlow<BiometricAuthState>(BiometricAuthState.Idle)
    val authenticationState: StateFlow<BiometricAuthState> = _authenticationState.asStateFlow()

    init {
        checkBiometricAvailability()
        loadBiometricPreference()
    }

    private fun checkBiometricAvailability() {
        _isBiometricAvailable.value = biometricAuthenticator.isBiometricAvailable()
    }
    
    private fun loadBiometricPreference() {
        _isBiometricEnabled.value = biometricAuthenticator.isBiometricEnabled()
    }

    fun enableBiometric() {
        viewModelScope.launch {
            _authenticationState.value = BiometricAuthState.Loading
            
            // Authenticate before enabling
            biometricAuthenticator.authenticate()
                .onSuccess { authenticated ->
                    if (authenticated) {
                        biometricAuthenticator.setBiometricEnabled(true)
                        _isBiometricEnabled.value = true
                        _authenticationState.value = BiometricAuthState.Success
                    } else {
                        _authenticationState.value = BiometricAuthState.Error("Authentication cancelled")
                    }
                }
                .onFailure { exception ->
                    _authenticationState.value = BiometricAuthState.Error(
                        exception.message ?: "Failed to enable biometric lock"
                    )
                }
        }
    }
    
    fun disableBiometric() {
        viewModelScope.launch {
            biometricAuthenticator.setBiometricEnabled(false)
            _isBiometricEnabled.value = false
        }
    }
    
    fun resetAuthState() {
        _authenticationState.value = BiometricAuthState.Idle
    }
}

/**
 * Authentication state for biometric settings
 */
sealed class BiometricAuthState {
    object Idle : BiometricAuthState()
    object Loading : BiometricAuthState()
    object Success : BiometricAuthState()
    data class Error(val message: String) : BiometricAuthState()
}
