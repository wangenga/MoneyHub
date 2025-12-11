package com.finance.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.finance.app.domain.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinanceApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var syncScheduler: SyncScheduler
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize periodic sync on app startup
        // This ensures sync is scheduled even if user doesn't open the app frequently
        syncScheduler.schedulePeriodicSync()
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
