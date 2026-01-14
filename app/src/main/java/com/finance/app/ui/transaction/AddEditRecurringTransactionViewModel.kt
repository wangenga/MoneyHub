package com.finance.app.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.finance.app.data.sync.NetworkStateObserver
import com.finance.app.domain.model.*
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.CategoryRepository
import com.finance.app.domain.repository.RecurringTransactionRepository
import com.finance.app.ui.common.AsyncState
import com.finance.app.ui.common.BaseViewModel
import com.finance.app.ui.common.NetworkState
import com.finance.app.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for add/edit recurring transaction screen
 */
@HiltViewModel
class AddEditRecurringTransactionViewModel @Inject constructor(
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
    errorHandler: ErrorHandler,
    networkStateObserver: NetworkStateObserver
) : BaseViewModel(errorHandler, networkStateObserver) {

    private val recurringTransactionId: String? = savedStateHandle.get<String>("recurringTransactionId")

    private val _uiState = MutableStateFlow(AddEditRecurringTransactionUiState())
    val uiState: StateFlow<AddEditRecurringTransactionUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _saveState = MutableStateFlow<AsyncState>(AsyncState.Idle)
    val saveState: StateFlow<AsyncState> = _saveState.asStateFlow()

    init {
        loadCategories()
        recurringTransactionId?.let { loadRecurringTransaction(it) }
        startNetworkObservation()
    }

    override fun onNetworkStateChanged(networkState: NetworkState) {
        if (networkState.isDisconnected && _saveState.value.isLoading) {
            _saveState.value = AsyncState.Error(
                "You're offline. Recurring transaction will be saved locally and synced when connection is restored.",
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
                android.util.Log.e("AddEditRecurringTransactionVM", "Error loading categories: $errorMessage")
            }
        )
    }

    private fun loadRecurringTransaction(id: String) {
        executeWithErrorHandling(
            operation = {
                recurringTransactionRepository.getRecurringTransactionById(id)
                    .filterNotNull()
                    .collect { recurringTransaction ->
                        val template = recurringTransaction.templateTransaction
                        _uiState.value = AddEditRecurringTransactionUiState(
                            amount = template.amount.toString(),
                            categoryId = template.categoryId,
                            startDate = recurringTransaction.nextDueDate,
                            type = template.type,
                            recurrencePattern = recurringTransaction.recurrencePattern,
                            paymentMethod = template.paymentMethod ?: "",
                            notes = template.notes ?: "",
                            isActive = recurringTransaction.isActive,
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

    fun updateStartDate(date: Long) {
        _uiState.value = _uiState.value.copy(startDate = date)
    }

    fun updateType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun updateRecurrencePattern(pattern: RecurrencePattern) {
        _uiState.value = _uiState.value.copy(recurrencePattern = pattern)
    }

    fun updatePaymentMethod(paymentMethod: String) {
        _uiState.value = _uiState.value.copy(paymentMethod = paymentMethod)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun updateIsActive(isActive: Boolean) {
        _uiState.value = _uiState.value.copy(isActive = isActive)
    }

    fun saveRecurringTransaction() {
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

        if (state.startDate < System.currentTimeMillis()) {
            _uiState.value = state.copy(startDateError = "Start date must be in the future")
            return
        }

        _saveState.value = AsyncState.Loading

        executeWithResult(
            operation = {
                val user = authRepository.getCurrentUser().firstOrNull()
                    ?: return@executeWithResult Result.failure(Exception("User not authenticated"))
                
                val now = System.currentTimeMillis()
                
                // Create template transaction
                val templateTransaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    type = state.type,
                    amount = amountValue,
                    categoryId = state.categoryId,
                    date = state.startDate,
                    paymentMethod = state.paymentMethod.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
                
                val recurringTransaction = RecurringTransaction(
                    id = recurringTransactionId ?: UUID.randomUUID().toString(),
                    userId = user.id,
                    templateTransaction = templateTransaction,
                    recurrencePattern = state.recurrencePattern,
                    nextDueDate = state.startDate,
                    isActive = state.isActive,
                    createdAt = if (recurringTransactionId == null) now else 0,
                    updatedAt = now
                )

                if (recurringTransactionId == null) {
                    recurringTransactionRepository.createRecurringTransaction(recurringTransaction)
                } else {
                    recurringTransactionRepository.updateRecurringTransaction(recurringTransaction)
                }
            },
            onSuccess = {
                _saveState.value = AsyncState.Success
            },
            onError = { errorMessage, isRetryable ->
                _saveState.value = if (isRetryable) {
                    createRetryableAsyncError(errorMessage) {
                        saveRecurringTransaction()
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
 * UI state for add/edit recurring transaction screen
 */
data class AddEditRecurringTransactionUiState(
    val amount: String = "",
    val categoryId: String = "",
    val startDate: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // Default to tomorrow
    val type: TransactionType = TransactionType.EXPENSE,
    val recurrencePattern: RecurrencePattern = RecurrencePattern.MONTHLY,
    val paymentMethod: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val amountError: String? = null,
    val categoryError: String? = null,
    val startDateError: String? = null,
    val isEditMode: Boolean = false
)
