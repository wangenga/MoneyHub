package com.finance.app.data.mapper

import com.finance.app.data.local.entity.UserEntity
import com.finance.app.domain.model.User

/**
 * Mapper functions to convert between User domain model and UserEntity
 * To be fully implemented in task 2
 */

fun UserEntity.toDomain(): User {
    return User(
        id = id,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        createdAt = createdAt,
        lastLoginAt = lastLoginAt
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        createdAt = createdAt,
        lastLoginAt = lastLoginAt
    )
}
