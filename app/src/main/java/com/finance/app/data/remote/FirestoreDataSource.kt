package com.finance.app.data.remote

import com.finance.app.data.remote.mapper.FirestoreCategoryMapper
import com.finance.app.data.remote.mapper.FirestoreTransactionMapper
import com.finance.app.data.remote.mapper.FirestoreUserMapper
import com.finance.app.data.remote.model.FirestoreCategory
import com.finance.app.data.remote.model.FirestoreTransaction
import com.finance.app.data.remote.model.FirestoreUser
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore data source for remote operations
 * Handles all Firebase Firestore interactions for users, transactions, and categories
 */
@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TRANSACTIONS_COLLECTION = "transactions"
        private const val CATEGORIES_COLLECTION = "categories"
    }
    
    // User operations
    
    /**
     * Save user to Firestore
     */
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            val firestoreUser = FirestoreUserMapper.toFirestore(user)
            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(firestoreUser)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user from Firestore
     */
    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val firestoreUser = document.toObject(FirestoreUser::class.java)
                val user = firestoreUser?.let { FirestoreUserMapper.toDomain(it) }
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Transaction operations
    
    /**
     * Save transaction to Firestore
     */
    suspend fun saveTransaction(userId: String, transaction: Transaction): Result<Unit> {
        return try {
            val firestoreTransaction = FirestoreTransactionMapper.toFirestore(transaction)
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(TRANSACTIONS_COLLECTION)
                .document(transaction.id)
                .set(firestoreTransaction)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all transactions for a user from Firestore
     */
    suspend fun getTransactions(userId: String): Result<List<Transaction>> {
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(TRANSACTIONS_COLLECTION)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val transactions = querySnapshot.documents.mapNotNull { document ->
                document.toObject(FirestoreTransaction::class.java)?.let { firestoreTransaction ->
                    FirestoreTransactionMapper.toDomain(firestoreTransaction, userId)
                }
            }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get transactions updated after a specific timestamp
     */
    suspend fun getTransactionsUpdatedAfter(userId: String, timestamp: Long): Result<List<Transaction>> {
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(TRANSACTIONS_COLLECTION)
                .whereGreaterThan("updatedAt", com.google.firebase.Timestamp(timestamp / 1000, 0))
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val transactions = querySnapshot.documents.mapNotNull { document ->
                document.toObject(FirestoreTransaction::class.java)?.let { firestoreTransaction ->
                    FirestoreTransactionMapper.toDomain(firestoreTransaction, userId)
                }
            }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete transaction from Firestore
     */
    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(TRANSACTIONS_COLLECTION)
                .document(transactionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Category operations
    
    /**
     * Save category to Firestore
     */
    suspend fun saveCategory(userId: String, category: Category): Result<Unit> {
        return try {
            val firestoreCategory = FirestoreCategoryMapper.toFirestore(category)
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CATEGORIES_COLLECTION)
                .document(category.id)
                .set(firestoreCategory)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all categories for a user from Firestore
     */
    suspend fun getCategories(userId: String): Result<List<Category>> {
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CATEGORIES_COLLECTION)
                .orderBy("name")
                .get()
                .await()
            
            val categories = querySnapshot.documents.mapNotNull { document ->
                document.toObject(FirestoreCategory::class.java)?.let { firestoreCategory ->
                    FirestoreCategoryMapper.toDomain(firestoreCategory, userId)
                }
            }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get categories updated after a specific timestamp
     */
    suspend fun getCategoriesUpdatedAfter(userId: String, timestamp: Long): Result<List<Category>> {
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CATEGORIES_COLLECTION)
                .whereGreaterThan("updatedAt", com.google.firebase.Timestamp(timestamp / 1000, 0))
                .orderBy("updatedAt")
                .get()
                .await()
            
            val categories = querySnapshot.documents.mapNotNull { document ->
                document.toObject(FirestoreCategory::class.java)?.let { firestoreCategory ->
                    FirestoreCategoryMapper.toDomain(firestoreCategory, userId)
                }
            }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete category from Firestore
     */
    suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CATEGORIES_COLLECTION)
                .document(categoryId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Batch save multiple transactions
     */
    suspend fun saveTransactionsBatch(userId: String, transactions: List<Transaction>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val userDocRef = firestore.collection(USERS_COLLECTION).document(userId)
            
            transactions.forEach { transaction ->
                val firestoreTransaction = FirestoreTransactionMapper.toFirestore(transaction)
                val transactionRef = userDocRef.collection(TRANSACTIONS_COLLECTION).document(transaction.id)
                batch.set(transactionRef, firestoreTransaction)
            }
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Batch save multiple categories
     */
    suspend fun saveCategoriesBatch(userId: String, categories: List<Category>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val userDocRef = firestore.collection(USERS_COLLECTION).document(userId)
            
            categories.forEach { category ->
                val firestoreCategory = FirestoreCategoryMapper.toFirestore(category)
                val categoryRef = userDocRef.collection(CATEGORIES_COLLECTION).document(category.id)
                batch.set(categoryRef, firestoreCategory)
            }
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
