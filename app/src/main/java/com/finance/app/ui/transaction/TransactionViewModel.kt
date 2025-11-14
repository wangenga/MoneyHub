package com.finance.app.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for transaction list screen
 */
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionListUiState>(TransactionListUiState.Loading)
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    init {
        loadCategories()
        loadTransactions()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories()
                .catch { e ->
                    // Log error but don't fail transaction loading
                    android.util.Log.e("TransactionViewModel", "Error loading categories", e)
                }
                .collect { categories ->
                    _categories.value = categories
                }
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions(),
                _filterState,
                _categories
            ) { transactions, filter, categories ->
                Triple(transactions, filter, categories)
            }
                .catch { e ->
                    _uiState.value = TransactionListUiState.Error(
                        e.message ?: "Failed to load transactions"
                    )
                }
                .collect { (transactions, filter, categories) ->
                    val filtered = applyFilters(transactions, filter)
                    val categoriesMap = categories.associateBy { it.id }
                    
                    _uiState.value = if (filtered.isEmpty()) {
                        TransactionListUiState.Empty
                    } else {
                        TransactionListUiState.Success(
                            transactions = filtered,
                            categoriesMap = categoriesMap
                        )
                    }
                }
        }
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
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionId)
                .onSuccess {
                    // Transaction will be automatically removed from the list via Flow
                }
                .onFailure { e ->
                    _uiState.value = TransactionListUiState.Error(
                        e.message ?: "Failed to delete transaction"
                    )
                }
        }
    }
}

/**
 * UI state for transaction list screen
 */
sealed class TransactionListUiState {
    object Loading : TransactionListUiState()
    object Empty : TransactionListUiState()
    data class Success(
        val transactions: List<Transaction>,
        val categoriesMap: Map<String, Category>
    ) : TransactionListUiState()
    data class Error(val message: String) : TransactionListUiState()
}

/**
 * Filter state for transaction list
 */
data class FilterState(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val type: TransactionType? = null,
    val categoryId: String? = null
)
