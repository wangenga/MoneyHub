package com.finance.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.finance.app.ui.common.NetworkState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes network connectivity state and provides reactive updates.
 * Uses both connectivity state and actual internet validation for accurate status.
 */
@Singleton
class NetworkStateObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    companion object {
        private const val CONNECTIVITY_CHECK_URL = "https://connectivitycheck.gstatic.com/generate_204"
        private const val CONNECTION_TIMEOUT_MS = 3000
        private const val READ_TIMEOUT_MS = 3000
        private const val EXPECTED_RESPONSE_CODE = 204
    }
    
    /**
     * Verifies actual internet access by pinging Google's connectivity check endpoint.
     * This provides more accurate detection than relying solely on NET_CAPABILITY_VALIDATED.
     * 
     * @return true if internet is reachable, false otherwise
     */
    suspend fun verifyInternetAccess(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(CONNECTIVITY_CHECK_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = CONNECTION_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.requestMethod = "HEAD"
                connection.useCaches = false
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                responseCode == EXPECTED_RESPONSE_CODE
            } catch (e: Exception) {
                // Any exception means we can't reach the internet
                false
            }
        }
    }
    
    /**
     * Observes network connectivity state changes.
     * Performs active reachability check when connectivity is detected.
     */
    fun observeNetworkState(): Flow<NetworkState> = callbackFlow {
        val scope = CoroutineScope(Dispatchers.IO)
        
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // When network becomes available, verify actual internet access
                scope.launch {
                    val hasInternet = verifyInternetAccess()
                    trySend(if (hasInternet) NetworkState.Connected else NetworkState.Disconnected)
                }
            }
            
            override fun onLost(network: Network) {
                trySend(NetworkState.Disconnected)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasCapability = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                if (hasCapability) {
                    // Verify with actual ping when capabilities indicate internet
                    scope.launch {
                        val hasInternet = verifyInternetAccess()
                        trySend(if (hasInternet) NetworkState.Connected else NetworkState.Disconnected)
                    }
                } else {
                    trySend(NetworkState.Disconnected)
                }
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Send initial state with verification
        scope.launch {
            val initialState = getCurrentNetworkStateWithVerification()
            trySend(initialState)
        }
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()
    
    /**
     * Gets the current network state with active internet verification.
     * This is a suspend function that performs actual reachability check.
     */
    suspend fun getCurrentNetworkStateWithVerification(): NetworkState {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        
        return if (network != null && networkCapabilities != null) {
            val hasCapability = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            if (hasCapability) {
                // Verify with actual ping
                if (verifyInternetAccess()) NetworkState.Connected else NetworkState.Disconnected
            } else {
                NetworkState.Disconnected
            }
        } else {
            NetworkState.Disconnected
        }
    }
    
    /**
     * Gets the current network state synchronously (without active verification).
     * Use getCurrentNetworkStateWithVerification() for more accurate results.
     */
    fun getCurrentNetworkState(): NetworkState {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        
        return if (network != null && networkCapabilities != null) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (hasInternet) NetworkState.Connected else NetworkState.Disconnected
        } else {
            NetworkState.Disconnected
        }
    }
    
    /**
     * Checks if network is currently available
     */
    fun isNetworkAvailable(): Boolean {
        return getCurrentNetworkState().isConnected
    }
}