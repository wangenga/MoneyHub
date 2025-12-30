package com.finance.app.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.SyncStatus
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for add/edit transaction screen
 */
@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String? = savedStateHandle.get<String>("transactionId")

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    init {
        loadCategories()
        transactionId?.let { loadTransaction(it) }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories()
                .catch { e ->
                    android.util.Log.e("AddEditTransactionVM", "Error loading categories", e)
                }
                .collect { categories ->
                    _categories.value = categories
                }
        }
    }

    private fun loadTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.getTransactionById(id)
                .catch { e ->
                    _saveState.value = SaveState.Error(
                        e.message ?: "Failed to load transaction"
                    )
                }
                .filterNotNull()
                .collect { transaction ->
                    _uiState.value = AddEditTransactionUiState(
                        amount = transaction.amount.toString(),
                        categoryId = transaction.categoryId,
                        date = transaction.date,
                        type = transaction.type,
                        paymentMethod = transaction.paymentMethod ?: "",
                        notes = transaction.notes ?: "",
                        isEditMode = true
                    )
                }
        }
    }

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(
            amount = amount,
            amountError = null
        )
    }

    fun updateCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(
            categoryId = categoryId,
            categoryError = null
        )
    }

    fun updateDate(date: Long) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun updateType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun updatePaymentMethod(paymentMethod: String) {
        _uiState.value = _uiState.value.copy(paymentMethod = paymentMethod)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun saveTransaction() {
        val state = _uiState.value
        
        // Validate
        val amountValue = state.amount.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            _uiState.value = state.copy(amountError = "Please enter a valid amount")
            return
        }

        if (state.categoryId.isEmpty()) {
            _uiState.value = state.copy(categoryError = "Please select a category")
            return
        }

        _saveState.value = SaveState.Saving

        viewModelScope.launch {
            authRepository.getCurrentUser()
                .firstOrNull()
                ?.let { user ->
                    val now = System.currentTimeMillis()
                    val transaction = Transaction(
                        id = transactionId ?: UUID.randomUUID().toString(),
                        userId = user.id,
                        type = state.type,
                        amount = amountValue,
                        categoryId = state.categoryId,
                        date = state.date,
                        paymentMethod = state.paymentMethod.ifBlank { null },
                        notes = state.notes.ifBlank { null },
                        createdAt = if (transactionId == null) now else 0, // Will be preserved if editing
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING
                    )

                    val result = if (transactionId == null) {
                        transactionRepository.insertTransaction(transaction)
                    } else {
                        transactionRepository.updateTransaction(transaction)
                    }

                    result
                        .onSuccess {
                            _saveState.value = SaveState.Success
                        }
                        .onFailure { e ->
                            _saveState.value = SaveState.Error(
                                e.message ?: "Failed to save transaction"
                            )
                        }
                } ?: run {
                _saveState.value = SaveState.Error("User not authenticated")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}

/**
 * UI state for add/edit transaction screen
 */
data class AddEditTransactionUiState(
    val amount: String = "",
    val categoryId: String = "",
    val date: Long = System.currentTimeMillis(),
    val type: TransactionType = TransactionType.EXPENSE,
    val paymentMethod: String = "",
    val notes: String = "",
    val amountError: String? = null,
    val categoryError: String? = null,
    val isEditMode: Boolean = false
)

/**
 * Save state for transaction
 */
sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}
