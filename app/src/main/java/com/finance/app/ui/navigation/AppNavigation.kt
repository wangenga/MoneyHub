package com.finance.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.finance.app.ui.auth.AuthNavigation
import com.finance.app.ui.auth.BiometricLockScreen

/**
 * Root navigation component that handles onboarding, authentication flow and main app navigation
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: AppNavigationViewModel = hiltViewModel()
) {
    val navigationState by viewModel.navigationState.collectAsState(initial = NavigationState.ONBOARDING)
    
    // Navigate based on navigation state
    LaunchedEffect(navigationState) {
        when (navigationState) {
            NavigationState.ONBOARDING -> {
                navController.navigate(NavigationRoutes.ONBOARDING) {
                    popUpTo(0) { inclusive = true }
                }
            }
            NavigationState.AUTH -> {
                navController.navigate(NavigationRoutes.AUTH_GRAPH) {
                    popUpTo(0) { inclusive = true }
                }
            }
            NavigationState.BIOMETRIC_LOCK -> {
                navController.navigate(NavigationRoutes.BIOMETRIC_LOCK) {
                    popUpTo(0) { inclusive = true }
                }
            }
            NavigationState.MAIN -> {
                navController.navigate(NavigationRoutes.MAIN_GRAPH) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = when (navigationState) {
            NavigationState.ONBOARDING -> NavigationRoutes.ONBOARDING
            NavigationState.AUTH -> NavigationRoutes.AUTH_GRAPH
            NavigationState.BIOMETRIC_LOCK -> NavigationRoutes.BIOMETRIC_LOCK
            NavigationState.MAIN -> NavigationRoutes.MAIN_GRAPH
        }
    ) {
        // Onboarding flow
        composable(NavigationRoutes.ONBOARDING) {
            com.finance.app.ui.onboarding.OnboardingScreen(
                onNavigateToAuth = {
                    navController.navigate(NavigationRoutes.AUTH_GRAPH) {
                        popUpTo(NavigationRoutes.ONBOARDING) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        
        // Biometric lock screen (shown when user is logged in but needs to unlock)
        composable(NavigationRoutes.BIOMETRIC_LOCK) {
            BiometricLockScreen(
                onAuthenticationSuccess = {
                    viewModel.onBiometricUnlocked()
                },
                onAuthenticationError = { error ->
                    android.util.Log.e("AppNavigation", "Biometric authentication failed: $error")
                    // Stay on biometric lock screen - user must authenticate
                }
            )
        }
        
        // Authentication flow
        navigation(
            startDestination = NavigationRoutes.LOGIN,
            route = NavigationRoutes.AUTH_GRAPH
        ) {
            composable(NavigationRoutes.LOGIN) {
                AuthNavigation(
                    onAuthenticationComplete = {
                        // Authentication handled by LaunchedEffect above
                    },
                    onGoogleSignIn = {
                        android.util.Log.d("AppNavigation", "Google Sign-In requested")
                    }
                )
            }
        }
        
        // Main app flow
        composable(NavigationRoutes.MAIN_GRAPH) {
            MainNavigation(
                onLogout = {
                    // Navigate to auth graph and clear back stack
                    navController.navigate(NavigationRoutes.AUTH_GRAPH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}