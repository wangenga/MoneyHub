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

/**
 * Root navigation component that handles authentication flow and main app navigation
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: AppNavigationViewModel = hiltViewModel()
) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState(initial = false)
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    
    // Navigate based on authentication state
    LaunchedEffect(isAuthenticated, currentUser) {
        if (isAuthenticated && currentUser != null) {
            // User is authenticated, navigate to main app
            navController.navigate(NavigationRoutes.MAIN_GRAPH) {
                popUpTo(NavigationRoutes.AUTH_GRAPH) {
                    inclusive = true
                }
            }
        } else {
            // User is not authenticated, navigate to auth
            navController.navigate(NavigationRoutes.AUTH_GRAPH) {
                popUpTo(NavigationRoutes.MAIN_GRAPH) {
                    inclusive = true
                }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) NavigationRoutes.MAIN_GRAPH else NavigationRoutes.AUTH_GRAPH
    ) {
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
        
        // Onboarding (future implementation)
        composable(NavigationRoutes.ONBOARDING) {
            // TODO: Implement onboarding screens
            // OnboardingNavigation()
        }
    }
}