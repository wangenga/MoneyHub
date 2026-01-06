package com.finance.app.data.mapper

import com.finance.app.data.local.entity.CategoryEntity
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType

/**
 * Mapper functions to convert between Category domain model and CategoryEntity
 * Note: categoryType mapping will be fully implemented in task 2 when CategoryEntity is updated
 */

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        userId = userId,
        name = name,
        color = color,
        iconName = iconName,
        categoryType = CategoryType.EXPENSE, // Default for backward compatibility until entity is updated
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
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
