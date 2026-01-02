package com.finance.app.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.local.dao.TransactionDao
import com.finance.app.data.local.dao.UserDao
import com.finance.app.data.local.entity.CategoryEntity
import com.finance.app.data.local.entity.TransactionEntity
import com.finance.app.data.local.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for database debug screen
 */
@HiltViewModel
class DatabaseDebugViewModel @Inject constructor(
    private val userDao: UserDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DatabaseDebugUiState())
    val uiState: StateFlow<DatabaseDebugUiState> = _uiState.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            try {
                val users = userDao.getAllUsers().first()
                val categories = categoryDao.getAllCategories().first()
                val transactions = transactionDao.getAllTransactionsForDebug().first()
                
                _uiState.value = DatabaseDebugUiState(
                    users = users,
                    categories = categories,
                    transactions = transactions,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}

data class DatabaseDebugUiState(
    val users: List<UserEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)