package com.finance.app.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.app.ui.components.FinanceButton
import com.finance.app.ui.components.FinanceCard
import com.finance.app.ui.components.FinanceTopAppBarWithBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionTestScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ConnectionTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            FinanceTopAppBarWithBack(
                title = "Connection Test",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Network Status
            FinanceCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Network Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Internet Available:")
                        Text(
                            text = if (uiState.isNetworkAvailable) "✅ Yes" else "❌ No",
                            color = if (uiState.isNetworkAvailable) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("WiFi Connected:")
                        Text(
                            text = if (uiState.isWifiConnected) "✅ Yes" else "❌ No",
                            color = if (uiState.isWifiConnected) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cellular Connected:")
                        Text(
                            text = if (uiState.isCellularConnected) "✅ Yes" else "❌ No",
                            color = if (uiState.isCellularConnected) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Firebase Status
            FinanceCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Firebase Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Firebase App:")
                        Text(
                            text = if (uiState.isFirebaseInitialized) "✅ Initialized" else "❌ Not Initialized",
                            color = if (uiState.isFirebaseInitialized) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Auth Status:")
                        Text(
                            text = when (uiState.authStatus) {
                                "authenticated" -> "✅ Authenticated"
                                "not_authenticated" -> "❌ Not Authenticated"
                                else -> "⏳ Checking..."
                            },
                            color = when (uiState.authStatus) {
                                "authenticated" -> MaterialTheme.colorScheme.primary
                                "not_authenticated" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    
                    if (uiState.authStatus == "authenticated") {
                        Text(
                            text = "User: ${uiState.userEmail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Test Buttons
            FinanceCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Connection Tests",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    FinanceButton(
                        text = "Test Firestore Connection",
                        onClick = { viewModel.testFirestoreConnection() },
                        enabled = !uiState.isTestingFirestore,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (uiState.isTestingFirestore) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Firestore...")
                        }
                    }
                    
                    FinanceButton(
                        text = "Test Authentication",
                        onClick = { viewModel.testAuthentication() },
                        enabled = !uiState.isTestingAuth,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (uiState.isTestingAuth) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Authentication...")
                        }
                    }
                }
            }
            
            // Test Results
            if (uiState.testResults.isNotEmpty()) {
                FinanceCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Test Results",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Text(
                            text = uiState.testResults,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }
            }
            
            // Refresh Button
            FinanceButton(
                text = "Refresh Status",
                onClick = { viewModel.refreshStatus() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}