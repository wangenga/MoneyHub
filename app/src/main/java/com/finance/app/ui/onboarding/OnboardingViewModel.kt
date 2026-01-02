package com.finance.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.model.OnboardingPages
import com.finance.app.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing onboarding flow state
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    val pages = OnboardingPages.pages
    
    init {
        checkOnboardingStatus()
    }
    
    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            val isCompleted = onboardingRepository.isOnboardingCompleted()
            _uiState.value = _uiState.value.copy(isOnboardingCompleted = isCompleted)
        }
    }
    
    fun nextPage() {
        val currentPage = _uiState.value.currentPage
        if (currentPage < pages.size - 1) {
            _uiState.value = _uiState.value.copy(currentPage = currentPage + 1)
        }
    }
    
    fun previousPage() {
        val currentPage = _uiState.value.currentPage
        if (currentPage > 0) {
            _uiState.value = _uiState.value.copy(currentPage = currentPage - 1)
        }
    }
    
    fun skipOnboarding() {
        viewModelScope.launch {
            onboardingRepository.setOnboardingCompleted()
            _uiState.value = _uiState.value.copy(
                isOnboardingCompleted = true,
                shouldNavigateToAuth = true
            )
        }
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingRepository.setOnboardingCompleted()
            _uiState.value = _uiState.value.copy(
                isOnboardingCompleted = true,
                shouldNavigateToAuth = true
            )
        }
    }
    
    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(shouldNavigateToAuth = false)
    }
}

/**
 * UI state for onboarding flow
 */
data class OnboardingUiState(
    val currentPage: Int = 0,
    val isOnboardingCompleted: Boolean = false,
    val shouldNavigateToAuth: Boolean = false
)