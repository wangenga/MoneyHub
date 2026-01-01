package com.finance.app

import android.os.Bundle
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
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.sync.SyncScheduler
import com.finance.app.ui.navigation.AppNavigation
import com.finance.app.ui.theme.FinanceAppTheme
import com.finance.app.util.ActivityProvider
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
    
    override fun onResume() {
        super.onResume()
        
        // Schedule foreground sync when app comes to foreground
        // Only if user is authenticated
        lifecycleScope.launch {
            val currentUser = authRepository.getCurrentUser().first()
            if (currentUser != null) {
                syncScheduler.scheduleForegroundSync()
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
