package com.finance.app.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.app.data.sync.NetworkConnectivityChecker
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ConnectionTestUiState(
    val isNetworkAvailable: Boolean = false,
    val isWifiConnected: Boolean = false,
    val isCellularConnected: Boolean = false,
    val isFirebaseInitialized: Boolean = false,
    val authStatus: String = "checking",
    val userEmail: String = "",
    val isTestingFirestore: Boolean = false,
    val isTestingAuth: Boolean = false,
    val testResults: String = ""
)

@HiltViewModel
class ConnectionTestViewModel @Inject constructor(
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ConnectionTestUiState())
    val uiState: StateFlow<ConnectionTestUiState> = _uiState.asStateFlow()
    
    init {
        refreshStatus()
    }
    
    fun refreshStatus() {
        viewModelScope.launch {
            try {
                // Check network status
                val isNetworkAvailable = networkConnectivityChecker.isNetworkAvailable()
                val isWifiConnected = networkConnectivityChecker.isWifiConnected()
                val isCellularConnected = networkConnectivityChecker.isCellularConnected()
                
                // Check Firebase initialization
                val isFirebaseInitialized = try {
                    FirebaseApp.getInstance() != null
                } catch (e: Exception) {
                    false
                }
                
                // Check auth status
                val currentUser = firebaseAuth.currentUser
                val authStatus = if (currentUser != null) "authenticated" else "not_authenticated"
                val userEmail = currentUser?.email ?: ""
                
                _uiState.value = _uiState.value.copy(
                    isNetworkAvailable = isNetworkAvailable,
                    isWifiConnected = isWifiConnected,
                    isCellularConnected = isCellularConnected,
                    isFirebaseInitialized = isFirebaseInitialized,
                    authStatus = authStatus,
                    userEmail = userEmail
                )
            } catch (e: Exception) {
                appendTestResult("Error refreshing status: ${e.message}")
            }
        }
    }
    
    fun testFirestoreConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingFirestore = true)
            
            try {
                appendTestResult("Testing Firestore connection...")
                
                // Try to read from Firestore
                val testDoc = firestore.collection("test").document("connection")
                val result = testDoc.get().await()
                
                appendTestResult("✅ Firestore connection successful")
                appendTestResult("Document exists: ${result.exists()}")
                
            } catch (e: Exception) {
                appendTestResult("❌ Firestore connection failed: ${e.message}")
                appendTestResult("Error type: ${e.javaClass.simpleName}")
                
                // Provide specific error guidance
                when {
                    e.message?.contains("PERMISSION_DENIED") == true -> {
                        appendTestResult("💡 Check Firestore security rules")
                    }
                    e.message?.contains("UNAVAILABLE") == true -> {
                        appendTestResult("💡 Check internet connection")
                    }
                    e.message?.contains("UNAUTHENTICATED") == true -> {
                        appendTestResult("💡 User needs to be authenticated")
                    }
                }
            } finally {
                _uiState.value = _uiState.value.copy(isTestingFirestore = false)
            }
        }
    }
    
    fun testAuthentication() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingAuth = true)
            
            try {
                appendTestResult("Testing Firebase Auth connection...")
                
                // Try to get current auth state
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    appendTestResult("✅ User is authenticated")
                    appendTestResult("User ID: ${currentUser.uid}")
                    appendTestResult("Email: ${currentUser.email}")
                    
                    // Try to get ID token
                    val tokenResult = currentUser.getIdToken(false).await()
                    appendTestResult("✅ ID Token retrieved successfully")
                    
                } else {
                    appendTestResult("ℹ️ No user is currently authenticated")
                    appendTestResult("💡 Try signing in first")
                }
                
            } catch (e: Exception) {
                appendTestResult("❌ Authentication test failed: ${e.message}")
                appendTestResult("Error type: ${e.javaClass.simpleName}")
                
                when {
                    e.message?.contains("network") == true -> {
                        appendTestResult("💡 Check internet connection")
                    }
                    e.message?.contains("api") == true -> {
                        appendTestResult("💡 Check Firebase project configuration")
                    }
                }
            } finally {
                _uiState.value = _uiState.value.copy(isTestingAuth = false)
            }
        }
    }
    
    private fun appendTestResult(message: String) {
        val timestamp = System.currentTimeMillis()
        val formattedMessage = "[${timestamp % 100000}] $message\n"
        _uiState.value = _uiState.value.copy(
            testResults = _uiState.value.testResults + formattedMessage
        )
    }
}