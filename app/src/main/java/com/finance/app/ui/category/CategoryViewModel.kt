package com.finance.app.ui.category

import com.finance.app.data.sync.NetworkStateObserver
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.ui.common.AsyncState
import com.finance.app.ui.common.BaseViewModel
import com.finance.app.ui.common.NetworkState
import com.finance.app.ui.common.UiState
import com.finance.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import androidx.lifecycle.viewModelScope
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

    private val _expenseCategories = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val expenseCategories: StateFlow<UiState<List<Category>>> = _expenseCategories.asStateFlow()

    private val _incomeCategories = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val incomeCategories: StateFlow<UiState<List<Category>>> = _incomeCategories.asStateFlow()

    // Computed property to detect empty income categories state
    val hasNoIncomeCategories: StateFlow<Boolean> = _incomeCategories
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.isEmpty()
                else -> false
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Computed property to show income category prompt
    val shouldShowIncomeCategoryPrompt: StateFlow<Boolean> = _incomeCategories
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.isEmpty()
                is UiState.Error -> false // Don't show prompt during error state
                UiState.Loading -> false // Don't show prompt during loading
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _deleteState = MutableStateFlow<AsyncState>(AsyncState.Idle)
    val deleteState: StateFlow<AsyncState> = _deleteState.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)

    init {
        loadCategories()
        loadCurrentUser()
        startNetworkObservation()
    }

    override fun onNetworkStateChanged(networkState: NetworkState) {
        // Retry loading if we just came back online and have an error
        if (networkState.isConnected && (_uiState.value.isError || _expenseCategories.value.isError || _incomeCategories.value.isError)) {
            loadCategories()
            loadExpenseCategories()
            loadIncomeCategories()
        }
    }

    private fun loadCurrentUser() {
        executeWithErrorHandling(
            operation = {
                authRepository.getCurrentUser().collect { user ->
                    _currentUserId.value = user?.id
                    if (user != null) {
                        loadExpenseCategories()
                        loadIncomeCategories()
                    }
                }
            }
        )
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

    fun loadExpenseCategories() {
        val userId = _currentUserId.value
        if (userId == null) {
            _expenseCategories.value = UiState.Error("User not authenticated")
            return
        }

        _expenseCategories.value = UiState.Loading
        
        executeWithErrorHandling(
            operation = {
                categoryRepository.getExpenseCategories(userId)
                    .collect { categories ->
                        _expenseCategories.value = UiState.Success(categories)
                    }
            },
            onError = { errorMessage ->
                _expenseCategories.value = createRetryableError(errorMessage) {
                    loadExpenseCategories()
                }
            }
        )
    }

    fun loadIncomeCategories() {
        val userId = _currentUserId.value
        if (userId == null) {
            _incomeCategories.value = UiState.Error("User not authenticated")
            return
        }

        _incomeCategories.value = UiState.Loading
        
        executeWithErrorHandling(
            operation = {
                categoryRepository.getIncomeCategories(userId)
                    .collect { categories ->
                        _incomeCategories.value = UiState.Success(categories)
                    }
            },
            onError = { errorMessage ->
                _incomeCategories.value = createRetryableError(errorMessage) {
                    loadIncomeCategories()
                }
            }
        )
    }

    fun getCategoriesByType(categoryType: CategoryType): StateFlow<UiState<List<Category>>> {
        return when (categoryType) {
            CategoryType.EXPENSE -> expenseCategories
            CategoryType.INCOME -> incomeCategories
        }
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
        loadExpenseCategories()
        loadIncomeCategories()
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
