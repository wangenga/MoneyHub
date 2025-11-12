package com.finance.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing categories in local database
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String?,
    val name: String,
    val color: String,
    val iconName: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
