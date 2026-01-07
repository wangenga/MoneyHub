package com.finance.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing categories in local database
 * Index added for performance optimization on userId and categoryType queries
 */
@Entity(
    tableName = "categories",
    indices = [
        androidx.room.Index(value = ["userId"]),
        androidx.room.Index(value = ["categoryType"])
    ]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String?,
    val name: String,
    val color: String,
    val iconName: String,
    val categoryType: String,  // Stored as string: "INCOME" or "EXPENSE"
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
