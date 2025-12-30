package com.finance.app.data.remote.mapper

import com.finance.app.data.remote.model.FirestoreTransaction
import com.finance.app.data.remote.util.TimestampUtils
import com.finance.app.domain.model.SyncStatus
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType

/**
 * Mapper between domain Transaction and Firestore Transaction models
 */
object FirestoreTransactionMapper {
    
    /**
     * Convert domain Transaction to Firestore Transaction
     */
    fun toFirestore(transaction: Transaction): FirestoreTransaction {
        return FirestoreTransaction(
            id = transaction.id,
            type = transaction.type.name,
            amount = transaction.amount,
            categoryId = transaction.categoryId,
            date = TimestampUtils.longToTimestamp(transaction.date),
            paymentMethod = transaction.paymentMethod,
            notes = transaction.notes,
            createdAt = TimestampUtils.longToTimestamp(transaction.createdAt),
            updatedAt = TimestampUtils.longToTimestamp(transaction.updatedAt)
        )
    }
    
    /**
     * Convert Firestore Transaction to domain Transaction
     * Note: userId and syncStatus are not stored in Firestore as they are local concerns
     */
    fun toDomain(firestoreTransaction: FirestoreTransaction, userId: String): Transaction {
        return Transaction(
            id = firestoreTransaction.id,
            userId = userId,
            type = TransactionType.valueOf(firestoreTransaction.type),
            amount = firestoreTransaction.amount,
            categoryId = firestoreTransaction.categoryId,
            date = TimestampUtils.timestampToLong(firestoreTransaction.date),
            paymentMethod = firestoreTransaction.paymentMethod,
            notes = firestoreTransaction.notes,
            createdAt = TimestampUtils.timestampToLong(firestoreTransaction.createdAt),
            updatedAt = TimestampUtils.timestampToLong(firestoreTransaction.updatedAt),
            syncStatus = SyncStatus.SYNCED // Assume synced when coming from Firestore
        )
    }
}