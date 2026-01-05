package com.finance.app.ui.transaction

import androidx.lifecycle.viewModelScope
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.domain.repository.TransactionRepository
import com.finance.app.ui.common.BaseViewModel
import com.finance.app.ui.common.NetworkState
import com.finance.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for transaction list screen with enhanced error handling
 */
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<TransactionListData>>(UiState.Loading)
    val uiState: StateFlow<UiState<TransactionListData>> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    init {
        loadCategories()
        loadTransactions()
    }

    override fun onNetworkStateChanged(networkState: NetworkState) {
        // Retry loading if we just came back online and have an error
        if (networkState.isConnected && _uiState.value.isError) {
            loadTransactions()
        }
    }

    private fun loadCategories() {
        executeWithErrorHandling(
            operation = {
                categoryRepository.getAllCategories()
                    .catch { e ->
                        // Log error but don't fail transaction loading
                        android.util.Log.e("TransactionViewModel", "Error loading categories", e)
                    }
                    .collect { categories ->
                        _categories.value = categories
                    }
            }
        )
    }

    private fun loadTransactions() {
        _uiState.value = UiState.Loading
        
        executeWithErrorHandling(
            operation = {
                combine(
                    transactionRepository.getAllTransactions(),
                    _filterState,
                    _categories
                ) { transactions, filter, categories ->
                    Triple(transactions, filter, categories)
                }
                    .collect { (transactions, filter, categories) ->
                        val filtered = applyFilters(transactions, filter)
                        val categoriesMap = categories.associateBy { it.id }
                        
                        val data = TransactionListData(
                            transactions = filtered,
                            categoriesMap = categoriesMap
                        )
                        
                        _uiState.value = if (filtered.isEmpty()) {
                            UiState.Success(data.copy(isEmpty = true))
                        } else {
                            UiState.Success(data)
                        }
                    }
            },
            onError = { errorMessage ->
                _uiState.value = createRetryableError(errorMessage) {
                    loadTransactions()
                }
            }
        )
    }

    private fun applyFilters(
        transactions: List<Transaction>,
        filter: FilterState
    ): List<Transaction> {
        var filtered = transactions

        // Filter by date range
        if (filter.startDate != null && filter.endDate != null) {
            filtered = filtered.filter { it.date in filter.startDate..filter.endDate }
        }

        // Filter by type
        if (filter.type != null) {
            filtered = filtered.filter { it.type == filter.type }
        }

        // Filter by category
        if (filter.categoryId != null) {
            filtered = filtered.filter { it.categoryId == filter.categoryId }
        }

        // Sort by date descending
        return filtered.sortedByDescending { it.date }
    }

    fun updateFilter(filter: FilterState) {
        _filterState.value = filter
    }

    fun clearFilters() {
        _filterState.value = FilterState()
    }

    fun deleteTransaction(transactionId: String) {
        executeWithResult(
            operation = { transactionRepository.deleteTransaction(transactionId) },
            onSuccess = {
                // Transaction will be automatically removed from the list via Flow
            },
            onError = { errorMessage, isRetryable ->
                if (isRetryable) {
                    _uiState.value = createRetryableError(errorMessage) {
                        deleteTransaction(transactionId)
                    }
                } else {
                    handleError(Exception(errorMessage))
                }
            }
        )
    }

    fun retry() {
        loadTransactions()
    }
}

/**
 * Data class for transaction list UI state
 */
data class TransactionListData(
    val transactions: List<Transaction> = emptyList(),
    val categoriesMap: Map<String, Category> = emptyMap(),
    val isEmpty: Boolean = false
)

/**
 * Filter state for transaction list
 */
data class FilterState(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val type: TransactionType? = null,
    val categoryId: String? = null
)
