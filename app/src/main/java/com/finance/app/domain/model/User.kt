package com.finance.app.domain.model

/**
 * Domain model representing a user in the system
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val createdAt: Long,
    val lastLoginAt: Long
)
