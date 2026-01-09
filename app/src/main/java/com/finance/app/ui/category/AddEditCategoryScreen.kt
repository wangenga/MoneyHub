package com.finance.app.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.app.domain.model.CategoryType

/**
 * Screen for adding or editing a category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryScreen(
    onNavigateBack: () -> Unit,
    onCategoryCreated: ((String) -> Unit)? = null,
    viewModel: AddEditCategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    
    var showColorPicker by remember { mutableStateOf(false) }
    var showIconSelector by remember { mutableStateOf(false) }

    // Handle save success - only navigate back if no callback is provided
    // When callback is provided, it handles navigation
    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success && onCategoryCreated == null) {
            onNavigateBack()
        }
    }
    
    // Handle category created event (for returning result to transaction screen)
    LaunchedEffect(Unit) {
        viewModel.categoryCreatedEvent.collect { categoryId ->
            onCategoryCreated?.invoke(categoryId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Category" else "Add Category") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveCategory() },
                        enabled = saveState !is SaveState.Saving && !uiState.isDefault
                    ) {
                        if (saveState is SaveState.Saving) {
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Show default category warning if editing a default category
            if (uiState.isDefault) {
                DefaultCategoryWarning()
            }
            
            // Preview Card
            CategoryPreviewCard(
                name = uiState.name.ifBlank { "Category Name" },
                color = uiState.color,
                iconName = uiState.iconName,
                categoryType = uiState.categoryType
            )

            // Category Type Selector (only for new categories)
            if (!uiState.isEditMode) {
                CategoryTypeSelector(
                    selectedType = uiState.categoryType,
                    onTypeSelected = { viewModel.updateCategoryType(it) }
                )
            }

            // Name Field
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Category Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null
                    )
                },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                placeholder = { Text("e.g., Groceries, Shopping") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isDefault
            )

            // Color Selector
            ColorSelector(
                selectedColor = uiState.color,
                onClick = { if (!uiState.isDefault) showColorPicker = true },
                enabled = !uiState.isDefault
            )

            // Icon Selector
            IconSelector(
                selectedIcon = uiState.iconName,
                color = uiState.color,
                onClick = { if (!uiState.isDefault) showIconSelector = true },
                enabled = !uiState.isDefault
            )

            // Error message
            if (saveState is SaveState.Error) {
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
                            text = (saveState as SaveState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // Color Picker Dialog
    if (showColorPicker) {
        ColorPickerDialog(
            selectedColor = uiState.color,
            onColorSelected = { color ->
                viewModel.updateColor(color)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    // Icon Selector Dialog
    if (showIconSelector) {
        IconSelectorDialog(
            selectedIcon = uiState.iconName,
            color = uiState.color,
            onIconSelected = { icon ->
                viewModel.updateIcon(icon)
                showIconSelector = false
            },
            onDismiss = { showIconSelector = false }
        )
    }
}

@Composable
private fun DefaultCategoryWarning() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "This is a default category and cannot be modified. Default categories are provided by the system for all users.",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CategoryTypeSelector(
    selectedType: CategoryType,
    onTypeSelected: (CategoryType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Category Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryTypeChip(
                categoryType = CategoryType.EXPENSE,
                isSelected = selectedType == CategoryType.EXPENSE,
                onClick = { onTypeSelected(CategoryType.EXPENSE) },
                modifier = Modifier.weight(1f)
            )
            CategoryTypeChip(
                categoryType = CategoryType.INCOME,
                isSelected = selectedType == CategoryType.INCOME,
                onClick = { onTypeSelected(CategoryType.INCOME) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CategoryTypeChip(
    categoryType: CategoryType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (text, icon) = when (categoryType) {
        CategoryType.EXPENSE -> "Expense" to Icons.Default.TrendingDown
        CategoryType.INCOME -> "Income" to Icons.Default.TrendingUp
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier
    )
}

@Composable
private fun CategoryPreviewCard(
    name: String,
    color: String,
    iconName: String,
    categoryType: CategoryType
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(parseColor(color)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            // Category type indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (categoryType) {
                        CategoryType.EXPENSE -> Icons.Default.TrendingDown
                        CategoryType.INCOME -> Icons.Default.TrendingUp
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = categoryType.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ColorSelector(
    selectedColor: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val colorName = namedColors.find { it.hex == selectedColor }?.name ?: "Custom"
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Color",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = CardDefaults.outlinedCardBorder()
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseColor(selectedColor))
                    )
                    Text(
                        text = colorName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Select color",
                    tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IconSelector(
    selectedIcon: String,
    color: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Letter",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = CardDefaults.outlinedCardBorder()
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseColor(color)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedIcon,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Letter: $selectedIcon",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Select letter",
                    tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Data class for named colors
 */
private data class NamedColor(val name: String, val hex: String)

/**
 * List of named colors without duplicates
 */
private val namedColors = listOf(
    NamedColor("Coral Red", "#FF6B6B"),
    NamedColor("Turquoise", "#4ECDC4"),
    NamedColor("Sky Blue", "#45B7D1"),
    NamedColor("Light Salmon", "#FFA07A"),
    NamedColor("Mint Green", "#98D8C8"),
    NamedColor("Pink", "#F06292"),
    NamedColor("Purple", "#BA68C8"),
    NamedColor("Deep Purple", "#9575CD"),
    NamedColor("Indigo", "#7986CB"),
    NamedColor("Blue", "#64B5F6"),
    NamedColor("Light Blue", "#4FC3F7"),
    NamedColor("Cyan", "#4DD0E1"),
    NamedColor("Teal", "#4DB6AC"),
    NamedColor("Green", "#81C784"),
    NamedColor("Light Green", "#AED581"),
    NamedColor("Lime", "#DCE775"),
    NamedColor("Amber", "#FFD54F"),
    NamedColor("Orange", "#FFB74D"),
    NamedColor("Deep Orange", "#FF8A65"),
    NamedColor("Brown", "#A1887F")
)

@Composable
private fun ColorPickerDialog(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(namedColors) { namedColor ->
                    ColorItem(
                        color = namedColor.hex,
                        colorName = namedColor.name,
                        isSelected = namedColor.hex == selectedColor,
                        onClick = { onColorSelected(namedColor.hex) }
                    )
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
private fun ColorItem(
    color: String,
    colorName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(parseColor(color))
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = colorName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun IconSelectorDialog(
    selectedIcon: String,
    color: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Simple letters A-Z for category icons
    val icons = ('A'..'Z').map { it.toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Letter") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(icons) { icon ->
                    IconItem(
                        iconName = icon,
                        color = color,
                        isSelected = icon == selectedIcon,
                        onClick = { onIconSelected(icon) }
                    )
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
private fun IconItem(
    iconName: String,
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(parseColor(color))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iconName,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
