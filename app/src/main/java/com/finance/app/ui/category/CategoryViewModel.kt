package com.finance.app.ui.category

import com.finance.app.data.sync.NetworkStateObserver
import com.finance.app.domain.model.Category
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.ui.common.AsyncState
import com.finance.app.ui.common.BaseViewModel
import com.finance.app.ui.common.NetworkState
import com.finance.app.ui.common.UiState
import com.finance.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel for category management screen with enhanced error handling
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    errorHandler: ErrorHandler,
    networkStateObserver: NetworkStateObserver
) : BaseViewModel(errorHandler, networkStateObserver) {

    private val _uiState = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Category>>> = _uiState.asStateFlow()

    private val _deleteState = MutableStateFlow<AsyncState>(AsyncState.Idle)
    val deleteState: StateFlow<AsyncState> = _deleteState.asStateFlow()

    init {
        loadCategories()
        startNetworkObservation()
    }

    override fun onNetworkStateChanged(networkState: NetworkState) {
        // Retry loading if we just came back online and have an error
        if (networkState.isConnected && _uiState.value.isError) {
            loadCategories()
        }
    }

    private fun loadCategories() {
        _uiState.value = UiState.Loading
        
        executeWithErrorHandling(
            operation = {
                authRepository.getCurrentUser().collect { user ->
                    if (user != null) {
                        categoryRepository.getAllCategories(user.id)
                            .collect { categories ->
                                _uiState.value = UiState.Success(categories)
                            }
                    } else {
                        _uiState.value = UiState.Error("User not authenticated")
                    }
                }
            },
            onError = { errorMessage ->
                _uiState.value = createRetryableError(errorMessage) {
                    loadCategories()
                }
            }
        )
    }

    fun deleteCategory(categoryId: String) {
        _deleteState.value = AsyncState.Loading
        
        executeWithResult(
            operation = { categoryRepository.deleteCategory(categoryId) },
            onSuccess = {
                _deleteState.value = AsyncState.Success
                // Reset delete state after a short delay
                executeWithErrorHandling(
                    operation = {
                        kotlinx.coroutines.delay(2000)
                        _deleteState.value = AsyncState.Idle
                    }
                )
            },
            onError = { errorMessage, isRetryable ->
                _deleteState.value = if (isRetryable) {
                    createRetryableAsyncError(errorMessage) {
                        deleteCategory(categoryId)
                    }
                } else {
                    AsyncState.Error(errorMessage, false)
                }
            }
        )
    }

    fun clearDeleteError() {
        _deleteState.value = AsyncState.Idle
    }

    fun retry() {
        loadCategories()
    }
}

/**
 * UI state for category list screen - kept for backward compatibility
 */
sealed class CategoryUiState {
    object Loading : CategoryUiState()
    object Empty : CategoryUiState()
    data class Success(val categories: List<Category>) : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}

/**
 * State for delete operations - kept for backward compatibility
 */
sealed class DeleteState {
    object Idle : DeleteState()
    object Deleting : DeleteState()
    object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}
