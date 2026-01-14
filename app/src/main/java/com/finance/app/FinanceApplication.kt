package com.finance.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.finance.app.domain.repository.BudgetRepository
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.domain.repository.RecurringTransactionRepository
import com.finance.app.domain.sync.SyncScheduler
import com.finance.app.domain.usecase.RecurringTransactionScheduler
import com.finance.app.util.ActivityProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FinanceApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var syncScheduler: SyncScheduler
    
    @Inject
    lateinit var recurringTransactionScheduler: RecurringTransactionScheduler
    
    @Inject
    lateinit var activityProvider: ActivityProvider
    
    @Inject
    lateinit var categoryRepository: CategoryRepository
    
    @Inject
    lateinit var budgetRepository: BudgetRepository
    
    @Inject
    lateinit var recurringTransactionRepository: RecurringTransactionRepository
    
    // Application scope for initialization tasks
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Register activity lifecycle callbacks for biometric authentication
        activityProvider.register(this)
        
        // Initialize Firebase and enable debug logging
        try {
            FirebaseApp.initializeApp(this)
            if (BuildConfig.DEBUG) {
                FirebaseFirestore.setLoggingEnabled(true)
                Log.d("FinanceApp", "Firebase initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("FinanceApp", "Firebase initialization failed", e)
        }
        
        // Initialize default categories on app startup
        applicationScope.launch {
            try {
                categoryRepository.initializeDefaultCategories().fold(
                    onSuccess = {
                        Log.d("FinanceApp", "Default categories initialized successfully")
                    },
                    onFailure = { error ->
                        Log.e("FinanceApp", "Failed to initialize default categories", error)
                    }
                )
            } catch (e: Exception) {
                Log.e("FinanceApp", "Default category initialization failed", e)
            }
        }
        
        // Load budget configurations on app startup
        // This ensures budget data is available for calculations and alerts
        applicationScope.launch {
            try {
                // Budget data is loaded reactively through Flow, so we just need to
                // ensure the repository is initialized and ready
                Log.d("FinanceApp", "Budget repository initialized and ready")
            } catch (e: Exception) {
                Log.e("FinanceApp", "Budget repository initialization failed", e)
            }
        }
        
        // Load recurring transaction configurations on app startup
        // This ensures recurring transaction data is restored and monitoring resumes
        applicationScope.launch {
            try {
                // Recurring transaction data is loaded reactively through Flow
                // The scheduler will pick up active recurring transactions automatically
                Log.d("FinanceApp", "Recurring transaction repository initialized and ready")
            } catch (e: Exception) {
                Log.e("FinanceApp", "Recurring transaction repository initialization failed", e)
            }
        }
        
        // Initialize periodic sync on app startup
        // This ensures sync is scheduled even if user doesn't open the app frequently
        try {
            syncScheduler.schedulePeriodicSync()
            Log.d("FinanceApp", "Sync scheduler initialized")
        } catch (e: Exception) {
            Log.e("FinanceApp", "Sync scheduler initialization failed", e)
        }
        
        // Initialize recurring transaction scheduler on app startup
        // This ensures recurring transactions are monitored and executed
        applicationScope.launch {
            try {
                recurringTransactionScheduler.startScheduling()
                Log.d("FinanceApp", "Recurring transaction scheduler initialized")
            } catch (e: Exception) {
                Log.e("FinanceApp", "Recurring transaction scheduler initialization failed", e)
            }
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
