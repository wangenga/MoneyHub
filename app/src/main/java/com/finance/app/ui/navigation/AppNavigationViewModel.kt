package com.finance.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {
    
    val isAuthenticated = authRepository.isAuthenticated()
    val currentUser = authRepository.getCurrentUser()
    
    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()
    
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
     * Combined flow to determine the initial navigation destination
     */
    val navigationState = combine(
        isOnboardingCompleted,
        isAuthenticated
    ) { onboardingCompleted, authenticated ->
        when {
            !onboardingCompleted -> NavigationState.ONBOARDING
            !authenticated -> NavigationState.AUTH
            else -> NavigationState.MAIN
        }
    }
}

/**
 * Represents the current navigation state of the app
 */
enum class NavigationState {
    ONBOARDING,
    AUTH,
    MAIN
}