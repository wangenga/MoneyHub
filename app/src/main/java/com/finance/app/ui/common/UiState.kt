package com.finance.app.ui.common

/**
 * Generic UI state wrapper for handling loading, success, and error states
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(
        val message: String,
        val isRetryable: Boolean = false,
        val retryAction: (() -> Unit)? = null
    ) : UiState<Nothing>()
    
    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}

/**
 * Represents the state of an async operation
 */
sealed class AsyncState {
    object Idle : AsyncState()
    object Loading : AsyncState()
    object Success : AsyncState()
    data class Error(
        val message: String,
        val isRetryable: Boolean = false,
        val retryAction: (() -> Unit)? = null
    ) : AsyncState()
    
    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isIdle: Boolean get() = this is Idle
}

/**
 * Represents network connectivity state
 */
sealed class NetworkState {
    object Connected : NetworkState()
    object Disconnected : NetworkState()
    object Unknown : NetworkState()
    
    val isConnected: Boolean get() = this is Connected
    val isDisconnected: Boolean get() = this is Disconnected
}

/**
 * Extension functions for UiState
 */
fun <T> UiState<T>.getDataOrNull(): T? = when (this) {
    is UiState.Success -> data
    else -> null
}

fun <T> UiState<T>.getErrorOrNull(): UiState.Error? = when (this) {
    is UiState.Error -> this
    else -> null
}

/**
 * Maps UiState data using the provided transform function
 */
fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Loading -> UiState.Loading
    is UiState.Success -> UiState.Success(transform(data))
    is UiState.Error -> this
}