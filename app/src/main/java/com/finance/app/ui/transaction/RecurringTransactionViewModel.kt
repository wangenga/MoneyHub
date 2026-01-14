package com.finance.app.ui.transaction

import androidx.lifecycle.viewModelScope
import com.finance.app.data.sync.NetworkStateObserver
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.RecurringTransaction
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.domain.repository.RecurringTransactionRepository
import com.finance.app.ui.common.BaseViewModel
import com.finance.app.ui.common.NetworkState
import com.finance.app.ui.common.UiState
import com.finance.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for recurring transaction list screen
 */
@HiltViewModel
class RecurringTransactionViewModel @Inject constructor(
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    errorHandler: ErrorHandler,
    networkStateObserver: NetworkStateObserver
) : BaseViewModel(errorHandler, networkStateObserver) {

    private val _uiState = MutableStateFlow<UiState<RecurringTransactionListData>>(UiState.Loading)
    val uiState: StateFlow<UiState<RecurringTransactionListData>> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    init {
        loadCategories()
        loadRecurringTransactions()
        startNetworkObservation()
    }

    override fun onNetworkStateChanged(networkState: NetworkState) {
        if (networkState.isConnected && _uiState.value.isError) {
            loadRecurringTransactions()
        }
    }

    private fun loadCategories() {
        executeWithErrorHandling(
            operation = {
                authRepository.getCurrentUser().collect { user ->
                    if (user != null) {
                        categoryRepository.getAllCategories(user.id)
                            .catch { e ->
                                android.util.Log.e("RecurringTransactionVM", "Error loading categories", e)
                            }
                            .collect { categories ->
                                _categories.value = categories
                            }
                    }
                }
            }
        )
    }

    private fun loadRecurringTransactions() {
        _uiState.value = UiState.Loading
        
        executeWithErrorHandling(
            operation = {
                combine(
                    recurringTransactionRepository.getRecurringTransactions(),
                    _categories
                ) { recurringTransactions, categories ->
                    Pair(recurringTransactions, categories)
                }
                    .collect { (recurringTransactions, categories) ->
                        val categoriesMap = categories.associateBy { it.id }
                        
                        val data = RecurringTransactionListData(
                            recurringTransactions = recurringTransactions,
                            categoriesMap = categoriesMap
                        )
                        
                        _uiState.value = if (recurringTransactions.isEmpty()) {
                            UiState.Success(data.copy(isEmpty = true))
                        } else {
                            UiState.Success(data)
                        }
                    }
            },
            onError = { errorMessage ->
                _uiState.value = createRetryableError(errorMessage) {
                    loadRecurringTransactions()
                }
            }
        )
    }

    fun deleteRecurringTransaction(recurringTransactionId: String) {
        executeWithResult(
            operation = { recurringTransactionRepository.deleteRecurringTransaction(recurringTransactionId) },
            onSuccess = {
                // Transaction will be automatically removed from the list via Flow
            },
            onError = { errorMessage, isRetryable ->
                if (isRetryable) {
                    _uiState.value = createRetryableError(errorMessage) {
                        deleteRecurringTransaction(recurringTransactionId)
                    }
                } else {
                    handleError(Exception(errorMessage))
                }
            }
        )
    }

    fun deactivateRecurringTransaction(recurringTransactionId: String) {
        executeWithResult(
            operation = { recurringTransactionRepository.deactivateRecurringTransaction(recurringTransactionId) },
            onSuccess = {
                // Transaction will be automatically updated in the list via Flow
            },
            onError = { errorMessage, isRetryable ->
                if (isRetryable) {
                    _uiState.value = createRetryableError(errorMessage) {
                        deactivateRecurringTransaction(recurringTransactionId)
                    }
                } else {
                    handleError(Exception(errorMessage))
                }
            }
        )
    }

    fun retry() {
        loadRecurringTransactions()
    }
}

/**
 * Data class for recurring transaction list UI state
 */
data class RecurringTransactionListData(
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val categoriesMap: Map<String, Category> = emptyMap(),
    val isEmpty: Boolean = false
)
