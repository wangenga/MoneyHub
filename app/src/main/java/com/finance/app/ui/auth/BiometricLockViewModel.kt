package com.finance.app.ui.auth

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
 * ViewModel for biometric lock screen
 */
@HiltViewModel
class BiometricLockViewModel @Inject constructor(
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {

    private val _uiState = MutableStateFlow<BiometricLockUiState>(BiometricLockUiState.Idle)
    val uiState: StateFlow<BiometricLockUiState> = _uiState.asStateFlow()

    fun authenticate() {
        viewModelScope.launch {
            _uiState.value = BiometricLockUiState.Loading
            
            biometricAuthenticator.authenticate()
                .onSuccess { authenticated ->
                    if (authenticated) {
                        _uiState.value = BiometricLockUiState.Success
                    } else {
                        _uiState.value = BiometricLockUiState.Error("Authentication cancelled")
                    }
                }
                .onFailure { exception ->
                    _uiState.value = BiometricLockUiState.Error(
                        exception.message ?: "Authentication failed"
                    )
                }
        }
    }
}

/**
 * UI state for biometric lock screen
 */
sealed class BiometricLockUiState {
    object Idle : BiometricLockUiState()
    object Loading : BiometricLockUiState()
    object Success : BiometricLockUiState()
    data class Error(val message: String) : BiometricLockUiState()
}
