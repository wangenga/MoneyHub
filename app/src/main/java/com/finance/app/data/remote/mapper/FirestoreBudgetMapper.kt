package com.finance.app.data.remote.mapper

import com.finance.app.data.remote.model.FirestoreBudget
import com.finance.app.data.remote.util.TimestampUtils
import com.finance.app.domain.model.Budget

/**
 * Mapper functions to convert between Budget domain model and FirestoreBudget
 */
object FirestoreBudgetMapper {
    
    fun toFirestore(budget: Budget): FirestoreBudget {
        return FirestoreBudget(
            id = budget.id,
            categoryId = budget.categoryId,
            monthlyLimit = budget.monthlyLimit,
            month = budget.month,
            year = budget.year,
            createdAt = TimestampUtils.longToTimestamp(budget.createdAt),
            updatedAt = TimestampUtils.longToTimestamp(budget.updatedAt)
        )
    }
    
    fun toDomain(firestoreBudget: FirestoreBudget, userId: String): Budget {
        return Budget(
            id = firestoreBudget.id,
            userId = userId,
            categoryId = firestoreBudget.categoryId,
            monthlyLimit = firestoreBudget.monthlyLimit,
            month = firestoreBudget.month,
            year = firestoreBudget.year,
            createdAt = TimestampUtils.timestampToLong(firestoreBudget.createdAt),
            updatedAt = TimestampUtils.timestampToLong(firestoreBudget.updatedAt)
        )
    }
}