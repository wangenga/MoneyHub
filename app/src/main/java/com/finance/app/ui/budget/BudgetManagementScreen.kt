package com.finance.app.ui.budget

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.app.domain.model.Budget
import com.finance.app.domain.model.BudgetUtilization
import com.finance.app.domain.model.Category
import com.finance.app.ui.common.UiState
import com.finance.app.util.CurrencyUtils
import com.finance.app.util.rememberCachedIcon

/**
 * Budget management screen for setting and viewing category budgets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManagementScreen(
    onNavigateBack: () -> Unit,
    onEditBudget: (String) -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val budgetUtilization by viewModel.budgetUtilization.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    
    var categoryToDelete by remember { mutableStateOf<Pair<Category, Budget>?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is com.finance.app.ui.common.AsyncState.Success -> {
                snackbarHostState.showSnackbar("Budget deleted successfully")
            }
            is com.finance.app.ui.common.AsyncState.Error -> {
                val errorState = deleteState as com.finance.app.ui.common.AsyncState.Error
                snackbarHostState.showSnackbar(errorState.message)
                viewModel.clearDeleteState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                budgets is UiState.Loading || categories is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                budgets is UiState.Error -> {
                    ErrorView(
                        message = (budgets as UiState.Error).message,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                categories is UiState.Error -> {
                    ErrorView(
                        message = (categories as UiState.Error).message,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                budgets is UiState.Success && categories is UiState.Success -> {
                    val budgetList = (budgets as UiState.Success).data
                    val categoryList = (categories as UiState.Success).data
                    val utilizationList = if (budgetUtilization is UiState.Success) {
                        (budgetUtilization as UiState.Success).data
                    } else {
                        emptyList()
                    }
                    
                    if (categoryList.isEmpty()) {
                        EmptyStateView(
                            message = "No categories available. Create categories first to set budgets.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        BudgetList(
                            budgets = budgetList,
                            categories = categoryList,
                            budgetUtilization = utilizationList,
                            onEditBudget = onEditBudget,
                            onDeleteBudget = { category, budget ->
                                categoryToDelete = Pair(category, budget)
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && categoryToDelete != null) {
        DeleteBudgetDialog(
            category = categoryToDelete!!.first,
            budget = categoryToDelete!!.second,
            onConfirm = {
                viewModel.deleteBudget(categoryToDelete!!.first.id)
                showDeleteDialog = false
                categoryToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                categoryToDelete = null
            }
        )
    }
}

@Composable
private fun BudgetList(
    budgets: List<Budget>,
    categories: List<Category>,
    budgetUtilization: List<BudgetUtilization>,
    onEditBudget: (String) -> Unit,
    onDeleteBudget: (Category, Budget) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Text(
                text = "Set monthly spending limits for your categories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Categories with budgets
        items(
            items = categories,
            key = { it.id }
        ) { category ->
            val budget = budgets.find { it.categoryId == category.id }
            val utilization = budgetUtilization.find { it.categoryId == category.id }
            BudgetCategoryItem(
                category = category,
                budget = budget,
                utilization = utilization,
                onEditBudget = { onEditBudget(category.id) },
                onDeleteBudget = { budget?.let { onDeleteBudget(category, it) } }
            )
        }
    }
}

@Composable
private fun BudgetCategoryItem(
    category: Category,
    budget: Budget?,
    utilization: BudgetUtilization?,
    onEditBudget: () -> Unit,
    onDeleteBudget: () -> Unit
) {
    val cachedIcon = rememberCachedIcon(
        iconName = category.iconName,
        color = category.color,
        size = 48.dp
    )
    
    val spent = utilization?.currentSpending ?: 0.0
    val remaining = if (budget != null) budget.monthlyLimit - spent else 0.0
    val percentageUsed = if (budget != null && budget.monthlyLimit > 0) {
        (spent / budget.monthlyLimit * 100).coerceIn(0.0, 100.0)
    } else {
        0.0
    }
    
    val categoryDescription = if (budget != null) {
        "Category: ${category.name}, Budget: ${CurrencyUtils.formatAmount(budget.monthlyLimit)}, Spent: ${CurrencyUtils.formatAmount(spent)}, Remaining: ${CurrencyUtils.formatAmount(remaining)}. Tap to edit."
    } else {
        "Category: ${category.name}, No budget set. Tap to add budget."
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditBudget)
            .semantics {
                contentDescription = categoryDescription
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Category icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(cachedIcon.colorInt))
                            .semantics {
                                contentDescription = "Category icon for ${category.name}"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cachedIcon.displayText,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (budget != null) {
                            Text(
                                text = "Budget: ${CurrencyUtils.formatAmount(budget.monthlyLimit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "No budget set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit/Add icon
                    IconButton(
                        onClick = onEditBudget,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = if (budget != null) {
                                    "Edit budget for ${category.name}"
                                } else {
                                    "Add budget for ${category.name}"
                                }
                            }
                    ) {
                        Icon(
                            imageVector = if (budget != null) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Delete icon (only show if budget exists)
                    if (budget != null) {
                        IconButton(
                            onClick = onDeleteBudget,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = "Delete budget for ${category.name}"
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
            
            // Budget progress and remaining amount (only show if budget exists)
            if (budget != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Spending info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Spent: ${CurrencyUtils.formatAmount(spent)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Remaining: ${CurrencyUtils.formatAmount(remaining)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (remaining >= 0) {
                                Color(0xFF4CAF50)
                            } else {
                                Color(0xFFF44336)
                            }
                        )
                    }
                    
                    // Progress bar
                    LinearProgressIndicator(
                        progress = (percentageUsed / 100).toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = when {
                            percentageUsed >= 100 -> Color(0xFFF44336) // Red when over budget
                            percentageUsed >= 80 -> Color(0xFFFF9800) // Orange when close to limit
                            else -> Color(0xFF4CAF50) // Green when safe
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    
                    // Percentage text
                    Text(
                        text = "${percentageUsed.toInt()}% used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No Categories",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
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
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun DeleteBudgetDialog(
    category: Category,
    budget: Budget,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Budget") },
        text = {
            Text("Are you sure you want to delete the budget for \"${category.name}\"? This will remove the ${CurrencyUtils.formatAmount(budget.monthlyLimit)} monthly limit.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
