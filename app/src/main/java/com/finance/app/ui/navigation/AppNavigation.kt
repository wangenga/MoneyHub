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
import com.finance.app.ui.navigation.AppNavigationViewModel
import com.finance.app.ui.onboarding.OnboardingScreen

/**
 * Root navigation component that handles onboarding, authentication flow and main app navigation
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: AppNavigationViewModel = hiltViewModel()
) {
    val navigationState by viewModel.navigationState.collectAsState(initial = NavigationState.ONBOARDING)
    val isAuthenticated by viewModel.isAuthenticated.collectAsState(initial = false)
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    
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
            NavigationState.MAIN -> NavigationRoutes.MAIN_GRAPH
        }
    ) {
        // Onboarding flow
        composable(NavigationRoutes.ONBOARDING) {
            OnboardingScreen(
                onNavigateToAuth = {
                    navController.navigate(NavigationRoutes.AUTH_GRAPH) {
                        popUpTo(NavigationRoutes.ONBOARDING) {
                            inclusive = true
                        }
                    }
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
                        // TODO: Implement Google Sign-In flow
                        android.util.Log.d("AppNavigation", "Google Sign-In requested")
                    }
                )
            }
            
            composable(NavigationRoutes.BIOMETRIC_LOCK) {
                BiometricLockScreen(
                    onAuthenticationSuccess = {
                        navController.navigate(NavigationRoutes.MAIN_GRAPH) {
                            popUpTo(NavigationRoutes.AUTH_GRAPH) {
                                inclusive = true
                            }
                        }
                    },
                    onAuthenticationError = { error ->
                        android.util.Log.e("AppNavigation", "Biometric authentication failed: $error")
                        // Stay on biometric lock screen or navigate to login
                        navController.navigate(NavigationRoutes.LOGIN) {
                            popUpTo(NavigationRoutes.BIOMETRIC_LOCK) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }
        
        // Main app flow
        composable(NavigationRoutes.MAIN_GRAPH) {
            MainNavigation()
        }
    }
}