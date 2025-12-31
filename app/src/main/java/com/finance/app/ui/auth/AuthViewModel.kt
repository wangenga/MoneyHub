package com.finance.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()
    
    private val _registerState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val registerState: StateFlow<AuthUiState> = _registerState.asStateFlow()
    
    fun signInWithEmail(email: String, password: String) {
        _loginState.value = AuthUiState.Loading
        
        viewModelScope.launch {
            try {
                val result = authRepository.signInWithEmail(email, password)
                
                result.fold(
                    onSuccess = { user ->
                        _loginState.value = AuthUiState.Success("Login successful!")
                    },
                    onFailure = { exception ->
                        _loginState.value = AuthUiState.Error(
                            exception.message ?: "Login failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _loginState.value = AuthUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
    
    fun signUp(email: String, password: String) {
        _registerState.value = AuthUiState.Loading
        
        viewModelScope.launch {
            try {
                val result = authRepository.signUp(email, password)
                
                result.fold(
                    onSuccess = { user ->
                        _registerState.value = AuthUiState.Success("Registration successful!")
                    },
                    onFailure = { exception ->
                        _registerState.value = AuthUiState.Error(
                            exception.message ?: "Registration failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _registerState.value = AuthUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
    
    fun signInWithGoogle(idToken: String) {
        _loginState.value = AuthUiState.Loading
        
        viewModelScope.launch {
            try {
                val result = authRepository.signInWithGoogle(idToken)
                
                result.fold(
                    onSuccess = { user ->
                        _loginState.value = AuthUiState.Success("Google sign-in successful!")
                    },
                    onFailure = { exception ->
                        _loginState.value = AuthUiState.Error(
                            exception.message ?: "Google sign-in failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _loginState.value = AuthUiState.Error(
                    e.message ?: "An unexpected error occurred"
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
    
    // Validation methods that the screens expect
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