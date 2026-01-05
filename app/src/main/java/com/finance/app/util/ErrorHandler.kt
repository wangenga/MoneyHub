package com.finance.app.util

import android.content.Context
import com.finance.app.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling utility that maps exceptions to user-friendly messages
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val context: Context
) {
    
    /**
     * Maps an exception to a user-friendly error message
     */
    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            // Network errors
            is UnknownHostException -> context.getString(R.string.error_no_internet)
            is SocketTimeoutException -> context.getString(R.string.error_network_timeout)
            is IOException -> context.getString(R.string.error_network_general)
            
            // Firebase Auth errors
            is FirebaseAuthException -> mapFirebaseAuthError(throwable)
            
            // Firestore errors
            is FirebaseFirestoreException -> mapFirestoreError(throwable)
            
            // Firebase general errors
            is FirebaseException -> context.getString(R.string.error_firebase_general)
            
            // Database errors
            is android.database.sqlite.SQLiteException -> context.getString(R.string.error_database)
            
            // Validation errors
            is IllegalArgumentException -> throwable.message ?: context.getString(R.string.error_validation)
            
            // Generic errors
            else -> throwable.message ?: context.getString(R.string.error_unknown)
        }
    }
    
    /**
     * Maps Firebase Auth exceptions to user-friendly messages
     */
    private fun mapFirebaseAuthError(exception: FirebaseAuthException): String {
        return when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> context.getString(R.string.error_invalid_email)
            "ERROR_WRONG_PASSWORD" -> context.getString(R.string.error_wrong_password)
            "ERROR_USER_NOT_FOUND" -> context.getString(R.string.error_user_not_found)
            "ERROR_USER_DISABLED" -> context.getString(R.string.error_user_disabled)
            "ERROR_TOO_MANY_REQUESTS" -> context.getString(R.string.error_too_many_requests)
            "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.error_email_in_use)
            "ERROR_WEAK_PASSWORD" -> context.getString(R.string.error_weak_password)
            "ERROR_NETWORK_REQUEST_FAILED" -> context.getString(R.string.error_no_internet)
            else -> exception.message ?: context.getString(R.string.error_auth_general)
        }
    }
    
    /**
     * Maps Firestore exceptions to user-friendly messages
     */
    private fun mapFirestoreError(exception: FirebaseFirestoreException): String {
        return when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> 
                context.getString(R.string.error_permission_denied)
            FirebaseFirestoreException.Code.UNAVAILABLE -> 
                context.getString(R.string.error_service_unavailable)
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> 
                context.getString(R.string.error_network_timeout)
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> 
                context.getString(R.string.error_authentication_required)
            else -> exception.message ?: context.getString(R.string.error_sync_failed)
        }
    }
    
    /**
     * Determines if an error is recoverable (can be retried)
     */
    fun isRecoverableError(throwable: Throwable): Boolean {
        return when (throwable) {
            is UnknownHostException,
            is SocketTimeoutException,
            is IOException -> true
            
            is FirebaseFirestoreException -> when (throwable.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.INTERNAL -> true
                else -> false
            }
            
            is FirebaseAuthException -> when (throwable.errorCode) {
                "ERROR_NETWORK_REQUEST_FAILED",
                "ERROR_TOO_MANY_REQUESTS" -> true
                else -> false
            }
            
            else -> false
        }
    }
    
    /**
     * Gets retry delay in milliseconds based on error type
     */
    fun getRetryDelay(throwable: Throwable, attempt: Int): Long {
        val baseDelay = when (throwable) {
            is SocketTimeoutException -> 2000L
            is UnknownHostException -> 5000L
            is FirebaseAuthException -> if (throwable.errorCode == "ERROR_TOO_MANY_REQUESTS") 10000L else 3000L
            else -> 3000L
        }
        
        // Exponential backoff with jitter
        val exponentialDelay = baseDelay * (1L shl attempt.coerceAtMost(5))
        val jitter = (Math.random() * 1000).toLong()
        return (exponentialDelay + jitter).coerceAtMost(30000L)
    }
}