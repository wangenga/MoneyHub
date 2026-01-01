package com.finance.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.finance.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for app-level navigation state
 */
@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    val isAuthenticated = authRepository.isAuthenticated()
    val currentUser = authRepository.getCurrentUser()
}