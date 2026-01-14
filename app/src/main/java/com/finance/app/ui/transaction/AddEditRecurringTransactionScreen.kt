package com.finance.app.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType
import com.finance.app.domain.model.RecurrencePattern
import com.finance.app.domain.model.TransactionType
import com.finance.app.ui.common.AsyncState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen for adding or editing a recurring transaction
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringTransactionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddCategory: () -> Unit = {},
    newCategoryId: String? = null,
    viewModel: AddEditRecurringTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySelector by remember { mutableStateOf(false) }
    var showRecurrencePatternDialog by remember { mutableStateOf(false) }

    // Handle save success
    LaunchedEffect(saveState) {
        if (saveState is AsyncState.Success) {
            onNavigateBack()
        }
    }
    
    // Auto-select newly created category
    LaunchedEffect(newCategoryId) {
        if (newCategoryId != null && newCategoryId.isNotEmpty()) {
            viewModel.updateCategory(newCategoryId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Recurring Transaction" else "Add Recurring Transaction") },
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
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveRecurringTransaction() },
                        enabled = saveState !is AsyncState.Loading,
                        modifier = Modifier.semantics {
                            contentDescription = if (saveState is AsyncState.Loading) {
                                "Saving recurring transaction"
                            } else {
                                "Save recurring transaction"
                            }
                        }
                    ) {
                        if (saveState is AsyncState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type Toggle
            TransactionTypeToggle(
                selectedType = uiState.type,
                onTypeSelected = { viewModel.updateType(it) }
            )

            // Amount Field
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Amount (KSh)") },
                leadingIcon = {
                    Text(
                        text = "KSh",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.amountError != null,
                supportingText = uiState.amountError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category Selector
            CategorySelector(
                selectedCategoryId = uiState.categoryId,
                categories = categories,
                isError = uiState.categoryError != null,
                errorMessage = uiState.categoryError,
                onClick = { showCategorySelector = true }
            )

            // Recurrence Pattern Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRecurrencePatternDialog = true }
            ) {
                OutlinedTextField(
                    value = formatRecurrencePattern(uiState.recurrencePattern),
                    onValueChange = {},
                    label = { Text("Recurrence Pattern") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select recurrence pattern"
                        )
                    },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Start Date Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = formatDate(uiState.startDate),
                    onValueChange = {},
                    label = { Text("Start Date") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Select start date"
                        )
                    },
                    readOnly = true,
                    enabled = false,
                    isError = uiState.startDateError != null,
                    supportingText = uiState.startDateError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = if (uiState.startDateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        disabledLabelColor = if (uiState.startDateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Payment Method
            OutlinedTextField(
                value = uiState.paymentMethod,
                onValueChange = { viewModel.updatePaymentMethod(it) },
                label = { Text("Payment Method (Optional)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null
                    )
                },
                placeholder = { Text("e.g., Cash, Credit Card") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Notes (Optional)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null
                    )
                },
                placeholder = { Text("Add any additional details") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Active Status Toggle
            if (uiState.isEditMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "When inactive, no new transactions will be created",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isActive,
                        onCheckedChange = { viewModel.updateIsActive(it) }
                    )
                }
            }

            // Error message
            if (saveState is AsyncState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = (saveState as AsyncState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = uiState.startDate,
            onDateSelected = { date ->
                viewModel.updateStartDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Category Selector Dialog
    if (showCategorySelector) {
        val transactionType = when (uiState.type) {
            TransactionType.EXPENSE -> CategoryType.EXPENSE
            TransactionType.INCOME -> CategoryType.INCOME
        }
        
        // Filter categories by transaction type
        val filteredCategories = categories.filter { it.categoryType == transactionType }
        
        CategorySelectorDialog(
            categories = filteredCategories,
            selectedCategoryId = uiState.categoryId,
            transactionType = transactionType,
            onCategorySelected = { categoryId ->
                viewModel.updateCategory(categoryId)
                showCategorySelector = false
            },
            onAddNewCategory = { categoryType ->
                showCategorySelector = false
                onNavigateToAddCategory()
            },
            onDismiss = { showCategorySelector = false }
        )
    }

    // Recurrence Pattern Dialog
    if (showRecurrencePatternDialog) {
        RecurrencePatternDialog(
            selectedPattern = uiState.recurrencePattern,
            onPatternSelected = { pattern ->
                viewModel.updateRecurrencePattern(pattern)
                showRecurrencePatternDialog = false
            },
            onDismiss = { showRecurrencePatternDialog = false }
        )
    }
}

@Composable
private fun TransactionTypeToggle(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TransactionTypeButton(
                text = "Expense",
                icon = Icons.Default.TrendingDown,
                isSelected = selectedType == TransactionType.EXPENSE,
                color = Color(0xFFF44336),
                onClick = { onTypeSelected(TransactionType.EXPENSE) },
                modifier = Modifier.weight(1f)
            )
            TransactionTypeButton(
                text = "Income",
                icon = Icons.Default.TrendingUp,
                isSelected = selectedType == TransactionType.INCOME,
                color = Color(0xFF4CAF50),
                onClick = { onTypeSelected(TransactionType.INCOME) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TransactionTypeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun CategorySelector(
    selectedCategoryId: String,
    categories: List<Category>,
    isError: Boolean,
    errorMessage: String?,
    onClick: () -> Unit
) {
    val selectedCategory = categories.find { it.id == selectedCategoryId }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "",
            onValueChange = {},
            label = { Text("Category") },
            leadingIcon = {
                if (selectedCategory != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parseColor(selectedCategory.color)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedCategory.iconName,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null
                    )
                }
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            readOnly = true,
            enabled = false,
            isError = isError,
            supportingText = errorMessage?.let { { Text(it) } },
            placeholder = { Text("Select a category") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                disabledLabelColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun RecurrencePatternDialog(
    selectedPattern: RecurrencePattern,
    onPatternSelected: (RecurrencePattern) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Recurrence Pattern") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecurrencePattern.values().forEach { pattern ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPatternSelected(pattern) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatRecurrencePattern(pattern),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (pattern == selectedPattern) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatRecurrencePattern(pattern: RecurrencePattern): String {
    return when (pattern) {
        RecurrencePattern.DAILY -> "Daily"
        RecurrencePattern.WEEKLY -> "Weekly"
        RecurrencePattern.MONTHLY -> "Monthly"
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
