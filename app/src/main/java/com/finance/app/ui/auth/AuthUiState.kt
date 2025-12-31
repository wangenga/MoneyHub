package com.finance.app.ui.auth

/**
 * UI state for authentication screens
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String = "Authentication successful") : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/**
 * Form data for authentication screens
 */
data class AuthFormData(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val name: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false
) {
    val isLoginFormValid: Boolean
        get() = email.isNotBlank() && password.isNotBlank()
    
    val isRegisterFormValid: Boolean
        get() = email.isNotBlank() && 
                password.isNotBlank() && 
                confirmPassword.isNotBlank() && 
                name.isNotBlank() &&
                password == confirmPassword
}