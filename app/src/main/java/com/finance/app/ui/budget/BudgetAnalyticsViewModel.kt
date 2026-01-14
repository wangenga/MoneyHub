package com.finance.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.model.BudgetUtilization
import com.finance.app.domain.repository.BudgetRepository
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for budget utilization analytics display.
 * 
 * Manages the state and data flow for displaying budget utilization information
 * across all categories with budget limits. Provides visual indicators and
 * spending progress tracking.
 */
@HiltViewModel
class BudgetAnalyticsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _budgetUtilizationState = MutableStateFlow<UiState<List<BudgetUtilization>>>(
        UiState.Loading
    )
    val budgetUtilizationState: StateFlow<UiState<List<BudgetUtilization>>> = 
        _budgetUtilizationState.asStateFlow()

    private val _currentMonth = MutableStateFlow(LocalDate.now().monthValue)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    private val _currentYear = MutableStateFlow(LocalDate.now().year)
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    init {
        loadBudgetUtilization()
    }

    /**
     * Load budget utilization data for the current month and year
     */
    private fun loadBudgetUtilization() {
        authRepository.getCurrentUser()
            .onEach { user ->
                if (user != null) {
                    budgetRepository.getBudgetUtilization(
                        userId = user.id,
                        month = _currentMonth.value,
                        year = _currentYear.value
                    )
                        .catch { error ->
                            _budgetUtilizationState.value = UiState.Error(
                                error.message ?: "Failed to load budget utilization"
                            )
                        }
                        .onEach { utilization ->
                            _budgetUtilizationState.value = UiState.Success(utilization)
                        }
                        .launchIn(viewModelScope)
                } else {
                    _budgetUtilizationState.value = UiState.Error("User not authenticated")
                }
            }
            .catch { error ->
                _budgetUtilizationState.value = UiState.Error(
                    error.message ?: "Failed to get current user"
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Navigate to previous month
     */
    fun previousMonth() {
        val currentMonth = _currentMonth.value
        val currentYear = _currentYear.value
        
        if (currentMonth == 1) {
            _currentMonth.value = 12
            _currentYear.value = currentYear - 1
        } else {
            _currentMonth.value = currentMonth - 1
        }
        
        loadBudgetUtilization()
    }

    /**
     * Navigate to next month
     */
    fun nextMonth() {
        val currentMonth = _currentMonth.value
        val currentYear = _currentYear.value
        
        if (currentMonth == 12) {
            _currentMonth.value = 1
            _currentYear.value = currentYear + 1
        } else {
            _currentMonth.value = currentMonth + 1
        }
        
        loadBudgetUtilization()
    }

    /**
     * Reset to current month and year
     */
    fun resetToCurrentMonth() {
        _currentMonth.value = LocalDate.now().monthValue
        _currentYear.value = LocalDate.now().year
        loadBudgetUtilization()
    }

    /**
     * Get total budget across all categories
     */
    fun getTotalBudget(utilizations: List<BudgetUtilization>): Double {
        return utilizations.sumOf { it.budgetLimit }
    }

    /**
     * Get total spending across all categories
     */
    fun getTotalSpending(utilizations: List<BudgetUtilization>): Double {
        return utilizations.sumOf { it.currentSpending }
    }

    /**
     * Get overall utilization percentage
     */
    fun getOverallUtilizationPercentage(utilizations: List<BudgetUtilization>): Double {
        val totalBudget = getTotalBudget(utilizations)
        if (totalBudget <= 0.0) return 0.0
        
        val totalSpending = getTotalSpending(utilizations)
        return (totalSpending / totalBudget) * 100.0
    }

    /**
     * Get count of categories over budget
     */
    fun getOverBudgetCount(utilizations: List<BudgetUtilization>): Int {
        return utilizations.count { it.isOverBudget }
    }

    /**
     * Get count of categories at critical threshold (90%+)
     */
    fun getCriticalCount(utilizations: List<BudgetUtilization>): Int {
        return utilizations.count { 
            it.utilizationPercentage >= 90.0 && !it.isOverBudget
        }
    }

    /**
     * Get count of categories at warning threshold (75%+)
     */
    fun getWarningCount(utilizations: List<BudgetUtilization>): Int {
        return utilizations.count { 
            it.utilizationPercentage >= 75.0 && it.utilizationPercentage < 90.0
        }
    }
}
