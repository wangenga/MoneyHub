package com.finance.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing user information in local database
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val createdAt: Long,
    val lastLoginAt: Long
)
