package com.finance.app.data.sync

import android.content.Context
import androidx.work.*
import com.finance.app.domain.repository.RecurringTransactionRepository
import com.finance.app.domain.repository.TransactionRepository
import com.finance.app.domain.usecase.RecurringTransactionScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of RecurringTransactionScheduler using WorkManager
 * Handles scheduling and execution of recurring transactions
 */
@Singleton
class RecurringTransactionSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository
) : RecurringTransactionScheduler {

    private val workManager = WorkManager.getInstance(context)
    
    companion object {
        private const val PERIODIC_CHECK_INTERVAL_HOURS = 1L
        private const val INDIVIDUAL_TRANSACTION_TAG_PREFIX = "recurring_transaction_"
    }

    override suspend fun startScheduling() = withContext(Dispatchers.IO) {
        // Schedule periodic work to check for due recurring transactions
        val periodicWorkRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            PERIODIC_CHECK_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Can work offline
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag(RecurringTransactionWorker.PERIODIC_WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            RecurringTransactionWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
        
        // Schedule immediate check for any overdue transactions
        scheduleImmediateCheck()
    }

    override suspend fun stopScheduling(): Unit = withContext(Dispatchers.IO) {
        // Cancel periodic work
        workManager.cancelUniqueWork(RecurringTransactionWorker.PERIODIC_WORK_NAME)
        
        // Cancel all individual recurring transaction work
        workManager.cancelAllWorkByTag(RecurringTransactionWorker.WORK_NAME)
    }

    override suspend fun scheduleRecurringTransaction(recurringTransactionId: String): Unit = withContext(Dispatchers.IO) {
        try {
            // Get the recurring transaction to determine when it's next due
            val dueTransactions = recurringTransactionRepository.getNextDueTransactions()
            val recurringTransaction = dueTransactions.find { it.id == recurringTransactionId }
            
            if (recurringTransaction != null && recurringTransaction.isActive) {
                val currentTime = System.currentTimeMillis()
                val delay = maxOf(0L, recurringTransaction.nextDueDate - currentTime)
                
                val workRequest = OneTimeWorkRequestBuilder<RecurringTransactionWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(
                        Data.Builder()
                            .putString(RecurringTransactionWorker.RECURRING_TRANSACTION_ID_KEY, recurringTransactionId)
                            .build()
                    )
                    .addTag(INDIVIDUAL_TRANSACTION_TAG_PREFIX + recurringTransactionId)
                    .addTag(RecurringTransactionWorker.WORK_NAME)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                            .build()
                    )
                    .build()

                workManager.enqueueUniqueWork(
                    INDIVIDUAL_TRANSACTION_TAG_PREFIX + recurringTransactionId,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("RecurringTransactionScheduler", "Error scheduling recurring transaction: $recurringTransactionId", e)
        }
    }

    override suspend fun cancelRecurringTransaction(recurringTransactionId: String): Unit = withContext(Dispatchers.IO) {
        workManager.cancelUniqueWork(INDIVIDUAL_TRANSACTION_TAG_PREFIX + recurringTransactionId)
    }

    override suspend fun processDueTransactions(): Unit = withContext(Dispatchers.IO) {
        try {
            val dueTransactions = recurringTransactionRepository.getNextDueTransactions()
            
            for (recurringTransaction in dueTransactions) {
                if (recurringTransaction.isActive) {
                    // Create transaction instance
                    val newTransaction = recurringTransaction.createTransactionInstance()
                    
                    // Insert the transaction
                    val insertResult = transactionRepository.insertTransaction(newTransaction)
                    
                    if (insertResult.isSuccess) {
                        // Calculate and update next due date
                        val nextDueDate = recurringTransaction.calculateNextDueDate()
                        recurringTransactionRepository.updateNextDueDate(
                            recurringTransaction.id,
                            nextDueDate
                        )
                        
                        // Reschedule this specific recurring transaction
                        scheduleRecurringTransaction(recurringTransaction.id)
                        
                        android.util.Log.d("RecurringTransactionScheduler", "Processed recurring transaction: ${recurringTransaction.id}")
                    } else {
                        android.util.Log.e("RecurringTransactionScheduler", "Failed to create transaction for recurring transaction: ${recurringTransaction.id}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RecurringTransactionScheduler", "Error processing due transactions", e)
        }
    }

    override suspend fun rescheduleAllTransactions(): Unit = withContext(Dispatchers.IO) {
        try {
            // Cancel all existing individual work
            workManager.cancelAllWorkByTag(RecurringTransactionWorker.WORK_NAME)
            
            // Get all active recurring transactions and schedule them
            val allRecurringTransactions = recurringTransactionRepository.getNextDueTransactions()
            
            for (recurringTransaction in allRecurringTransactions) {
                if (recurringTransaction.isActive) {
                    scheduleRecurringTransaction(recurringTransaction.id)
                }
            }
            
            android.util.Log.d("RecurringTransactionScheduler", "Rescheduled ${allRecurringTransactions.size} recurring transactions")
        } catch (e: Exception) {
            android.util.Log.e("RecurringTransactionScheduler", "Error rescheduling all transactions", e)
        }
    }
    
    /**
     * Schedule an immediate check for due transactions
     */
    private fun scheduleImmediateCheck() {
        val immediateWorkRequest = OneTimeWorkRequestBuilder<RecurringTransactionWorker>()
            .addTag(RecurringTransactionWorker.WORK_NAME)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            "immediate_recurring_check",
            ExistingWorkPolicy.REPLACE,
            immediateWorkRequest
        )
    }
}