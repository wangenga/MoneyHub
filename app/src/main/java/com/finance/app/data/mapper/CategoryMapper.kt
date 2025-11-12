package com.finance.app.data.mapper

import com.finance.app.data.local.entity.CategoryEntity
import com.finance.app.domain.model.Category

/**
 * Mapper functions to convert between Category domain model and CategoryEntity
 * To be fully implemented in task 2
 */

fun CategoryEntity.toDomain(): Category {
    return Category(
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
