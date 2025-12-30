package com.finance.app.domain.model

/**
 * Domain model representing a single point in a spending trend chart
 */
data class TrendPoint(
    val date: Long,
    val amount: Double
)
