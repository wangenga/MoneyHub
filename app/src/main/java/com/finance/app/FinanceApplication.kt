package com.finance.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.finance.app.domain.sync.SyncScheduler
import com.finance.app.util.ActivityProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinanceApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var syncScheduler: SyncScheduler
    
    @Inject
    lateinit var activityProvider: ActivityProvider
    
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
        
        // Initialize periodic sync on app startup
        // This ensures sync is scheduled even if user doesn't open the app frequently
        try {
            syncScheduler.schedulePeriodicSync()
            Log.d("FinanceApp", "Sync scheduler initialized")
        } catch (e: Exception) {
            Log.e("FinanceApp", "Sync scheduler initialization failed", e)
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
