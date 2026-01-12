package com.finance.app.data.remote.mapper

import com.finance.app.data.remote.model.FirestoreRecurringTransaction
import com.finance.app.domain.model.RecurringTransaction
import com.finance.app.domain.model.RecurrencePattern
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType
import com.finance.app.domain.model.SyncStatus
import com.google.firebase.Timestamp

/**
 * Mapper for converting between RecurringTransaction domain model and FirestoreRecurringTransaction
 */
object FirestoreRecurringTransactionMapper {
    
    fun toFirestore(recurringTransaction: RecurringTransaction): FirestoreRecurringTransaction {
        return FirestoreRecurringTransaction(
            id = recurringTransaction.id,
            userId = recurringTransaction.userId,
            templateTransactionId = recurringTransaction.templateTransaction.id,
            templateType = recurringTransaction.templateTransaction.type.name,
            templateAmount = recurringTransaction.templateTransaction.amount,
            templateCategoryId = recurringTransaction.templateTransaction.categoryId,
            templatePaymentMethod = recurringTransaction.templateTransaction.paymentMethod,
            templateNotes = recurringTransaction.templateTransaction.notes,
            recurrencePattern = recurringTransaction.recurrencePattern.name,
            nextDueDate = Timestamp(recurringTransaction.nextDueDate / 1000, 0),
            isActive = recurringTransaction.isActive,
            createdAt = Timestamp(recurringTransaction.createdAt / 1000, 0),
            updatedAt = Timestamp(recurringTransaction.updatedAt / 1000, 0)
        )
    }
    
    fun toDomain(firestoreRecurringTransaction: FirestoreRecurringTransaction, userId: String): RecurringTransaction {
        // Create template transaction from Firestore fields
        val templateTransaction = Transaction(
            id = firestoreRecurringTransaction.templateTransactionId,
            userId = userId,
            type = TransactionType.valueOf(firestoreRecurringTransaction.templateType),
            amount = firestoreRecurringTransaction.templateAmount,
            categoryId = firestoreRecurringTransaction.templateCategoryId,
            date = 0L, // Will be set when creating instances
            paymentMethod = firestoreRecurringTransaction.templatePaymentMethod,
            notes = firestoreRecurringTransaction.templateNotes,
            createdAt = firestoreRecurringTransaction.createdAt.toDate().time,
            updatedAt = firestoreRecurringTransaction.updatedAt.toDate().time,
            syncStatus = SyncStatus.SYNCED // From Firestore, so it's synced
        )
        
        return RecurringTransaction(
            id = firestoreRecurringTransaction.id,
            userId = userId,
            templateTransaction = templateTransaction,
            recurrencePattern = RecurrencePattern.valueOf(firestoreRecurringTransaction.recurrencePattern),
            nextDueDate = firestoreRecurringTransaction.nextDueDate.toDate().time,
            isActive = firestoreRecurringTransaction.isActive,
            createdAt = firestoreRecurringTransaction.createdAt.toDate().time,
            updatedAt = firestoreRecurringTransaction.updatedAt.toDate().time
        )
    }
}