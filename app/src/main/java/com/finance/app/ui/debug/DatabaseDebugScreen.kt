package com.finance.app.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Debug screen to view database contents
 * Only available in debug builds
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseDebugScreen(
    onNavigateBack: () -> Unit,
    viewModel: DatabaseDebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Database Debug") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Users Section
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Users (${uiState.users.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.users.isEmpty()) {
                            Text("No users found", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            uiState.users.forEach { user ->
                                Text("• ${user.email} (${user.displayName ?: "No name"})")
                            }
                        }
                    }
                }
            }
            
            // Categories Section
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Categories (${uiState.categories.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.categories.isEmpty()) {
                            Text("No categories found", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            uiState.categories.forEach { category ->
                                Text("• ${category.name} ${if (category.isDefault) "(Default)" else "(Custom)"}")
                            }
                        }
                    }
                }
            }
            
            // Transactions Section
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Transactions (${uiState.transactions.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.transactions.isEmpty()) {
                            Text("No transactions found", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            uiState.transactions.take(10).forEach { transaction ->
                                Text("• ${transaction.type}: $${transaction.amount} (${transaction.categoryId})")
                            }
                            if (uiState.transactions.size > 10) {
                                Text("... and ${uiState.transactions.size - 10} more")
                            }
                        }
                    }
                }
            }
            
            // Refresh Button
            item {
                Button(
                    onClick = { viewModel.loadData() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh Data")
                }
            }
        }
    }
}