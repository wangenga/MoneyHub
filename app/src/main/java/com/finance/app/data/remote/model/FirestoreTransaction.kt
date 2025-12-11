package com.finance.app.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore data model for Transaction
 * Maps to domain Transaction model
 */
data class FirestoreTransaction(
    @DocumentId
    val id: String = "",
    
    @PropertyName("type")
    val type: String = "", // "INCOME" or "EXPENSE"
    
    @PropertyName("amount")
    val amount: Double = 0.0,
    
    @PropertyName("categoryId")
    val categoryId: String = "",
    
    @PropertyName("date")
    val date: Timestamp = Timestamp.now(),
    
    @PropertyName("paymentMethod")
    val paymentMethod: String? = null,
    
    @PropertyName("notes")
    val notes: String? = null,
    
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    @PropertyName("updatedAt")
    val updatedAt: Timestamp = Timestamp.now()
)