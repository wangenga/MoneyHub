package com.finance.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finance.app.domain.repository.RecurringTransactionRepository
import com.finance.app.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for processing due recurring transactions
 * Runs in the background to create transaction instances from recurring transaction templates
 */
@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "recurring_transaction_work"
        const val PERIODIC_WORK_NAME = "periodic_recurring_transaction_work"
        
        // Input data keys
        const val RECURRING_TRANSACTION_ID_KEY = "recurring_transaction_id"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            // Get specific recurring transaction ID if provided
            val specificId = inputData.getString(RECURRING_TRANSACTION_ID_KEY)
            
            if (specificId != null) {
                // Process specific recurring transaction
                processSpecificRecurringTransaction(specificId)
            } else {
                // Process all due recurring transactions
                processAllDueRecurringTransactions()
            }
        } catch (e: Exception) {
            android.util.Log.e("RecurringTransactionWorker", "Error processing recurring transactions", e)
            Result.retry()
        }
    }
    
    /**
     * Process a specific recurring transaction by ID
     */
    private suspend fun processSpecificRecurringTransaction(recurringTransactionId: String): Result {
        return try {
            val dueTransactions = recurringTransactionRepository.getNextDueTransactions()
            val targetTransaction = dueTransactions.find { it.id == recurringTransactionId }
            
            if (targetTransaction != null && targetTransaction.isActive) {
                val success = processRecurringTransaction(targetTransaction)
                if (success) Result.success() else Result.retry()
            } else {
                // Transaction not found or not due - this is success
                Result.success()
            }
        } catch (e: Exception) {
            android.util.Log.e("RecurringTransactionWorker", "Error processing specific recurring transaction: $recurringTransactionId", e)
            Result.retry()
        }
    }
    
    /**
     * Process all due recurring transactions
     */
    private suspend fun processAllDueRecurringTransactions(): Result {
        return try {
            val dueTransactions = recurringTransactionRepository.getNextDueTransactions()
            var allSuccessful = true
            
            for (recurringTransaction in dueTransactions) {
                if (recurringTransaction.isActive) {
                    val success = processRecurringTransaction(recurringTransaction)
                    if (!success) {
                        allSuccessful = false
                        android.util.Log.w("RecurringTransactionWorker", "Failed to process recurring transaction: ${recurringTransaction.id}")
                    }
                }
            }
            
            if (allSuccessful) Result.success() else Result.retry()
        } catch (e: Exception) {
            android.util.Log.e("RecurringTransactionWorker", "Error processing all due recurring transactions", e)
            Result.retry()
        }
    }
    
    /**
     * Process a single recurring transaction
     * Creates a new transaction instance and updates the next due date
     */
    private suspend fun processRecurringTransaction(recurringTransaction: com.finance.app.domain.model.RecurringTransaction): Boolean {
        return try {
            // Create transaction instance from template
            val newTransaction = recurringTransaction.createTransactionInstance()
            
            // Insert the new transaction
            val insertResult = transactionRepository.insertTransaction(newTransaction)
            
            if (insertResult.isSuccess) {
                // Calculate next due date
                val nextDueDate = recurringTransaction.calculateNextDueDate()
                
                // Update the recurring transaction with new next due date
                val updateResult = recurringTransactionRepository.updateNextDueDate(
                    recurringTransaction.id, 
                    nextDueDate
                )
                
                if (updateResult.isSuccess) {
                    android.util.Log.d("RecurringTransactionWorker", "Successfully processed recurring transaction: ${recurringTransaction.id}")
                    true
                } else {
                    android.util.Log.e("RecurringTransactionWorker", "Failed to update next due date for recurring transaction: ${recurringTransaction.id}")
                    false
                }
            } else {
                android.util.Log.e("RecurringTransactionWorker", "Failed to insert transaction for recurring transaction: ${recurringTransaction.id}")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("RecurringTransactionWorker", "Exception processing recurring transaction: ${recurringTransaction.id}", e)
            false
        }
    }
}