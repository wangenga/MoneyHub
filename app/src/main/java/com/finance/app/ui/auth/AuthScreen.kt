package com.finance.app.ui.auth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

/**
 * Main authentication screen that wraps the auth navigation with error handling
 */
@Composable
fun AuthScreen(
    onAuthenticationComplete: () -> Unit,
    onGoogleSignIn: () -> Unit,
    startWithBiometric: Boolean = false
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val startDestination = if (startWithBiometric) {
        AuthRoutes.BIOMETRIC_LOCK
    } else {
        AuthRoutes.LOGIN
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AuthNavigation(
                navController = navController,
                startDestination = startDestination,
                onAuthenticationComplete = onAuthenticationComplete,
                onGoogleSignIn = onGoogleSignIn
            )
        }
    }
}
