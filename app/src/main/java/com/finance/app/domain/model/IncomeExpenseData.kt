package com.finance.app.domain.model

/**
 * Domain model representing income vs expense data for a time period
 */
data class IncomeExpenseData(
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val period: String
)
