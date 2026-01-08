package com.finance.app.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.finance.app.data.sync.NetworkStateObserver
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.SyncStatus
import com.finance.app.domain.model.Transaction
import com.finance.app.domain.model.TransactionType
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.domain.repository.TransactionRepository
import com.finance.app.ui.common.AsyncState
import com.finance.app.ui.common.BaseViewModel
import com.finance.app.ui.common.NetworkState
import com.finance.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for add/edit transaction screen with enhanced error handling
 */
@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
    errorHandler: ErrorHandler,
    networkStateObserver: NetworkStateObserver
) : BaseViewModel(errorHandler, networkStateObserver) {

    private val transactionId: String? = savedStateHandle.get<String>("transactionId")

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _saveState = MutableStateFlow<AsyncState>(AsyncState.Idle)
    val saveState: StateFlow<AsyncState> = _saveState.asStateFlow()

    init {
        loadCategories()
        transactionId?.let { loadTransaction(it) }
        startNetworkObservation()
    }

    override fun onNetworkStateChanged(networkState: NetworkState) {
        // Show offline message if trying to save while offline
        if (networkState.isDisconnected && _saveState.value.isLoading) {
            _saveState.value = AsyncState.Error(
                "You're offline. Transaction will be saved locally and synced when connection is restored.",
                false
            )
        }
    }

    private fun loadCategories() {
        executeWithErrorHandling(
            operation = {
                authRepository.getCurrentUser().collect { user ->
                    if (user != null) {
                        categoryRepository.getAllCategories(user.id)
                            .collect { categories ->
                                _categories.value = categories
                            }
                    }
                }
            },
            onError = { errorMessage ->
                android.util.Log.e("AddEditTransactionVM", "Error loading categories: $errorMessage")
            }
        )
    }

    private fun loadTransaction(id: String) {
        executeWithErrorHandling(
            operation = {
                transactionRepository.getTransactionById(id)
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
            },
            onError = { errorMessage ->
                _saveState.value = AsyncState.Error(errorMessage, false)
            }
        )
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

        _saveState.value = AsyncState.Loading

        executeWithResult(
            operation = {
                val user = authRepository.getCurrentUser().firstOrNull()
                    ?: return@executeWithResult Result.failure(Exception("User not authenticated"))
                
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

                if (transactionId == null) {
                    transactionRepository.insertTransaction(transaction)
                } else {
                    transactionRepository.updateTransaction(transaction)
                }
            },
            onSuccess = {
                _saveState.value = AsyncState.Success
            },
            onError = { errorMessage, isRetryable ->
                _saveState.value = if (isRetryable) {
                    createRetryableAsyncError(errorMessage) {
                        saveTransaction()
                    }
                } else {
                    AsyncState.Error(errorMessage, false)
                }
            }
        )
    }

    fun resetSaveState() {
        _saveState.value = AsyncState.Idle
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
