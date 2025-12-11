package com.finance.app.data.local.model

/**
 * Data class for daily spending aggregation query results
 */
data class DailySpending(
    val date: Long,
    val total: Double
)
