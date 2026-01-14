package com.finance.app.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.RecurringTransaction
import com.finance.app.domain.model.RecurrencePattern
import com.finance.app.domain.model.TransactionType
import com.finance.app.ui.common.UiState
import com.finance.app.ui.common.UserFeedbackManager
import com.finance.app.ui.components.ErrorDisplay
import com.finance.app.ui.components.OfflineIndicator
import com.finance.app.util.CurrencyUtils
import com.finance.app.util.rememberCachedIcon
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen for managing recurring transactions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionScreen(
    onAddRecurringTransaction: () -> Unit,
    onEditRecurringTransaction: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RecurringTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val networkState by viewModel.networkState.collectAsState()
    val globalError by viewModel.globalError.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val userFeedbackManager = remember(snackbarHostState, scope) {
        UserFeedbackManager(snackbarHostState, scope)
    }

    // Handle global errors
    LaunchedEffect(globalError) {
        globalError?.let { error ->
            userFeedbackManager.showError(error)
            viewModel.clearGlobalError()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Recurring Transactions") },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.semantics {
                                contentDescription = "Navigate back"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                )
                
                // Offline indicator
                OfflineIndicator(networkState = networkState)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRecurringTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = "Add new recurring transaction"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Success -> {
                    if (state.data.isEmpty) {
                        EmptyRecurringTransactionsView(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        RecurringTransactionList(
                            recurringTransactions = state.data.recurringTransactions,
                            categoriesMap = state.data.categoriesMap,
                            onRecurringTransactionClick = onEditRecurringTransaction,
                            onDeleteRecurringTransaction = { viewModel.deleteRecurringTransaction(it) },
                            onDeactivateRecurringTransaction = { viewModel.deactivateRecurringTransaction(it) },
                            userFeedbackManager = userFeedbackManager
                        )
                    }
                }
                is UiState.Error -> {
                    ErrorDisplay(
                        error = state,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringTransactionList(
    recurringTransactions: List<RecurringTransaction>,
    categoriesMap: Map<String, Category>,
    onRecurringTransactionClick: (String) -> Unit,
    onDeleteRecurringTransaction: (String) -> Unit,
    onDeactivateRecurringTransaction: (String) -> Unit,
    userFeedbackManager: UserFeedbackManager
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = recurringTransactions,
            key = { it.id }
        ) { recurringTransaction ->
            RecurringTransactionItem(
                recurringTransaction = recurringTransaction,
                category = categoriesMap[recurringTransaction.templateTransaction.categoryId],
                onClick = { onRecurringTransactionClick(recurringTransaction.id) },
                onDelete = { 
                    onDeleteRecurringTransaction(recurringTransaction.id)
                    userFeedbackManager.showSuccess("Recurring transaction deleted")
                },
                onDeactivate = {
                    onDeactivateRecurringTransaction(recurringTransaction.id)
                    userFeedbackManager.showSuccess("Recurring transaction deactivated")
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringTransactionItem(
    recurringTransaction: RecurringTransaction,
    category: Category?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDeactivate: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeactivateDialog by remember { mutableStateOf(false) }
    
    val formattedNextDueDate = remember(recurringTransaction.nextDueDate) { 
        formatDate(recurringTransaction.nextDueDate) 
    }
    val formattedAmount = remember(recurringTransaction.templateTransaction.amount, recurringTransaction.templateTransaction.type) { 
        CurrencyUtils.formatAmountWithType(
            recurringTransaction.templateTransaction.amount, 
            recurringTransaction.templateTransaction.type
        ) 
    }
    val amountColor = remember(recurringTransaction.templateTransaction.type) {
        if (recurringTransaction.templateTransaction.type == TransactionType.INCOME) {
            Color(0xFF4CAF50)
        } else {
            Color(0xFFF44336)
        }
    }
    
    val cachedIcon = rememberCachedIcon(
        iconName = category?.iconName ?: "?",
        color = category?.color ?: "#FF6B6B",
        size = 48.dp
    )
    
    val recurrenceText = remember(recurringTransaction.recurrencePattern) {
        when (recurringTransaction.recurrencePattern) {
            RecurrencePattern.DAILY -> "Daily"
            RecurrencePattern.WEEKLY -> "Weekly"
            RecurrencePattern.MONTHLY -> "Monthly"
        }
    }
    
    val statusText = if (recurringTransaction.isActive) "Active" else "Inactive"
    val statusColor = if (recurringTransaction.isActive) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFF9E9E9E)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Edit recurring transaction",
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = "Recurring transaction: ${category?.name ?: "Unknown"}, $formattedAmount, $recurrenceText, Next due: $formattedNextDueDate, Status: $statusText"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(cachedIcon.colorInt))
                        .semantics {
                            contentDescription = "Category: ${category?.name ?: "Unknown"}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cachedIcon.displayText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category?.name ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurring",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "$recurrenceText • Next: $formattedNextDueDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (recurringTransaction.isActive) {
                        IconButton(
                            onClick = { showDeactivateDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .semantics {
                                    contentDescription = "Deactivate recurring transaction"
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .semantics {
                                contentDescription = "Delete recurring transaction"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recurring Transaction") },
            text = { Text("Are you sure you want to delete this recurring transaction? This will stop all future transactions from being created.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text("Deactivate Recurring Transaction") },
            text = { Text("Are you sure you want to deactivate this recurring transaction? You can reactivate it later by editing it.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeactivate()
                        showDeactivateDialog = false
                    }
                ) {
                    Text("Deactivate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyRecurringTransactionsView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Repeat,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No recurring transactions yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tap the + button to add your first recurring transaction",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
