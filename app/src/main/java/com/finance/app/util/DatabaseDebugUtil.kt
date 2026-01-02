package com.finance.app.util

import android.util.Log
import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.local.dao.UserDao
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug utility to log database contents
 * Only use in debug builds
 */
@Singleton
class DatabaseDebugUtil @Inject constructor(
    private val userDao: UserDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    
    companion object {
        private const val TAG = "DatabaseDebug"
    }
    
    suspend fun logAllUsers() {
        try {
            val users = userDao.getAllUsers().first()
            Log.d(TAG, "=== USERS TABLE (${users.size} records) ===")
            users.forEach { user ->
                Log.d(TAG, "User: id=${user.id}, email=${user.email}, name=${user.displayName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging users", e)
        }
    }
    
    suspend fun logAllCategories() {
        try {
            val categories = categoryDao.getAllCategories().first()
            Log.d(TAG, "=== CATEGORIES TABLE (${categories.size} records) ===")
            categories.forEach { category ->
                Log.d(TAG, "Category: id=${category.id}, name=${category.name}, userId=${category.userId}, isDefault=${category.isDefault}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging categories", e)
        }
    }
    
    suspend fun logAllTransactions(userId: String? = null) {
        try {
            val transactions = if (userId != null) {
                transactionDao.getAllTransactions(userId).first()
            } else {
                // Get all transactions for debugging
                transactionDao.getAllTransactionsForDebug().first()
            }
            
            Log.d(TAG, "=== TRANSACTIONS TABLE (${transactions.size} records) ===")
            transactions.forEach { transaction ->
                Log.d(TAG, "Transaction: id=${transaction.id}, type=${transaction.type}, amount=${transaction.amount}, categoryId=${transaction.categoryId}, userId=${transaction.userId}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging transactions", e)
        }
    }
    
    suspend fun logDatabaseSummary() {
        Log.d(TAG, "=== DATABASE SUMMARY ===")
        logAllUsers()
        logAllCategories()
        logAllTransactions()
        Log.d(TAG, "=== END DATABASE SUMMARY ===")
    }
}