package com.finance.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing recurring transactions in local database
 * Indexes added for performance optimization on frequently queried columns
 */
@Entity(
    tableName = "recurring_transactions",
    indices = [
        androidx.room.Index(value = ["userId"]),
        androidx.room.Index(value = ["nextDueDate"]),
        androidx.room.Index(value = ["isActive"]),
        androidx.room.Index(value = ["userId", "isActive"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    // Template transaction fields
    val templateTransactionId: String,
    val templateType: String,
    val templateAmount: Double,
    val templateCategoryId: String,
    val templatePaymentMethod: String?,
    val templateNotes: String?,
    // Recurrence configuration
    val recurrencePattern: String,
    val nextDueDate: Long,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)