package com.finance.app.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.model.Category
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for add/edit category screen
 */
@HiltViewModel
class AddEditCategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: String? = savedStateHandle.get<String>("categoryId")

    private val _uiState = MutableStateFlow(AddEditCategoryUiState())
    val uiState: StateFlow<AddEditCategoryUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()
    
    // Event for when a new category is created (emits the category ID)
    private val _categoryCreatedEvent = MutableSharedFlow<String>()
    val categoryCreatedEvent: SharedFlow<String> = _categoryCreatedEvent.asSharedFlow()

    init {
        if (categoryId != null) {
            loadCategory(categoryId)
        }
    }

    private fun loadCategory(id: String) {
        viewModelScope.launch {
            categoryRepository.getCategoryById(id)
                .firstOrNull()
                ?.let { category ->
                    _uiState.value = _uiState.value.copy(
                        isEditMode = true,
                        name = category.name,
                        color = category.color,
                        iconName = category.iconName
                    )
                }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = null
        )
    }

    fun updateColor(color: String) {
        _uiState.value = _uiState.value.copy(color = color)
    }

    fun updateIcon(iconName: String) {
        _uiState.value = _uiState.value.copy(iconName = iconName)
    }

    fun saveCategory() {
        // Validate name
        if (_uiState.value.name.isBlank()) {
            _uiState.value = _uiState.value.copy(
                nameError = "Category name is required"
            )
            return
        }

        viewModelScope.launch {
            _saveState.value = SaveState.Saving

            val currentUser = authRepository.getCurrentUser().firstOrNull()
            val userId = currentUser?.id
            
            val newCategoryId = categoryId ?: UUID.randomUUID().toString()

            val category = Category(
                id = newCategoryId,
                userId = userId,
                name = _uiState.value.name.trim(),
                color = _uiState.value.color,
                iconName = _uiState.value.iconName,
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val result = if (categoryId != null) {
                categoryRepository.updateCategory(category)
            } else {
                categoryRepository.insertCategory(category)
            }

            result
                .onSuccess {
                    _saveState.value = SaveState.Success
                    // Emit the created category ID for new categories
                    if (categoryId == null) {
                        _categoryCreatedEvent.emit(newCategoryId)
                    }
                }
                .onFailure { e ->
                    _saveState.value = SaveState.Error(
                        e.message ?: "Failed to save category"
                    )
                }
        }
    }
}

/**
 * UI state for add/edit category screen
 */
data class AddEditCategoryUiState(
    val isEditMode: Boolean = false,
    val name: String = "",
    val color: String = "#FF6B6B",
    val iconName: String = "category",
    val nameError: String? = null
)

/**
 * Save state for category operations
 */
sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}
