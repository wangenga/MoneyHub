package com.finance.app.domain.repository

import com.finance.app.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signOut()
    fun getCurrentUser(): Flow<User?>
    fun isAuthenticated(): Flow<Boolean>
}
