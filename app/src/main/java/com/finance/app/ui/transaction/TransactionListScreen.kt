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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType
import com.finance.app.ui.accessibility.createTransactionContentDescription
import com.finance.app.ui.accessibility.AccessibilityAnnouncement
import com.finance.app.ui.common.UiState
import com.finance.app.ui.common.UserFeedbackManager
import com.finance.app.ui.components.ErrorDisplay
import com.finance.app.ui.components.OfflineIndicator
import com.finance.app.util.CurrencyUtils
import com.finance.app.util.IconCache
import com.finance.app.util.rememberCachedIcon
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transaction list screen with enhanced error handling and offline support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val networkState by viewModel.networkState.collectAsState()
    val globalError by viewModel.globalError.collectAsState()
    
    var showFilterDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val userFeedbackManager = remember(snackbarHostState, scope) {
        UserFeedbackManager(snackbarHostState, scope)
    }

    // Optimize filter state checks with derivedStateOf
    val hasActiveFilters by remember {
        derivedStateOf {
            filterState.hasActiveFilters()
        }
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
                    title = { Text("Transactions") },
                    actions = {
                        IconButton(
                            onClick = { showFilterDialog = true },
                            modifier = Modifier.semantics {
                                contentDescription = if (hasActiveFilters) {
                                    "Filter transactions, filters currently active"
                                } else {
                                    "Filter transactions"
                                }
                            }
                        ) {
                            Badge(
                                containerColor = if (hasActiveFilters) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null // IconButton already has content description
                                )
                            }
                        }
                    }
                )
                
                // Offline indicator
                OfflineIndicator(networkState = networkState)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = "Add new transaction"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null // FAB already has content description
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
                        EmptyTransactionsView(
                            modifier = Modifier.align(Alignment.Center),
                            hasFilters = hasActiveFilters,
                            onClearFilters = { viewModel.clearFilters() }
                        )
                    } else {
                        TransactionList(
                            transactions = state.data.transactions,
                            categoriesMap = state.data.categoriesMap,
                            onTransactionClick = onEditTransaction,
                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                            userFeedbackManager = userFeedbackManager,
                            hasMoreData = state.data.hasMoreData,
                            onLoadMore = { viewModel.loadMoreTransactions() }
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

    if (showFilterDialog) {
        FilterDialog(
            filterState = filterState,
            categories = categories,
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { filter ->
                viewModel.updateFilter(filter)
                showFilterDialog = false
            },
            onClearFilters = {
                viewModel.clearFilters()
                showFilterDialog = false
            }
        )
    }
}

@Composable
private fun TransactionList(
    transactions: List<Transaction>,
    categoriesMap: Map<String, Category>,
    onTransactionClick: (String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    userFeedbackManager: UserFeedbackManager,
    hasMoreData: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = transactions,
            key = { it.id }
        ) { transaction ->
            TransactionItem(
                transaction = transaction,
                category = categoriesMap[transaction.categoryId],
                onClick = { onTransactionClick(transaction.id) },
                onDelete = { 
                    onDeleteTransaction(transaction.id)
                    userFeedbackManager.showSuccess("Transaction deleted")
                }
            )
        }
        
        // Load more item at the end
        if (hasMoreData) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onLoadMore,
                        modifier = Modifier.semantics {
                            contentDescription = "Load more transactions"
                        }
                    ) {
                        Text("Load More")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Optimize expensive calculations with remember
    val formattedDate = remember(transaction.date) { formatDate(transaction.date) }
    val formattedAmount = remember(transaction.amount, transaction.type) { 
        CurrencyUtils.formatAmountWithType(transaction.amount, transaction.type) 
    }
    val amountColor = remember(transaction.type) {
        if (transaction.type == TransactionType.INCOME) {
            Color(0xFF4CAF50)
        } else {
            Color(0xFFF44336)
        }
    }
    
    // Use cached icon for better performance
    val cachedIcon = rememberCachedIcon(
        iconName = category?.iconName ?: "?",
        color = category?.color ?: "#FF6B6B",
        size = 48.dp
    )
    
    // Create comprehensive content description for the transaction item
    val transactionDescription = remember(category?.name, formattedAmount, formattedDate, transaction.notes) {
        createTransactionContentDescription(
            category = category?.name ?: "Unknown category",
            amount = formattedAmount,
            date = formattedDate,
            notes = transaction.notes,
            actionHint = "Double tap to edit"
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Edit transaction",
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = transactionDescription
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
                // Category icon with minimum touch target
                Box(
                    modifier = Modifier
                        .size(48.dp) // Minimum 48dp touch target
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
                    Text(
                        text = category?.name ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!transaction.notes.isNullOrBlank()) {
                        Text(
                            text = transaction.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
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
                    color = amountColor,
                    modifier = Modifier.semantics {
                        contentDescription = "${if (transaction.type == TransactionType.INCOME) "Income" else "Expense"}: ${CurrencyUtils.formatAmount(transaction.amount)}"
                    }
                )
                
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .size(48.dp) // Minimum 48dp touch target
                        .semantics {
                            contentDescription = "Delete transaction: ${category?.name ?: "Unknown"}, $formattedAmount"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null, // IconButton already has content description
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { 
                Text(
                    text = "Delete Transaction",
                    modifier = Modifier.semantics {
                        contentDescription = "Delete transaction confirmation dialog"
                    }
                ) 
            },
            text = { 
                Text(
                    text = "Are you sure you want to delete this transaction?",
                    modifier = Modifier.semantics {
                        contentDescription = "Confirmation message: Are you sure you want to delete this transaction?"
                    }
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Confirm delete transaction"
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.semantics {
                        contentDescription = "Cancel delete transaction"
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Announce deletion to accessibility services
    if (showDeleteDialog) {
        AccessibilityAnnouncement(
            message = "Delete confirmation dialog opened for transaction",
            shouldAnnounce = true
        )
    }
}

@Composable
private fun EmptyTransactionsView(
    modifier: Modifier = Modifier,
    hasFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (hasFilters) "No transactions match your filters" else "No transactions yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (hasFilters) "Try adjusting your filters" else "Tap the + button to add your first transaction",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasFilters) {
            Button(onClick = onClearFilters) {
                Text("Clear Filters")
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper functions
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}

private fun FilterState.hasActiveFilters(): Boolean {
    return startDate != null || endDate != null || type != null || categoryId != null || !searchQuery.isNullOrBlank()
}
