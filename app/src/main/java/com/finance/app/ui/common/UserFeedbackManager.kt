package com.finance.app.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manages user feedback through Snackbars with consistent behavior
 */
class UserFeedbackManager(
    private val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope
) {
    
    /**
     * Shows a success message
     */
    fun showSuccess(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration
            )
        }
    }
    
    /**
     * Shows an error message with optional retry action
     */
    fun showError(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Long,
        onAction: (() -> Unit)? = null
    ) {
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = duration
            )
            
            if (result == SnackbarResult.ActionPerformed && onAction != null) {
                onAction()
            }
        }
    }
    
    /**
     * Shows a retryable error message
     */
    fun showRetryableError(
        message: String,
        retryLabel: String = "Retry",
        onRetry: () -> Unit
    ) {
        showError(
            message = message,
            actionLabel = retryLabel,
            duration = SnackbarDuration.Indefinite,
            onAction = onRetry
        )
    }
    
    /**
     * Shows an offline indicator
     */
    fun showOfflineMessage(
        message: String = "You're offline. Changes will sync when connection is restored.",
        duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration
            )
        }
    }
    
    /**
     * Shows a loading message (typically used for long operations)
     */
    fun showLoading(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Indefinite
    ) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration
            )
        }
    }
    
    /**
     * Dismisses the current snackbar
     */
    fun dismiss() {
        snackbarHostState.currentSnackbarData?.dismiss()
    }
}