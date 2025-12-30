package com.finance.app.data.remote.mapper

import com.finance.app.data.remote.model.FirestoreUser
import com.finance.app.data.remote.util.TimestampUtils
import com.finance.app.domain.model.User

/**
 * Mapper between domain User and Firestore User models
 */
object FirestoreUserMapper {
    
    /**
     * Convert domain User to Firestore User
     */
    fun toFirestore(user: User): FirestoreUser {
        return FirestoreUser(
            id = user.id,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoUrl,
            createdAt = TimestampUtils.longToTimestamp(user.createdAt),
            lastLoginAt = TimestampUtils.longToTimestamp(user.lastLoginAt)
        )
    }
    
    /**
     * Convert Firestore User to domain User
     */
    fun toDomain(firestoreUser: FirestoreUser): User {
        return User(
            id = firestoreUser.id,
            email = firestoreUser.email,
            displayName = firestoreUser.displayName,
            photoUrl = firestoreUser.photoUrl,
            createdAt = TimestampUtils.timestampToLong(firestoreUser.createdAt),
            lastLoginAt = TimestampUtils.timestampToLong(firestoreUser.lastLoginAt)
        )
    }
}