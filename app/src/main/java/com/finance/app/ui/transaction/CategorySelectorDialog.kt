package com.finance.app.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType

/**
 * Dialog for selecting a category with icons, filtered by transaction type
 */
@Composable
fun CategorySelectorDialog(
    categories: List<Category>,
    selectedCategoryId: String,
    transactionType: CategoryType,
    onCategorySelected: (String) -> Unit,
    onAddNewCategory: (CategoryType) -> Unit,
    onDismiss: () -> Unit
) {
    val dialogTitle = when (transactionType) {
        CategoryType.EXPENSE -> "Select Expense Category"
        CategoryType.INCOME -> "Select Income Category"
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            if (categories.isEmpty()) {
                EmptyStateContent(
                    transactionType = transactionType,
                    onAddNewCategory = { onAddNewCategory(transactionType) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show existing categories
                    items(categories) { category ->
                        CategoryItem(
                            category = category,
                            isSelected = category.id == selectedCategoryId,
                            onClick = { onCategorySelected(category.id) }
                        )
                    }
                    
                    // Divider before add option
                    item {
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    
                    // Add New Category option
                    item {
                        AddNewCategoryItem(
                            transactionType = transactionType,
                            onClick = { onAddNewCategory(transactionType) }
                        )
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

@Composable
private fun EmptyStateContent(
    transactionType: CategoryType,
    onAddNewCategory: () -> Unit
) {
    val (title, description, buttonText) = when (transactionType) {
        CategoryType.EXPENSE -> Triple(
            "No expense categories available",
            "You need at least one expense category to create expense transactions.",
            "Add Expense Category"
        )
        CategoryType.INCOME -> Triple(
            "No income categories yet",
            "Create your first income category to track income sources.",
            "Add Income Category"
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onAddNewCategory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText)
        }
    }
}

/**
 * Item for adding a new category, styled differently from regular categories
 */
@Composable
private fun AddNewCategoryItem(
    transactionType: CategoryType,
    onClick: () -> Unit
) {
    val buttonText = when (transactionType) {
        CategoryType.EXPENSE -> "Add New Expense Category"
        CategoryType.INCOME -> "Add New Income Category"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buttonText
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add icon with primary color background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Add new category text
            Text(
                text = buttonText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon with color
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(parseColor(category.color)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.iconName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Category name
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            // Selected indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
