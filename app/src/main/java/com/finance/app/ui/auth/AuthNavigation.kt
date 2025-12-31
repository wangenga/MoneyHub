package com.finance.app.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Navigation component for authentication flow
 */
@Composable
fun AuthNavigation(
    navController: NavHostController = rememberNavController(),
    onAuthenticationComplete: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = onAuthenticationComplete,
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onGoogleSignIn = onGoogleSignIn
            )
        }
        
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = onAuthenticationComplete,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}