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
import com.finance.app.ui.auth.AuthNavigation
import com.finance.app.ui.theme.FinanceAppTheme
import com.finance.app.util.ActivityProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
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
                    // Auth navigation - will be replaced with main app navigation after authentication
                    AuthNavigation(
                        onAuthenticationComplete = {
                            // TODO: Navigate to main app screen (will be implemented in later tasks)
                        },
                        onGoogleSignIn = {
                            // TODO: Implement Google Sign-In flow (requires Google Sign-In setup)
                        }
                    )
                }
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
