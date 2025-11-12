package com.finance.app.domain.model

/**
 * Domain model representing a transaction category
 */
data class Category(
    val id: String,
    val userId: String?,
    val name: String,
    val color: String,
    val iconName: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
