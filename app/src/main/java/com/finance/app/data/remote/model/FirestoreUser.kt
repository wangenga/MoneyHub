package com.finance.app.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore data model for User
 * Maps to domain User model
 */
data class FirestoreUser(
    @DocumentId
    val id: String = "",
    
    @PropertyName("email")
    val email: String = "",
    
    @PropertyName("displayName")
    val displayName: String? = null,
    
    @PropertyName("photoUrl")
    val photoUrl: String? = null,
    
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    @PropertyName("lastLoginAt")
    val lastLoginAt: Timestamp = Timestamp.now()
)