package com.finance.app.data.remote.model

import com.google.firebase.Timestamp

/**
 * Firestore model for recurring transactions
 * Used for remote storage and synchronization
 */
data class FirestoreRecurringTransaction(
    val id: String = "",
    val userId: String = "",
    // Template transaction fields
    val templateTransactionId: String = "",
    val templateType: String = "",
    val templateAmount: Double = 0.0,
    val templateCategoryId: String = "",
    val templatePaymentMethod: String? = null,
    val templateNotes: String? = null,
    // Recurrence configuration
    val recurrencePattern: String = "",
    val nextDueDate: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)