package com.finance.app.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit tests for NetworkConnectivityChecker
 * Tests network connectivity detection functionality
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [28]) // Use API 28 for Robolectric compatibility
class NetworkConnectivityCheckerTest {

    private lateinit var context: Context
    private lateinit var networkConnectivityChecker: NetworkConnectivityChecker

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        networkConnectivityChecker = NetworkConnectivityChecker(context)
    }

    @Test
    fun networkConnectivityChecker_canBeInstantiated() {
        // When/Then - just verify the class can be instantiated without errors
        assertThat(networkConnectivityChecker).isNotNull()
    }

    @Test
    fun isNetworkAvailable_doesNotThrowException() {
        // When/Then - verify method doesn't throw exceptions
        try {
            networkConnectivityChecker.isNetworkAvailable()
            // Test passes if no exception is thrown
        } catch (e: Exception) {
            throw AssertionError("isNetworkAvailable should not throw exception", e)
        }
    }
}