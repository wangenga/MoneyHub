package com.finance.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing budgets in local database.
 * 
 * Indexes are added for performance optimization on frequently queried columns:
 * - userId: For filtering budgets by user
 * - categoryId: For finding budgets by category
 * - month, year: For filtering budgets by time period
 * - userId, month, year: Composite index for monthly budget queries
 */
@Entity(
    tableName = "budgets",
    indices = [
        androidx.room.Index(value = ["userId"]),
        androidx.room.Index(value = ["categoryId"]),
        androidx.room.Index(value = ["month", "year"]),
        androidx.room.Index(value = ["userId", "month", "year"]),
        androidx.room.Index(value = ["userId", "categoryId"])
    ]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val categoryId: String,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int,
    val createdAt: Long,
    val updatedAt: Long
)