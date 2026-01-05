package com.finance.app.ui.category

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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.app.domain.model.Category
import com.finance.app.ui.common.AsyncState
import com.finance.app.ui.common.UiState

/**
 * Category management screen to display and manage all categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    onAddCategory: () -> Unit,
    onEditCategory: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Show snackbar for delete success
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is AsyncState.Success -> {
                snackbarHostState.showSnackbar("Category deleted successfully")
            }
            is AsyncState.Error -> {
                val errorState = deleteState as AsyncState.Error
                snackbarHostState.showSnackbar(errorState.message)
                viewModel.clearDeleteError()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCategory,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = "Add new category"
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
                    if (state.data.isEmpty()) {
                        EmptyCategoriesView(
                            modifier = Modifier.align(Alignment.Center),
                            onAddCategory = onAddCategory
                        )
                    } else {
                        CategoryList(
                            categories = state.data,
                            onCategoryClick = onEditCategory,
                            onDeleteCategory = { category ->
                                categoryToDelete = category
                                showDeleteDialog = true
                            }
                        )
                    }
                }
                is UiState.Error -> {
                    ErrorView(
                        message = state.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && categoryToDelete != null) {
        DeleteCategoryDialog(
            category = categoryToDelete!!,
            onConfirm = {
                viewModel.deleteCategory(categoryToDelete!!.id)
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
private fun CategoryList(
    categories: List<Category>,
    onCategoryClick: (String) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = categories,
            key = { it.id }
        ) { category ->
            CategoryItem(
                category = category,
                onClick = { onCategoryClick(category.id) },
                onDelete = { onDeleteCategory(category) }
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryDescription = "Category: ${category.name}, color ${category.color}${if (category.isDefault) ", default category" else ""}. Double tap to edit."
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Edit category ${category.name}",
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = categoryDescription
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
                // Category icon with color and minimum touch target
                Box(
                    modifier = Modifier
                        .size(48.dp) // Already minimum 48dp touch target
                        .clip(CircleShape)
                        .background(parseColor(category.color))
                        .semantics {
                            contentDescription = "Category icon for ${category.name}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.iconName.firstOrNull()?.toString() ?: "?",
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
                    if (category.isDefault) {
                        Text(
                            text = "Default category",
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
                // Edit icon with minimum touch target
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(48.dp) // Minimum 48dp touch target
                        .semantics {
                            contentDescription = "Edit category ${category.name}"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null, // IconButton already has content description
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Delete icon with minimum touch target
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp) // Minimum 48dp touch target
                        .semantics {
                            contentDescription = if (category.isDefault) {
                                "Cannot delete default category ${category.name}"
                            } else {
                                "Delete category ${category.name}"
                            }
                        },
                    enabled = !category.isDefault
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null, // IconButton already has content description
                        tint = if (category.isDefault) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCategoriesView(
    modifier: Modifier = Modifier,
    onAddCategory: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Category,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No categories yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tap the + button to create your first category",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onAddCategory) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Category")
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

@Composable
private fun DeleteCategoryDialog(
    category: Category,
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
        title = { Text("Delete Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Are you sure you want to delete \"${category.name}\"?")
                if (!category.isDefault) {
                    Text(
                        text = "Warning: This action cannot be undone. If this category has existing transactions, deletion will fail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
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

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
