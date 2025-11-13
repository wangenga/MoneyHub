package com.finance.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Settings item for enabling/disabling biometric authentication
 */
@Composable
fun BiometricSettingsItem(
    viewModel: BiometricSettingsViewModel = hiltViewModel()
) {
    val isBiometricAvailable by viewModel.isBiometricAvailable.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val authenticationState by viewModel.authenticationState.collectAsState()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric lock",
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Biometric Lock",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Text(
                        text = if (isBiometricAvailable) {
                            "Secure app with fingerprint or face"
                        } else {
                            "Not available on this device"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Switch(
                checked = isBiometricEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        viewModel.enableBiometric()
                    } else {
                        viewModel.disableBiometric()
                    }
                },
                enabled = isBiometricAvailable
            )
        }
    }
    
    // Show snackbar for authentication result
    when (authenticationState) {
        is BiometricAuthState.Success -> {
            LaunchedEffect(Unit) {
                viewModel.resetAuthState()
            }
        }
        is BiometricAuthState.Error -> {
            LaunchedEffect(Unit) {
                // In a real app, show a Snackbar here
                viewModel.resetAuthState()
            }
        }
        else -> { /* Do nothing */ }
    }
}
