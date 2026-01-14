package com.finance.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.app.domain.repository.SyncState
import com.finance.app.ui.common.UserFeedbackManager
import com.finance.app.ui.components.*
import com.finance.app.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Settings screen composable with enhanced error handling and user feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit = {},
    onNavigateToBudgetManagement: () -> Unit = {},
    onNavigateToRecurringTransactions: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val userFeedbackManager = remember(snackbarHostState, scope) {
        UserFeedbackManager(snackbarHostState, scope)
    }
    
    // Observe logout event for navigation
    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect {
            onLogout()
        }
    }
    
    Scaffold(
        topBar = {
            FinanceTopAppBar(
                title = "Settings"
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            item {
                SettingsSection(title = "Appearance") {
                    // Theme Selection
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        subtitle = when (uiState.themeMode) {
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.SYSTEM -> "System default"
                        },
                        onClick = { showThemeDialog = true }
                    )
                    
                    // Dynamic Color Toggle
                    SettingsToggleItem(
                        icon = Icons.Default.ColorLens,
                        title = "Dynamic Colors",
                        subtitle = "Use colors from your wallpaper",
                        checked = uiState.isDynamicColorEnabled,
                        onCheckedChange = viewModel::setDynamicColorEnabled
                    )
                }
            }
            
            // Financial Management Section
            item {
                SettingsSection(title = "Financial Management") {
                    // Budget Management
                    SettingsItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Budget Management",
                        subtitle = "Set and manage category budgets",
                        onClick = onNavigateToBudgetManagement
                    )
                    
                    // Recurring Transactions
                    SettingsItem(
                        icon = Icons.Default.Repeat,
                        title = "Recurring Transactions",
                        subtitle = "Manage automated transactions",
                        onClick = onNavigateToRecurringTransactions
                    )
                }
            }
            
            // Security Section
            item {
                SettingsSection(title = "Security") {
                    // Biometric Lock Toggle
                    SettingsToggleItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Lock",
                        subtitle = if (uiState.isBiometricAvailable) {
                            "Secure app with fingerprint or face"
                        } else {
                            "Not available on this device"
                        },
                        checked = uiState.isBiometricEnabled,
                        onCheckedChange = viewModel::setBiometricEnabled,
                        enabled = uiState.isBiometricAvailable
                    )
                }
            }
            
            // Sync Section
            item {
                SettingsSection(title = "Synchronization") {
                    // Manual Sync Button
                    SettingsActionItem(
                        icon = Icons.Default.Sync,
                        title = "Sync Now",
                        subtitle = when (uiState.syncState) {
                            is SyncState.Idle -> {
                                uiState.lastSyncTimestamp?.let { timestamp ->
                                    "Last synced: ${formatSyncTimestamp(timestamp)}"
                                } ?: "Never synced"
                            }
                            is SyncState.Syncing -> "Syncing..."
                            is SyncState.Success -> "Sync completed successfully"
                            is SyncState.Error -> {
                                val errorState = uiState.syncState as SyncState.Error
                                "Sync failed: ${errorState.message}"
                            }
                        },
                        onClick = viewModel::triggerManualSync,
                        isLoading = uiState.isSyncing,
                        enabled = !uiState.isSyncing
                    )
                    
                    // Sync Status
                    SettingsInfoItem(
                        icon = Icons.Default.CloudDone,
                        title = "Sync Status",
                        subtitle = when (uiState.syncState) {
                            is SyncState.Idle -> "Ready"
                            is SyncState.Syncing -> "In progress..."
                            is SyncState.Success -> "Up to date"
                            is SyncState.Error -> "Error occurred"
                        }
                    )
                }
            }
            
            // Account Section
            item {
                SettingsSection(title = "Account") {
                    // Logout Button
                    SettingsActionItem(
                        icon = Icons.Default.ExitToApp,
                        title = "Log Out",
                        subtitle = "Sign out of your account",
                        onClick = { showLogoutDialog = true },
                        isLoading = uiState.isLoggingOut,
                        enabled = !uiState.isLoggingOut
                    )
                }
            }
            
            // About Section
            item {
                SettingsSection(title = "About") {
                    // App Version
                    SettingsInfoItem(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = uiState.appVersion
                    )
                }
            }
            
            // Debug Section (only in debug builds)
            item {
                SettingsSection(title = "Debug") {
                    // Connection Test
                    SettingsItem(
                        icon = Icons.Default.NetworkCheck,
                        title = "Connection Test",
                        subtitle = "Test Firebase and network connectivity",
                        onClick = { /* TODO: Navigate to connection test */ }
                    )
                    
                    // Database Debug
                    SettingsItem(
                        icon = Icons.Default.Storage,
                        title = "Database Debug",
                        subtitle = "View database status and debug info",
                        onClick = { /* TODO: Navigate to database debug */ }
                    )
                }
            }
        }
    }
    
    // Theme Selection Dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.themeMode,
            onThemeSelected = { theme ->
                viewModel.setThemeMode(theme)
                showThemeDialog = false
                userFeedbackManager.showSuccess("Theme changed to ${theme.name.lowercase()}")
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
    
    // Handle biometric authentication state
    LaunchedEffect(uiState.biometricAuthState) {
        when (uiState.biometricAuthState) {
            is BiometricAuthState.Success -> {
                userFeedbackManager.showSuccess("Biometric authentication enabled")
                viewModel.clearBiometricAuthState()
            }
            is BiometricAuthState.Error -> {
                val errorState = uiState.biometricAuthState as BiometricAuthState.Error
                userFeedbackManager.showError(
                    message = "Biometric authentication failed: ${errorState.message}",
                    actionLabel = "Retry",
                    onAction = { viewModel.setBiometricEnabled(true) }
                )
                viewModel.clearBiometricAuthState()
            }
            else -> { /* Do nothing */ }
        }
    }
    
    // Handle sync state changes
    LaunchedEffect(uiState.syncState) {
        when (val syncState = uiState.syncState) {
            is SyncState.Success -> {
                userFeedbackManager.showSuccess("Data synced successfully")
            }
            is SyncState.Error -> {
                userFeedbackManager.showRetryableError(
                    message = "Sync failed: ${syncState.message}",
                    onRetry = { viewModel.triggerManualSync() }
                )
            }
            else -> { /* Do nothing */ }
        }
    }
    
    // Handle sync errors
    uiState.syncError?.let { error ->
        LaunchedEffect(error) {
            userFeedbackManager.showError(error)
            viewModel.clearSyncError()
        }
    }
    
    // Handle logout errors
    uiState.logoutError?.let { error ->
        LaunchedEffect(error) {
            userFeedbackManager.showRetryableError(
                message = "Logout failed: $error",
                onRetry = { viewModel.logout() }
            )
            viewModel.clearLogoutError()
        }
    }
}

/**
 * Settings section with title and content with accessibility support
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        FinanceText(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        FinanceCard {
            content()
        }
    }
}

/**
 * Clickable settings item with accessibility support
 */
@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentDesc = "$title: $subtitle. Double tap to change."
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Change $title",
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = contentDesc
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Row already has content description
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            FinanceText(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            FinanceText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null, // Decorative
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Settings item with toggle switch and accessibility support
 */
@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val contentDesc = buildString {
        append(title)
        append(": ")
        append(subtitle)
        append(". ")
        append(if (checked) "Enabled" else "Disabled")
        if (enabled) {
            append(". Double tap to toggle.")
        } else {
            append(". Not available.")
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = contentDesc
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Row already has content description
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            FinanceText(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            FinanceText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = if (enabled) {
                    "Toggle $title, currently ${if (checked) "enabled" else "disabled"}"
                } else {
                    "$title not available"
                }
            }
        )
    }
}

/**
 * Settings item with action button and accessibility support
 */
@Composable
private fun SettingsActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val contentDesc = buildString {
        append(title)
        append(": ")
        append(subtitle)
        if (enabled && !isLoading) {
            append(". Double tap to activate.")
        } else if (isLoading) {
            append(". Currently in progress.")
        } else {
            append(". Not available.")
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled && !isLoading,
                onClickLabel = title,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = contentDesc
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null, // Row already has content description
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            FinanceText(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            FinanceText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Settings item for displaying information only with accessibility support
 */
@Composable
private fun SettingsInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val contentDesc = "$title: $subtitle"
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = contentDesc
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Row already has content description
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            FinanceText(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            FinanceText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Theme selection dialog with accessibility support
 */
@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            FinanceText(
                text = "Choose Theme",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                ThemeMode.values().forEach { theme ->
                    val isSelected = currentTheme == theme
                    val themeLabel = when (theme) {
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                        ThemeMode.SYSTEM -> "System default"
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { onThemeSelected(theme) },
                                role = Role.RadioButton
                            )
                            .semantics {
                                contentDescription = if (isSelected) {
                                    "$themeLabel theme, selected"
                                } else {
                                    "Select $themeLabel theme"
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null // Handled by Row
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        FinanceText(
                            text = themeLabel,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            FinanceTextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Close theme selection dialog"
                }
            ) {
                ButtonText("Cancel")
            }
        }
    )
}

/**
 * Format sync timestamp for display
 */
private fun formatSyncTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}

/**
 * Logout confirmation dialog with accessibility support
 */
@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            FinanceText(
                text = "Log Out",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            FinanceText(
                text = "Are you sure you want to log out?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            FinanceTextButton(
                onClick = onConfirm,
                modifier = Modifier.semantics {
                    contentDescription = "Confirm logout"
                }
            ) {
                ButtonText("Log Out")
            }
        },
        dismissButton = {
            FinanceTextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel logout"
                }
            ) {
                ButtonText("Cancel")
            }
        }
    )
}