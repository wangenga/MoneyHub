package com.finance.app.data.mapper

import com.finance.app.data.local.entity.BudgetEntity
import com.finance.app.domain.model.Budget

/**
 * Mapper functions to convert between Budget domain model and BudgetEntity
 */

fun BudgetEntity.toDomain(): Budget {
    return Budget(
        id = id,
        userId = userId,
        categoryId = categoryId,
        monthlyLimit = monthlyLimit,
        month = month,
        year = year,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = id,
        userId = userId,
        categoryId = categoryId,
        monthlyLimit = monthlyLimit,
        month = month,
        year = year,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}