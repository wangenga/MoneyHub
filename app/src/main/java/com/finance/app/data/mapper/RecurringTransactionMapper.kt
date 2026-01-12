package com.finance.app.data.mapper

import com.finance.app.data.local.entity.RecurringTransactionEntity
import com.finance.app.domain.model.RecurringTransaction
import com.finance.app.domain.model.RecurrencePattern
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType
import com.finance.app.domain.model.SyncStatus

/**
 * Mapper functions to convert between RecurringTransaction domain model and RecurringTransactionEntity
 */

fun RecurringTransactionEntity.toDomain(): RecurringTransaction {
    // Create template transaction from entity fields
    val templateTransaction = Transaction(
        id = templateTransactionId,
        userId = userId,
        type = TransactionType.valueOf(templateType),
        amount = templateAmount,
        categoryId = templateCategoryId,
        date = 0L, // Will be set when creating instances
        paymentMethod = templatePaymentMethod,
        notes = templateNotes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = SyncStatus.PENDING // Template transactions are always pending
    )
    
    return RecurringTransaction(
        id = id,
        userId = userId,
        templateTransaction = templateTransaction,
        recurrencePattern = RecurrencePattern.valueOf(recurrencePattern),
        nextDueDate = nextDueDate,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun RecurringTransaction.toEntity(): RecurringTransactionEntity {
    return RecurringTransactionEntity(
        id = id,
        userId = userId,
        templateTransactionId = templateTransaction.id,
        templateType = templateTransaction.type.name,
        templateAmount = templateTransaction.amount,
        templateCategoryId = templateTransaction.categoryId,
        templatePaymentMethod = templateTransaction.paymentMethod,
        templateNotes = templateTransaction.notes,
        recurrencePattern = recurrencePattern.name,
        nextDueDate = nextDueDate,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}