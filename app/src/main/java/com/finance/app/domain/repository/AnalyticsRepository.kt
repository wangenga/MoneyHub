package com.finance.app.domain.repository

import com.finance.app.domain.model.Category
import com.finance.app.domain.model.IncomeExpenseData
import com.finance.app.domain.model.TrendPoint
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for analytics and reporting operations
 */
interface AnalyticsRepository {
    /**
     * Get spending breakdown by category for a date range
     * @param startDate Start timestamp in milliseconds
     * @param endDate End timestamp in milliseconds
     * @return Flow of map with Category as key and total spending as value
     */
    fun getSpendingByCategory(startDate: Long, endDate: Long): Flow<Map<Category, Double>>

    /**
     * Get income vs expense data for a time period
     * @param startDate Start timestamp in milliseconds
     * @param endDate End timestamp in milliseconds
     * @return Flow of IncomeExpenseData containing totals and net balance
     */
    fun getIncomeVsExpense(startDate: Long, endDate: Long): Flow<IncomeExpenseData>

    /**
     * Get spending trend over time
     * @param startDate Start timestamp in milliseconds
     * @param endDate End timestamp in milliseconds
     * @return Flow of list of TrendPoint representing spending over time
     */
    fun getSpendingTrend(startDate: Long, endDate: Long): Flow<List<TrendPoint>>

    /**
     * Get monthly balance (income - expenses for current month)
     * @return Flow of current month's net balance
     */
    fun getMonthlyBalance(): Flow<Double>
}
