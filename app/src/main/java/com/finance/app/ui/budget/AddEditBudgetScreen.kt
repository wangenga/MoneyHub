package com.finance.app.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.app.domain.model.Budget
import com.finance.app.domain.model.Category
import com.finance.app.ui.common.AsyncState
import com.finance.app.ui.common.UiState
import com.finance.app.ui.components.FinanceOutlinedTextField
import com.finance.app.util.CurrencyUtils
import com.finance.app.util.rememberCachedIcon

/**
 * Screen for adding or editing a budget for a specific category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
    categoryId: String,
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val budget by viewModel.getBudgetForCategory(categoryId).collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    
    var budgetAmount by remember { mutableStateOf("") }
    var budgetAmountError by remember { mutableStateOf<String?>(null) }
    
    val category = remember(categories, categoryId) {
        (categories as? UiState.Success)?.data?.find { it.id == categoryId }
    }
    
    // Initialize budget amount from existing budget
    LaunchedEffect(budget) {
        budget?.let {
            budgetAmount = it.monthlyLimit.toString()
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(saveState) {
        when (saveState) {
            is AsyncState.Success -> {
                snackbarHostState.showSnackbar("Budget saved successfully")
                onNavigateBack()
            }
            is AsyncState.Error -> {
                val errorState = saveState as AsyncState.Error
                snackbarHostState.showSnackbar(errorState.message)
                viewModel.clearSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (budget != null) "Edit Budget" else "Add Budget") 
                },
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
        when {
            categories is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            category == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Category not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                BudgetForm(
                    category = category,
                    budgetAmount = budgetAmount,
                    budgetAmountError = budgetAmountError,
                    onBudgetAmountChange = { 
                        budgetAmount = it
                        budgetAmountError = null
                    },
                    onSave = {
                        val amount = budgetAmount.toDoubleOrNull()
                        when {
                            amount == null -> {
                                budgetAmountError = "Please enter a valid amount"
                            }
                            !Budget.isValidBudgetAmount(amount) -> {
                                budgetAmountError = "Budget must be between 0 and 1,000,000"
                            }
                            else -> {
                                viewModel.saveBudget(categoryId, amount)
                            }
                        }
                    },
                    isSaving = saveState is AsyncState.Loading,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun BudgetForm(
    category: Category,
    budgetAmount: String,
    budgetAmountError: String?,
    onBudgetAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    val cachedIcon = rememberCachedIcon(
        iconName = category.iconName,
        color = category.color,
        size = 64.dp
    )
    
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Category display
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(cachedIcon.colorInt))
                        .semantics {
                            contentDescription = "Category icon for ${category.name}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cachedIcon.displayText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Set monthly spending limit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Budget amount input
        FinanceOutlinedTextField(
            value = budgetAmount,
            onValueChange = onBudgetAmountChange,
            label = "Monthly Budget Limit",
            placeholder = "0.00",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null
                )
            },
            supportingText = budgetAmountError ?: "Enter the maximum amount you want to spend in this category per month",
            isError = budgetAmountError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isRequired = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Budget Alerts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "You'll receive alerts when you reach 75%, 90%, and 100% of your budget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Save button
        Button(
            onClick = onSave,
            enabled = !isSaving && budgetAmount.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Budget")
            }
        }
    }
}
