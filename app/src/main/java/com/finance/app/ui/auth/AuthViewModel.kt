package com.finance.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.model.User
import com.finance.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screens (Login and Register)
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val registerState: StateFlow<AuthUiState> = _registerState.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthUiState.Loading
            
            authRepository.signInWithEmail(email, password)
                .onSuccess { user ->
                    _loginState.value = AuthUiState.Success(user)
                }
                .onFailure { exception ->
                    _loginState.value = AuthUiState.Error(
                        exception.message ?: "Login failed. Please check your credentials."
                    )
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = AuthUiState.Loading
            
            authRepository.signInWithGoogle(idToken)
                .onSuccess { user ->
                    _loginState.value = AuthUiState.Success(user)
                }
                .onFailure { exception ->
                    _loginState.value = AuthUiState.Error(
                        exception.message ?: "Google Sign-In failed. Please try again."
                    )
                }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = AuthUiState.Loading
            
            authRepository.signUp(email, password)
                .onSuccess { user ->
                    _registerState.value = AuthUiState.Success(user)
                }
                .onFailure { exception ->
                    _registerState.value = AuthUiState.Error(
                        exception.message ?: "Registration failed. Please try again."
                    )
                }
        }
    }

    fun resetLoginState() {
        _loginState.value = AuthUiState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = AuthUiState.Idle
    }

    fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Please confirm your password"
            password != confirmPassword -> "Passwords do not match"
            else -> null
        }
    }
}

/**
 * UI state for authentication operations
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
