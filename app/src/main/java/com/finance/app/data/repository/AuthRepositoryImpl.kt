package com.finance.app.data.repository

import com.finance.app.data.local.dao.UserDao
import com.finance.app.data.mapper.toDomain
import com.finance.app.data.mapper.toEntity
import com.finance.app.domain.model.User
import com.finance.app.domain.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementation of AuthRepository using Firebase Authentication
 */
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient,
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Authentication failed: User is null"))

            val user = firebaseUser.toUser()
            
            // Save user to local database
            userDao.insert(user.toEntity())
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Google Sign-In failed: User is null"))

            val user = firebaseUser.toUser()
            
            // Save user to local database
            userDao.insert(user.toEntity())
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Registration failed: User is null"))

            val user = firebaseUser.toUser()
            
            // Save user to local database
            userDao.insert(user.toEntity())
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            // Revoke access to force account picker on next login
            // revokeAccess() disconnects the app completely, ensuring fresh account selection
            googleSignInClient.revokeAccess().await()
            // Clear local user data
            userDao.deleteAll()
        } catch (e: Exception) {
            // Log error but don't throw - sign out should always succeed locally
        }
    }

    override fun getCurrentUser(): Flow<User?> {
        return callbackFlow {
            val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                val firebaseUser = auth.currentUser
                val user = firebaseUser?.toUser()
                trySend(user)
            }
            
            firebaseAuth.addAuthStateListener(authStateListener)
            
            awaitClose {
                firebaseAuth.removeAuthStateListener(authStateListener)
            }
        }
    }

    override fun isAuthenticated(): Flow<Boolean> {
        return getCurrentUser().map { it != null }
    }

    /**
     * Converts FirebaseUser to domain User model
     */
    private fun FirebaseUser.toUser(): User {
        val currentTime = System.currentTimeMillis()
        return User(
            id = uid,
            email = email ?: "",
            displayName = displayName,
            photoUrl = photoUrl?.toString(),
            createdAt = metadata?.creationTimestamp ?: currentTime,
            lastLoginAt = metadata?.lastSignInTimestamp ?: currentTime
        )
    }
}
