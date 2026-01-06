package com.finance.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.biometric.BiometricAuthenticator
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.SettingsRepository
import com.finance.app.domain.repository.SyncState
import com.finance.app.domain.repository.SyncRepository
import com.finance.app.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    init {
        loadSettings()
        observeSyncStatus()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.getThemeMode(),
                settingsRepository.isDynamicColorEnabled(),
                settingsRepository.isBiometricLockEnabled()
            ) { themeMode, isDynamicColorEnabled, isBiometricEnabled ->
                _uiState.value = _uiState.value.copy(
                    themeMode = themeMode,
                    isDynamicColorEnabled = isDynamicColorEnabled,
                    isBiometricEnabled = isBiometricEnabled,
                    isBiometricAvailable = biometricAuthenticator.isBiometricAvailable(),
                    appVersion = settingsRepository.getAppVersion()
                )
            }.collect { }
        }
        
        // Load last sync timestamp
        viewModelScope.launch {
            val lastSync = settingsRepository.getLastSyncTimestamp()
            _uiState.value = _uiState.value.copy(lastSyncTimestamp = lastSync)
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncRepository.getSyncStatus().collect { syncState ->
                _uiState.value = _uiState.value.copy(
                    syncState = syncState,
                    isSyncing = syncState is SyncState.Syncing
                )
                
                // Update last sync timestamp when sync completes successfully
                if (syncState is SyncState.Success) {
                    _uiState.value = _uiState.value.copy(lastSyncTimestamp = syncState.timestamp)
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorEnabled(enabled)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        if (!enabled) {
            // Disable directly and update UI immediately
            viewModelScope.launch {
                settingsRepository.setBiometricLockEnabled(false)
                _uiState.value = _uiState.value.copy(isBiometricEnabled = false)
            }
        } else {
            // Enable with authentication
            enableBiometricWithAuth()
        }
    }

    private fun enableBiometricWithAuth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(biometricAuthState = BiometricAuthState.Loading)
            
            biometricAuthenticator.authenticate()
                .onSuccess { authenticated ->
                    if (authenticated) {
                        settingsRepository.setBiometricLockEnabled(true)
                        _uiState.value = _uiState.value.copy(
                            isBiometricEnabled = true,
                            biometricAuthState = BiometricAuthState.Success
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            biometricAuthState = BiometricAuthState.Error("Authentication cancelled")
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        biometricAuthState = BiometricAuthState.Error(
                            exception.message ?: "Failed to enable biometric lock"
                        )
                    )
                }
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            settingsRepository.triggerManualSync()
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        syncError = exception.message ?: "Sync failed"
                    )
                }
        }
    }

    fun clearBiometricAuthState() {
        _uiState.value = _uiState.value.copy(biometricAuthState = BiometricAuthState.Idle)
    }

    fun clearSyncError() {
        _uiState.value = _uiState.value.copy(syncError = null)
    }

    /**
     * Logs out the current user from the application.
     * Signs out from Firebase Auth and clears local session data.
     */
    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true, logoutError = null)
            try {
                authRepository.signOut()
                _logoutEvent.emit(Unit)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    logoutError = e.message ?: "Failed to log out"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoggingOut = false)
            }
        }
    }

    /**
     * Clears the logout error state
     */
    fun clearLogoutError() {
        _uiState.value = _uiState.value.copy(logoutError = null)
    }
}

/**
 * UI state for the Settings screen
 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDynamicColorEnabled: Boolean = true,
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val appVersion: String = "",
    val lastSyncTimestamp: Long? = null,
    val syncState: SyncState = SyncState.Idle,
    val isSyncing: Boolean = false,
    val biometricAuthState: BiometricAuthState = BiometricAuthState.Idle,
    val syncError: String? = null,
    val isLoggingOut: Boolean = false,
    val logoutError: String? = null
)