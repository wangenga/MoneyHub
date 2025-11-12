package com.finance.app.data.repository

import com.finance.app.domain.model.User
import com.finance.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Implementation of AuthRepository - to be fully implemented in task 3
 */
class AuthRepositoryImpl @Inject constructor() : AuthRepository {
    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        TODO("Implementation pending - task 3")
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        TODO("Implementation pending - task 3")
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        TODO("Implementation pending - task 3")
    }

    override suspend fun signOut() {
        TODO("Implementation pending - task 3")
    }

    override fun getCurrentUser(): Flow<User?> {
        return flowOf(null)
    }

    override fun isAuthenticated(): Flow<Boolean> {
        return flowOf(false)
    }
}
