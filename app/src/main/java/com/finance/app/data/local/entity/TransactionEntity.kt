package com.finance.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing transactions in local database
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val amount: Double,
    val categoryId: String,
    val date: Long,
    val paymentMethod: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String
)
