package com.finance.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.app.domain.repository.SyncState
import com.finance.app.ui.components.FinanceButton
import com.finance.app.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings screen composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle biometric auth state
    LaunchedEffect(uiState.biometricAuthState) {
        val authState = uiState.biometricAuthState
        when (authState) {
            is BiometricAuthState.Error -> {
                snackbarHostState.showSnackbar(authState.message)
                viewModel.clearBiometricAuthState()
            }
            is BiometricAuthState.Success -> {
                snackbarHostState.showSnackbar("Biometric lock enabled")
                viewModel.clearBiometricAuthState()
            }
            else -> {}
        }
    }

    // Handle sync errors
    LaunchedEffect(uiState.syncError) {
        uiState.syncError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearSyncError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Settings
            SettingsSection(
                title = "Appearance",
                icon = Icons.Default.Palette
            ) {
                ThemeSettingsContent(
                    themeMode = uiState.themeMode,
                    isDynamicColorEnabled = uiState.isDynamicColorEnabled,
                    onThemeModeChanged = viewModel::setThemeMode,
                    onDynamicColorChanged = viewModel::setDynamicColorEnabled
                )
            }

            // Security Settings
            SettingsSection(
                title = "Security",
                icon = Icons.Default.Fingerprint
            ) {
                BiometricSettingsContent(
                    isAvailable = uiState.isBiometricAvailable,
                    isEnabled = uiState.isBiometricEnabled,
                    isLoading = uiState.biometricAuthState is BiometricAuthState.Loading,
                    onToggle = viewModel::setBiometricEnabled
                )
            }

            // Sync Settings
            SettingsSection(
                title = "Synchronization",
                icon = Icons.Default.Sync
            ) {
                SyncSettingsContent(
                    syncState = uiState.syncState,
                    lastSyncTimestamp = uiState.lastSyncTimestamp,
                    isSyncing = uiState.isSyncing,
                    onManualSync = viewModel::triggerManualSync
                )
            }

            // App Info
            SettingsSection(
                title = "About",
                icon = Icons.Default.Info
            ) {
                AppInfoContent(
                    appVersion = uiState.appVersion
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
private fun ThemeSettingsContent(
    themeMode: ThemeMode,
    isDynamicColorEnabled: Boolean,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit
) {
    Column {
        Text(
            text = "Theme Mode",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ThemeMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChanged(mode) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = themeMode == mode,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (mode) {
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                        ThemeMode.SYSTEM -> "System Default"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dynamic Colors",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Use colors from your wallpaper",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isDynamicColorEnabled,
                onCheckedChange = onDynamicColorChanged
            )
        }
    }
}

@Composable
private fun BiometricSettingsContent(
    isAvailable: Boolean,
    isEnabled: Boolean,
    isLoading: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Biometric Lock",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isAvailable) {
                    "Secure app with fingerprint or face unlock"
                } else {
                    "Biometric authentication not available"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                enabled = isAvailable
            )
        }
    }
}

@Composable
private fun SyncSettingsContent(
    syncState: SyncState,
    lastSyncTimestamp: Long?,
    isSyncing: Boolean,
    onManualSync: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sync Status",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when (syncState) {
                        is SyncState.Idle -> "Ready to sync"
                        is SyncState.Syncing -> "Syncing..."
                        is SyncState.Success -> "Last sync successful"
                        is SyncState.Error -> "Sync failed: ${syncState.message}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (syncState) {
                        is SyncState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            if (syncState is SyncState.Success) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Sync successful",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        lastSyncTimestamp?.let { timestamp ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Last sync: ${formatTimestamp(timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        FinanceButton(
            text = if (isSyncing) "Syncing..." else "Sync Now",
            onClick = onManualSync,
            enabled = !isSyncing,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppInfoContent(
    appVersion: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Version",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = appVersion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
