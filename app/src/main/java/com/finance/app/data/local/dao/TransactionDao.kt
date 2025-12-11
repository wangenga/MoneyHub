package com.finance.app.data.local.dao

import androidx.room.*
import com.finance.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for transaction operations
 */
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :start AND :end ORDER BY date DESC")
    fun getTransactionsByDateRange(userId: String, start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(userId: String, categoryId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type ORDER BY date DESC")
    fun getTransactionsByType(userId: String, type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND syncStatus = :syncStatus")
    fun getTransactionsBySyncStatus(userId: String, syncStatus: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND syncStatus = :syncStatus")
    suspend fun getTransactionsByStatus(userId: String, syncStatus: String): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    // Analytics queries
    @Query("""
        SELECT categoryId, SUM(amount) as total 
        FROM transactions 
        WHERE userId = :userId 
        AND type = 'EXPENSE' 
        AND date BETWEEN :startDate AND :endDate 
        GROUP BY categoryId
    """)
    fun getSpendingByCategory(userId: String, startDate: Long, endDate: Long): Flow<List<com.finance.app.data.local.model.CategorySpending>>

    @Query("""
        SELECT SUM(amount) 
        FROM transactions 
        WHERE userId = :userId 
        AND type = :type 
        AND date BETWEEN :startDate AND :endDate
    """)
    fun getTotalByType(userId: String, type: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT date, SUM(amount) as total 
        FROM transactions 
        WHERE userId = :userId 
        AND type = 'EXPENSE' 
        AND date BETWEEN :startDate AND :endDate 
        GROUP BY date 
        ORDER BY date ASC
    """)
    fun getSpendingTrend(userId: String, startDate: Long, endDate: Long): Flow<List<com.finance.app.data.local.model.DailySpending>>
}
