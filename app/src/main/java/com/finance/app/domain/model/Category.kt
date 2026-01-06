package com.finance.app.domain.model

/**
 * Domain model representing a transaction category.
 * 
 * Categories are used to organize transactions into logical groups.
 * They can be either system-provided defaults (for expenses only) or user-created custom categories.
 * 
 * @property id Unique identifier for the category
 * @property userId Owner's user ID (null for default categories)
 * @property name Display name of the category
 * @property color Hex color code for visual representation (e.g., "#2196F3")
 * @property iconName Material icon name for visual representation
 * @property categoryType Type of category: INCOME or EXPENSE
 * @property isDefault Whether this is a system-provided default category
 * @property createdAt Timestamp when the category was created
 * @property updatedAt Timestamp when the category was last updated
 */
data class Category(
    val id: String,
    val userId: String?,
    val name: String,
    val color: String,
    val iconName: String,
    val categoryType: CategoryType,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
