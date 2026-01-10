package com.finance.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.biometric.BiometricAuthenticator
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for app-level navigation state
 */
@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val onboardingRepository: OnboardingRepository,
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {
    
    val isAuthenticated = authRepository.isAuthenticated()
    val currentUser = authRepository.getCurrentUser()
    
    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()
    
    private val _isBiometricUnlocked = MutableStateFlow(false)
    val isBiometricUnlocked: StateFlow<Boolean> = _isBiometricUnlocked.asStateFlow()
    
    init {
        checkOnboardingStatus()
    }
    
    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            val completed = onboardingRepository.isOnboardingCompleted()
            _isOnboardingCompleted.value = completed
        }
    }
    
    /**
     * Call this when biometric authentication succeeds
     */
    fun onBiometricUnlocked() {
        _isBiometricUnlocked.value = true
    }
    
    /**
     * Check if biometric lock should be shown
     */
    fun shouldShowBiometricLock(): Boolean {
        return biometricAuthenticator.isBiometricEnabled() && 
               biometricAuthenticator.isBiometricAvailable() &&
               !_isBiometricUnlocked.value
    }
    
    /**
     * Combined flow to determine the initial navigation destination
     */
    val navigationState = combine(
        isOnboardingCompleted,
        isAuthenticated,
        isBiometricUnlocked
    ) { onboardingCompleted, authenticated, biometricUnlocked ->
        when {
            !onboardingCompleted -> NavigationState.ONBOARDING
            !authenticated -> NavigationState.AUTH
            authenticated && biometricAuthenticator.isBiometricEnabled() && 
                biometricAuthenticator.isBiometricAvailable() && !biometricUnlocked -> NavigationState.BIOMETRIC_LOCK
            authenticated -> NavigationState.MAIN
            else -> NavigationState.AUTH
        }
    }
}

/**
 * Represents the current navigation state of the app
 */
enum class NavigationState {
    ONBOARDING,
    AUTH,
    BIOMETRIC_LOCK,
    MAIN
}