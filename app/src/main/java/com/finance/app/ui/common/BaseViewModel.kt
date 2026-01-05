package com.finance.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.data.sync.NetworkStateObserver
import com.finance.app.util.ErrorHandler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel that provides common error handling and network state management
 */
abstract class BaseViewModel(
    protected val errorHandler: ErrorHandler,
    protected val networkStateObserver: NetworkStateObserver
) : ViewModel() {
    
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Unknown)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val _globalError = MutableStateFlow<String?>(null)
    val globalError: StateFlow<String?> = _globalError.asStateFlow()
    
    protected val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }
    
    init {
        // Network state observation will be started by child classes when they're ready
    }
    
    /**
     * Starts observing network state. Should be called by child classes after initialization.
     */
    protected fun startNetworkObservation() {
        observeNetworkState()
    }
    
    private fun observeNetworkState() {
        viewModelScope.launch {
            networkStateObserver.observeNetworkState()
                .collect { state ->
                    _networkState.value = state
                    onNetworkStateChanged(state)
                }
        }
    }
    
    /**
     * Called when network state changes. Override in subclasses to handle network changes.
     */
    protected open fun onNetworkStateChanged(networkState: NetworkState) {
        // Default implementation does nothing
    }
    
    /**
     * Handles errors and converts them to user-friendly messages
     */
    protected fun handleError(throwable: Throwable) {
        val errorMessage = errorHandler.getErrorMessage(throwable)
        _globalError.value = errorMessage
    }
    
    /**
     * Executes a suspending operation with error handling
     */
    protected fun executeWithErrorHandling(
        operation: suspend () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch(exceptionHandler) {
            try {
                operation()
            } catch (e: Exception) {
                val errorMessage = errorHandler.getErrorMessage(e)
                onError?.invoke(errorMessage) ?: run {
                    _globalError.value = errorMessage
                }
            }
        }
    }
    
    /**
     * Executes a suspending operation that returns a Result with error handling
     */
    protected fun <T> executeWithResult(
        operation: suspend () -> Result<T>,
        onSuccess: (T) -> Unit,
        onError: ((String, Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val result = operation()
                result.fold(
                    onSuccess = onSuccess,
                    onFailure = { throwable ->
                        val errorMessage = errorHandler.getErrorMessage(throwable)
                        val isRetryable = errorHandler.isRecoverableError(throwable)
                        onError?.invoke(errorMessage, isRetryable) ?: run {
                            _globalError.value = errorMessage
                        }
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.getErrorMessage(e)
                val isRetryable = errorHandler.isRecoverableError(e)
                onError?.invoke(errorMessage, isRetryable) ?: run {
                    _globalError.value = errorMessage
                }
            }
        }
    }
    
    /**
     * Clears the global error state
     */
    fun clearGlobalError() {
        _globalError.value = null
    }
    
    /**
     * Checks if network is available
     */
    protected fun isNetworkAvailable(): Boolean {
        return networkStateObserver.isNetworkAvailable()
    }
    
    /**
     * Creates a UiState.Error with retry functionality
     */
    protected fun createRetryableError(
        message: String,
        retryAction: () -> Unit
    ): UiState.Error {
        return UiState.Error(
            message = message,
            isRetryable = true,
            retryAction = retryAction
        )
    }
    
    /**
     * Creates an AsyncState.Error with retry functionality
     */
    protected fun createRetryableAsyncError(
        message: String,
        retryAction: () -> Unit
    ): AsyncState.Error {
        return AsyncState.Error(
            message = message,
            isRetryable = true,
            retryAction = retryAction
        )
    }
}