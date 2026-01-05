package com.finance.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.IncomeExpenseData
import com.finance.app.domain.model.TrendPoint
import com.finance.app.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel for analytics and reports screen
 * Manages state for charts, statistics, and date range selection
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        // Initialize with current month data
        val (startDate, endDate) = getCurrentMonthRange()
        updateDateRange(startDate, endDate)
    }

    /**
     * Update the selected date range and refresh all analytics data
     */
    fun updateDateRange(startDate: Long, endDate: Long) {
        _uiState.value = _uiState.value.copy(
            selectedStartDate = startDate,
            selectedEndDate = endDate,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                // Combine all analytics data flows
                combine(
                    analyticsRepository.getSpendingByCategory(startDate, endDate),
                    analyticsRepository.getIncomeVsExpense(startDate, endDate),
                    analyticsRepository.getSpendingTrend(startDate, endDate),
                    analyticsRepository.getMonthlyBalance()
                ) { spendingByCategory, incomeExpenseData, spendingTrend, monthlyBalance ->
                    AnalyticsData(
                        spendingByCategory = spendingByCategory,
                        incomeExpenseData = incomeExpenseData,
                        spendingTrend = spendingTrend,
                        monthlyBalance = monthlyBalance
                    )
                }.catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load analytics data: ${exception.message}"
                    )
                }.collect { analyticsData ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        spendingByCategory = analyticsData.spendingByCategory,
                        incomeExpenseData = analyticsData.incomeExpenseData,
                        spendingTrend = analyticsData.spendingTrend,
                        monthlyBalance = analyticsData.monthlyBalance,
                        hasData = analyticsData.hasData()
                    )
                }
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load analytics data: ${exception.message}"
                )
            }
        }
    }

    /**
     * Set predefined date ranges (This Month, Last Month, Last 3 Months, etc.)
     */
    fun setDateRangePreset(preset: DateRangePreset) {
        val (startDate, endDate) = when (preset) {
            DateRangePreset.THIS_MONTH -> getCurrentMonthRange()
            DateRangePreset.LAST_MONTH -> getLastMonthRange()
            DateRangePreset.LAST_3_MONTHS -> getLast3MonthsRange()
            DateRangePreset.LAST_6_MONTHS -> getLast6MonthsRange()
            DateRangePreset.THIS_YEAR -> getThisYearRange()
        }
        
        _uiState.value = _uiState.value.copy(selectedPreset = preset)
        updateDateRange(startDate, endDate)
    }

    /**
     * Clear any error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Get the start and end timestamps for the current month
     */
    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        // Start of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis
        
        // End of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis
        
        return Pair(startDate, endDate)
    }

    /**
     * Get the start and end timestamps for last month
     */
    private fun getLastMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        
        // Start of last month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis
        
        // End of last month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis
        
        return Pair(startDate, endDate)
    }

    /**
     * Get the start and end timestamps for last 3 months
     */
    private fun getLast3MonthsRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        // End date is end of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis
        
        // Start date is 3 months ago
        calendar.add(Calendar.MONTH, -3)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis
        
        return Pair(startDate, endDate)
    }

    /**
     * Get the start and end timestamps for last 6 months
     */
    private fun getLast6MonthsRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        // End date is end of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis
        
        // Start date is 6 months ago
        calendar.add(Calendar.MONTH, -6)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis
        
        return Pair(startDate, endDate)
    }

    /**
     * Get the start and end timestamps for this year
     */
    private fun getThisYearRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        // End date is end of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis
        
        // Start date is beginning of year
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis
        
        return Pair(startDate, endDate)
    }
}

/**
 * UI state for analytics screen
 */
data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedStartDate: Long = 0L,
    val selectedEndDate: Long = 0L,
    val selectedPreset: DateRangePreset = DateRangePreset.THIS_MONTH,
    val spendingByCategory: Map<Category, Double> = emptyMap(),
    val incomeExpenseData: IncomeExpenseData = IncomeExpenseData(0.0, 0.0, 0.0, ""),
    val spendingTrend: List<TrendPoint> = emptyList(),
    val monthlyBalance: Double = 0.0,
    val hasData: Boolean = false
)

/**
 * Combined analytics data
 */
private data class AnalyticsData(
    val spendingByCategory: Map<Category, Double>,
    val incomeExpenseData: IncomeExpenseData,
    val spendingTrend: List<TrendPoint>,
    val monthlyBalance: Double
) {
    fun hasData(): Boolean {
        return spendingByCategory.isNotEmpty() || 
               incomeExpenseData.totalIncome > 0 || 
               incomeExpenseData.totalExpense > 0 ||
               spendingTrend.isNotEmpty()
    }
}

/**
 * Predefined date range options
 */
enum class DateRangePreset(val displayName: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    THIS_YEAR("This Year")
}