package com.finance.app.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore data model for Category
 * Maps to domain Category model
 */
data class FirestoreCategory(
    @DocumentId
    val id: String = "",
    
    @PropertyName("name")
    val name: String = "",
    
    @PropertyName("color")
    val color: String = "",
    
    @PropertyName("iconName")
    val iconName: String = "",
    
    @PropertyName("isDefault")
    val isDefault: Boolean = false,
    
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    @PropertyName("updatedAt")
    val updatedAt: Timestamp = Timestamp.now()
)