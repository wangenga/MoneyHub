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
import com.finance.app.domain.model.TransactionType
import com.finance.app.ui.common.AsyncState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen for adding or editing a transaction
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddCategory: () -> Unit = {},
    newCategoryId: String? = null,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySelector by remember { mutableStateOf(false) }

    // Handle save success
    LaunchedEffect(saveState) {
        if (saveState is AsyncState.Success) {
            onNavigateBack()
        }
    }
    
    // Auto-select newly created category when returning from AddEditCategoryScreen
    LaunchedEffect(newCategoryId) {
        if (newCategoryId != null && newCategoryId.isNotEmpty()) {
            viewModel.updateCategory(newCategoryId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Transaction" else "Add Transaction") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back, cancel ${if (uiState.isEditMode) "editing" else "adding"} transaction"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null // IconButton already has content description
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveTransaction() },
                        enabled = saveState !is AsyncState.Loading,
                        modifier = Modifier.semantics {
                            contentDescription = if (saveState is AsyncState.Loading) {
                                "Saving transaction, please wait"
                            } else {
                                "Save transaction"
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

            // Date Picker
            OutlinedTextField(
                value = formatDate(uiState.date),
                onValueChange = {},
                label = { Text("Date") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null
                    )
                },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

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
            initialDate = uiState.date,
            onDateSelected = { date ->
                viewModel.updateDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Category Selector Dialog
    if (showCategorySelector) {
        CategorySelectorDialog(
            categories = categories,
            selectedCategoryId = uiState.categoryId,
            onCategorySelected = { categoryId ->
                viewModel.updateCategory(categoryId)
                showCategorySelector = false
            },
            onAddNewCategory = {
                showCategorySelector = false
                onNavigateToAddCategory()
            },
            onDismiss = { showCategorySelector = false }
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
                            text = selectedCategory.iconName.firstOrNull()?.toString() ?: "?",
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
