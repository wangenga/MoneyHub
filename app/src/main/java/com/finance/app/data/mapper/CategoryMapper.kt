package com.finance.app.data.mapper

import com.finance.app.data.local.entity.CategoryEntity
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType

/**
 * Mapper functions to convert between Category domain model and CategoryEntity
 */

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        userId = userId,
        name = name,
        color = color,
        iconName = iconName,
        categoryType = when (categoryType) {
            "INCOME" -> CategoryType.INCOME
            "EXPENSE" -> CategoryType.EXPENSE
            else -> CategoryType.EXPENSE // Default fallback for invalid values
        },
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        userId = userId,
        name = name,
        color = color,
        iconName = iconName,
        categoryType = categoryType.name, // Convert enum to string
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
