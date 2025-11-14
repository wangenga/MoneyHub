package com.finance.app.data.repository

import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.IncomeExpenseData
import com.finance.app.domain.model.TransactionType
import com.finance.app.domain.model.TrendPoint
import com.finance.app.domain.repository.AnalyticsRepository
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

/**
 * Implementation of AnalyticsRepository
 * Provides analytics calculations and aggregations for financial data
 */
class AnalyticsRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : AnalyticsRepository {

    override fun getSpendingByCategory(startDate: Long, endDate: Long): Flow<Map<Category, Double>> {
        return authRepository.getCurrentUser().map { user ->
            if (user == null) {
                emptyMap()
            } else {
                val spendingList = transactionDao.getSpendingByCategory(user.id, startDate, endDate).first()
                val categories = categoryRepository.getAllCategories().first()
                
                // Map category IDs to Category objects with spending amounts
                val categoryMap = categories.associateBy { it.id }
                spendingList.mapNotNull { categorySpending ->
                    categoryMap[categorySpending.categoryId]?.let { category ->
                        category to categorySpending.total
                    }
                }.toMap()
            }
        }
    }

    override fun getIncomeVsExpense(startDate: Long, endDate: Long): Flow<IncomeExpenseData> {
        return authRepository.getCurrentUser().map { user ->
            if (user == null) {
                IncomeExpenseData(
                    totalIncome = 0.0,
                    totalExpense = 0.0,
                    netBalance = 0.0,
                    period = formatPeriod(startDate, endDate)
                )
            } else {
                val totalIncome = transactionDao.getTotalByType(
                    user.id,
                    TransactionType.INCOME.name,
                    startDate,
                    endDate
                ).first() ?: 0.0

                val totalExpense = transactionDao.getTotalByType(
                    user.id,
                    TransactionType.EXPENSE.name,
                    startDate,
                    endDate
                ).first() ?: 0.0

                IncomeExpenseData(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netBalance = totalIncome - totalExpense,
                    period = formatPeriod(startDate, endDate)
                )
            }
        }
    }

    override fun getSpendingTrend(startDate: Long, endDate: Long): Flow<List<TrendPoint>> {
        return authRepository.getCurrentUser().map { user ->
            if (user == null) {
                emptyList()
            } else {
                val trendList = transactionDao.getSpendingTrend(user.id, startDate, endDate).first()
                
                // Convert list to TrendPoint objects
                trendList.map { dailySpending ->
                    TrendPoint(date = dailySpending.date, amount = dailySpending.total)
                }
            }
        }
    }

    override fun getMonthlyBalance(): Flow<Double> {
        return authRepository.getCurrentUser().map { user ->
            if (user == null) {
                0.0
            } else {
                val (startDate, endDate) = getCurrentMonthRange()
                
                val totalIncome = transactionDao.getTotalByType(
                    user.id,
                    TransactionType.INCOME.name,
                    startDate,
                    endDate
                ).first() ?: 0.0

                val totalExpense = transactionDao.getTotalByType(
                    user.id,
                    TransactionType.EXPENSE.name,
                    startDate,
                    endDate
                ).first() ?: 0.0

                totalIncome - totalExpense
            }
        }
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
     * Format a date range into a readable period string
     */
    private fun formatPeriod(startDate: Long, endDate: Long): String {
        val startCalendar = Calendar.getInstance().apply { timeInMillis = startDate }
        val endCalendar = Calendar.getInstance().apply { timeInMillis = endDate }
        
        val startMonth = startCalendar.get(Calendar.MONTH)
        val endMonth = endCalendar.get(Calendar.MONTH)
        val startYear = startCalendar.get(Calendar.YEAR)
        val endYear = endCalendar.get(Calendar.YEAR)
        
        return when {
            startYear == endYear && startMonth == endMonth -> {
                "${getMonthName(startMonth)} $startYear"
            }
            startYear == endYear -> {
                "${getMonthName(startMonth)} - ${getMonthName(endMonth)} $startYear"
            }
            else -> {
                "${getMonthName(startMonth)} $startYear - ${getMonthName(endMonth)} $endYear"
            }
        }
    }

    /**
     * Get month name from month index
     */
    private fun getMonthName(month: Int): String {
        return when (month) {
            Calendar.JANUARY -> "January"
            Calendar.FEBRUARY -> "February"
            Calendar.MARCH -> "March"
            Calendar.APRIL -> "April"
            Calendar.MAY -> "May"
            Calendar.JUNE -> "June"
            Calendar.JULY -> "July"
            Calendar.AUGUST -> "August"
            Calendar.SEPTEMBER -> "September"
            Calendar.OCTOBER -> "October"
            Calendar.NOVEMBER -> "November"
            Calendar.DECEMBER -> "December"
            else -> "Unknown"
        }
    }
}
