package com.finance.app

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.finance.app.data.sync.NetworkConnectivityChecker
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.sync.SyncScheduler
import com.finance.app.ui.navigation.AppNavigation
import com.finance.app.ui.theme.FinanceAppTheme
import com.finance.app.util.ActivityProvider
import com.finance.app.util.DatabaseDebugUtil
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var syncScheduler: SyncScheduler
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var databaseDebugUtil: DatabaseDebugUtil
    
    @Inject
    lateinit var networkConnectivityChecker: NetworkConnectivityChecker
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Debug connection status on startup
        if (BuildConfig.DEBUG) {
            debugConnectionStatus()
        }
        
        // Set activity for biometric authentication
        ActivityProvider.setActivity(this)
        
        setContent {
            FinanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
    
    private fun debugConnectionStatus() {
        try {
            Log.d("MainActivity", "=== CONNECTION DEBUG ===")
            
            // Check network connectivity
            val isNetworkAvailable = networkConnectivityChecker.isNetworkAvailable()
            val isWifiConnected = networkConnectivityChecker.isWifiConnected()
            val isCellularConnected = networkConnectivityChecker.isCellularConnected()
            
            Log.d("MainActivity", "Network available: $isNetworkAvailable")
            Log.d("MainActivity", "WiFi connected: $isWifiConnected")
            Log.d("MainActivity", "Cellular connected: $isCellularConnected")
            
            // Check Firebase initialization
            try {
                val firebaseApp = FirebaseApp.getInstance()
                Log.d("MainActivity", "Firebase app initialized: ${firebaseApp.name}")
                Log.d("MainActivity", "Firebase project ID: ${firebaseApp.options.projectId}")
                Log.d("MainActivity", "Firebase app ID: ${firebaseApp.options.applicationId}")
            } catch (e: Exception) {
                Log.e("MainActivity", "Firebase not initialized", e)
            }
            
            // Check Firebase Auth
            try {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                Log.d("MainActivity", "Firebase Auth current user: ${currentUser?.uid ?: "null"}")
            } catch (e: Exception) {
                Log.e("MainActivity", "Firebase Auth error", e)
            }
            
            // Check Firestore
            try {
                val firestore = FirebaseFirestore.getInstance()
                Log.d("MainActivity", "Firestore instance created successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Firestore initialization error", e)
            }
            
            Log.d("MainActivity", "=== END CONNECTION DEBUG ===")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during connection debug", e)
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Schedule foreground sync when app comes to foreground
        // Only if user is authenticated
        lifecycleScope.launch {
            val currentUser = authRepository.getCurrentUser().first()
            if (currentUser != null) {
                syncScheduler.scheduleForegroundSync()
            }
            
            // Debug: Log database contents (only in debug builds)
            if (BuildConfig.DEBUG) {
                databaseDebugUtil.logDatabaseSummary()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ActivityProvider.clearActivity()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FinanceAppTheme {
        Text("Personal Finance App")
    }
}