package com.finance.app.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.model.Category
import com.finance.app.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for category management screen
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories()
                .catch { e ->
                    _uiState.value = CategoryUiState.Error(
                        e.message ?: "Failed to load categories"
                    )
                }
                .collect { categories ->
                    _uiState.value = if (categories.isEmpty()) {
                        CategoryUiState.Empty
                    } else {
                        CategoryUiState.Success(categories)
                    }
                }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            categoryRepository.deleteCategory(categoryId)
                .onSuccess {
                    _deleteState.value = DeleteState.Success
                    // Reset delete state after a short delay
                    kotlinx.coroutines.delay(1000)
                    _deleteState.value = DeleteState.Idle
                }
                .onFailure { e ->
                    _deleteState.value = DeleteState.Error(
                        e.message ?: "Failed to delete category"
                    )
                }
        }
    }

    fun clearDeleteError() {
        _deleteState.value = DeleteState.Idle
    }
}

/**
 * UI state for category list screen
 */
sealed class CategoryUiState {
    object Loading : CategoryUiState()
    object Empty : CategoryUiState()
    data class Success(val categories: List<Category>) : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}

/**
 * State for delete operations
 */
sealed class DeleteState {
    object Idle : DeleteState()
    object Deleting : DeleteState()
    object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}
