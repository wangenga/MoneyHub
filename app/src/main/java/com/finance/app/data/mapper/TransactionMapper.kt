package com.finance.app.data.mapper

import com.finance.app.data.local.entity.TransactionEntity
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType
import com.finance.app.domain.model.SyncStatus

/**
 * Mapper functions to convert between Transaction domain model and TransactionEntity
 * To be fully implemented in task 2
 */

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        userId = userId,
        type = TransactionType.valueOf(type),
        amount = amount,
        categoryId = categoryId,
        date = date,
        paymentMethod = paymentMethod,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        userId = userId,
        type = type.name,
        amount = amount,
        categoryId = categoryId,
        date = date,
        paymentMethod = paymentMethod,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus.name
    )
}
