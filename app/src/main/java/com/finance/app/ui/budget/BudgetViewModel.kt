package com.finance.app.ui.budget

import androidx.lifecycle.viewModelScope
import com.finance.app.data.sync.NetworkStateObserver
import com.finance.app.domain.model.Budget
import com.finance.app.domain.model.BudgetUtilization
import com.finance.app.domain.model.Category
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.BudgetRepository
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.ui.common.AsyncState
import com.finance.app.ui.common.BaseViewModel
import com.finance.app.ui.common.NetworkState
import com.finance.app.ui.common.UiState
import com.finance.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for budget management screens
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    errorHandler: ErrorHandler,
    networkStateObserver: NetworkStateObserver
) : BaseViewModel(errorHandler, networkStateObserver) {

    private val _currentUserId = MutableStateFlow<String?>(null)
    
    private val _budgets = MutableStateFlow<UiState<List<Budget>>>(UiState.Loading)
    val budgets: StateFlow<UiState<List<Budget>>> = _budgets.asStateFlow()
    
    private val _categories = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val categories: StateFlow<UiState<List<Category>>> = _categories.asStateFlow()
    
    private val _budgetUtilization = MutableStateFlow<UiState<List<BudgetUtilization>>>(UiState.Loading)
    val budgetUtilization: StateFlow<UiState<List<BudgetUtilization>>> = _budgetUtilization.asStateFlow()
    
    private val _saveState = MutableStateFlow<AsyncState>(AsyncState.Idle)
    val saveState: StateFlow<AsyncState> = _saveState.asStateFlow()
    
    private val _deleteState = MutableStateFlow<AsyncState>(AsyncState.Idle)
    val deleteState: StateFlow<AsyncState> = _deleteState.asStateFlow()
    
    // Current month and year for budget display
    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()
    
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    init {
        loadCurrentUser()
        startNetworkObservation()
    }

    override fun onNetworkStateChanged(networkState: NetworkState) {
        // Retry loading if we just came back online and have an error
        if (networkState.isConnected && (_budgets.value.isError || _categories.value.isError)) {
            loadBudgets()
            loadCategories()
        }
    }

    private fun loadCurrentUser() {
        executeWithErrorHandling(
            operation = {
                authRepository.getCurrentUser().collect { user ->
                    _currentUserId.value = user?.id
                    if (user != null) {
                        loadBudgets()
                        loadCategories()
                        loadBudgetUtilization()
                    }
                }
            }
        )
    }

    fun loadBudgets() {
        val userId = _currentUserId.value
        if (userId == null) {
            _budgets.value = UiState.Error("User not authenticated")
            return
        }

        _budgets.value = UiState.Loading
        
        executeWithErrorHandling(
            operation = {
                budgetRepository.getBudgetsForUser(userId)
                    .collect { budgetList ->
                        _budgets.value = UiState.Success(budgetList)
                    }
            },
            onError = { errorMessage ->
                _budgets.value = createRetryableError(errorMessage) {
                    loadBudgets()
                }
            }
        )
    }

    fun loadCategories() {
        val userId = _currentUserId.value
        if (userId == null) {
            _categories.value = UiState.Error("User not authenticated")
            return
        }

        _categories.value = UiState.Loading
        
        executeWithErrorHandling(
            operation = {
                // Only load expense categories for budget management
                categoryRepository.getExpenseCategories(userId)
                    .collect { categoryList ->
                        _categories.value = UiState.Success(categoryList)
                    }
            },
            onError = { errorMessage ->
                _categories.value = createRetryableError(errorMessage) {
                    loadCategories()
                }
            }
        )
    }

    fun loadBudgetUtilization() {
        val userId = _currentUserId.value
        if (userId == null) {
            _budgetUtilization.value = UiState.Error("User not authenticated")
            return
        }

        _budgetUtilization.value = UiState.Loading
        
        executeWithErrorHandling(
            operation = {
                budgetRepository.getBudgetUtilization(
                    userId = userId,
                    month = _currentMonth.value,
                    year = _currentYear.value
                ).collect { utilization ->
                    _budgetUtilization.value = UiState.Success(utilization)
                }
            },
            onError = { errorMessage ->
                _budgetUtilization.value = createRetryableError(errorMessage) {
                    loadBudgetUtilization()
                }
            }
        )
    }

    fun saveBudget(categoryId: String, monthlyLimit: Double) {
        val userId = _currentUserId.value
        if (userId == null) {
            _saveState.value = AsyncState.Error("User not authenticated", false)
            return
        }

        // Validate budget amount
        if (!Budget.isValidBudgetAmount(monthlyLimit)) {
            _saveState.value = AsyncState.Error("Budget amount must be between 0 and 1,000,000", false)
            return
        }

        _saveState.value = AsyncState.Loading
        
        val budget = Budget(
            id = "${userId}_${categoryId}_${_currentMonth.value}_${_currentYear.value}",
            userId = userId,
            categoryId = categoryId,
            monthlyLimit = monthlyLimit,
            month = _currentMonth.value,
            year = _currentYear.value,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        executeWithResult(
            operation = { budgetRepository.setBudget(budget) },
            onSuccess = {
                _saveState.value = AsyncState.Success
                // Reset save state after a short delay
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _saveState.value = AsyncState.Idle
                }
            },
            onError = { errorMessage, isRetryable ->
                _saveState.value = if (isRetryable) {
                    createRetryableAsyncError(errorMessage) {
                        saveBudget(categoryId, monthlyLimit)
                    }
                } else {
                    AsyncState.Error(errorMessage, false)
                }
            }
        )
    }

    fun deleteBudget(categoryId: String) {
        val userId = _currentUserId.value
        if (userId == null) {
            _deleteState.value = AsyncState.Error("User not authenticated", false)
            return
        }

        _deleteState.value = AsyncState.Loading
        
        executeWithResult(
            operation = { budgetRepository.deleteBudget(userId, categoryId) },
            onSuccess = {
                _deleteState.value = AsyncState.Success
                // Reset delete state after a short delay
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _deleteState.value = AsyncState.Idle
                }
            },
            onError = { errorMessage, isRetryable ->
                _deleteState.value = if (isRetryable) {
                    createRetryableAsyncError(errorMessage) {
                        deleteBudget(categoryId)
                    }
                } else {
                    AsyncState.Error(errorMessage, false)
                }
            }
        )
    }

    fun getBudgetForCategory(categoryId: String): StateFlow<Budget?> {
        val userId = _currentUserId.value ?: return MutableStateFlow(null)
        
        return budgetRepository.getBudgetByCategory(userId, categoryId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    fun clearSaveState() {
        _saveState.value = AsyncState.Idle
    }

    fun clearDeleteState() {
        _deleteState.value = AsyncState.Idle
    }

    fun retry() {
        loadBudgets()
        loadCategories()
        loadBudgetUtilization()
    }
}
