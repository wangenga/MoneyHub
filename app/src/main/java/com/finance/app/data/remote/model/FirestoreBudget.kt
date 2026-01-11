package com.finance.app.data.remote.model

import com.google.firebase.Timestamp

/**
 * Firestore model for budget data
 * This class represents how budget data is stored in Firestore
 */
data class FirestoreBudget(
    val id: String = "",
    val categoryId: String = "",
    val monthlyLimit: Double = 0.0,
    val month: Int = 0,
    val year: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)