package com.finance.app.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Navigation routes for authentication screens
 */
object AuthRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val BIOMETRIC_LOCK = "biometric_lock"
}

/**
 * Navigation graph for authentication flow
 */
@Composable
fun AuthNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AuthRoutes.LOGIN,
    onAuthenticationComplete: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AuthRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = onAuthenticationComplete,
                onNavigateToRegister = {
                    navController.navigate(AuthRoutes.REGISTER)
                },
                onGoogleSignIn = onGoogleSignIn
            )
        }
        
        composable(AuthRoutes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    // Navigate back to login after successful registration
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(AuthRoutes.BIOMETRIC_LOCK) {
            BiometricLockScreen(
                onAuthenticationSuccess = onAuthenticationComplete,
                onAuthenticationError = { errorMessage ->
                    // Handle biometric error - could show a snackbar or dialog
                    // For now, just log or ignore
                }
            )
        }
    }
}
