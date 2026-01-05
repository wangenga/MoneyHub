package com.finance.app.ui.settings

import app.cash.turbine.test
import com.finance.app.domain.biometric.BiometricAuthenticator
import com.finance.app.domain.repository.AuthRepository
import com.finance.app.domain.repository.SettingsRepository
import com.finance.app.domain.repository.SyncRepository
import com.finance.app.domain.repository.SyncState
import com.finance.app.ui.theme.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var biometricAuthenticator: BiometricAuthenticator
    private lateinit var syncRepository: SyncRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        settingsRepository = mockk(relaxed = true)
        biometricAuthenticator = mockk(relaxed = true)
        syncRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        
        // Setup default mocks
        every { settingsRepository.getThemeMode() } returns flowOf(ThemeMode.SYSTEM)
        every { settingsRepository.isDynamicColorEnabled() } returns flowOf(true)
        every { settingsRepository.isBiometricLockEnabled() } returns flowOf(false)
        every { biometricAuthenticator.isBiometricAvailable() } returns false
        coEvery { settingsRepository.getAppVersion() } returns "1.0.0"
        coEvery { settingsRepository.getLastSyncTimestamp() } returns null
        every { syncRepository.getSyncStatus() } returns flowOf(SyncState.Idle)
        
        viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            biometricAuthenticator = biometricAuthenticator,
            syncRepository = syncRepository,
            authRepository = authRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `logout calls authRepository signOut`() = runTest {
        // Given
        coEvery { authRepository.signOut() } returns Unit

        // When
        viewModel.logout()

        // Then
        coVerify { authRepository.signOut() }
    }

    @Test
    fun `logout success emits navigation event`() = runTest {
        // Given
        coEvery { authRepository.signOut() } returns Unit

        // When/Then
        viewModel.logoutEvent.test {
            viewModel.logout()
            awaitItem() // Should receive Unit
        }
    }

    @Test
    fun `logout failure updates error state`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { authRepository.signOut() } throws Exception(errorMessage)

        // When
        viewModel.logout()

        // Then
        assertEquals(errorMessage, viewModel.uiState.value.logoutError)
    }

    @Test
    fun `logout sets isLoggingOut to true during operation`() = runTest {
        // Given
        coEvery { authRepository.signOut() } coAnswers {
            // Check state during operation
            assertTrue(viewModel.uiState.value.isLoggingOut)
        }

        // When
        viewModel.logout()

        // Then - after completion, isLoggingOut should be false
        assertFalse(viewModel.uiState.value.isLoggingOut)
    }

    @Test
    fun `clearLogoutError clears the error state`() = runTest {
        // Given
        coEvery { authRepository.signOut() } throws Exception("Error")
        viewModel.logout()
        assertNotNull(viewModel.uiState.value.logoutError)

        // When
        viewModel.clearLogoutError()

        // Then
        assertNull(viewModel.uiState.value.logoutError)
    }
}
