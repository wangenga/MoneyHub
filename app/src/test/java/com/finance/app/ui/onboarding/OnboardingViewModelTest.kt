package com.finance.app.ui.onboarding

import com.finance.app.domain.repository.OnboardingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    
    private lateinit var onboardingRepository: OnboardingRepository
    private lateinit var viewModel: OnboardingViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        onboardingRepository = mockk()
        coEvery { onboardingRepository.isOnboardingCompleted() } returns false
        
        viewModel = OnboardingViewModel(onboardingRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state has correct values`() {
        // Then
        val state = viewModel.uiState.value
        assertEquals(0, state.currentPage)
        assertFalse(state.isOnboardingCompleted)
        assertFalse(state.shouldNavigateToAuth)
    }
    
    @Test
    fun `nextPage increments current page`() {
        // When
        viewModel.nextPage()
        
        // Then
        assertEquals(1, viewModel.uiState.value.currentPage)
    }
    
    @Test
    fun `nextPage does not exceed max pages`() {
        // Given - navigate to last page
        repeat(viewModel.pages.size - 1) {
            viewModel.nextPage()
        }
        val lastPage = viewModel.uiState.value.currentPage
        
        // When - try to go beyond last page
        viewModel.nextPage()
        
        // Then - should stay on last page
        assertEquals(lastPage, viewModel.uiState.value.currentPage)
    }
    
    @Test
    fun `previousPage decrements current page`() {
        // Given
        viewModel.nextPage() // Go to page 1
        
        // When
        viewModel.previousPage()
        
        // Then
        assertEquals(0, viewModel.uiState.value.currentPage)
    }
    
    @Test
    fun `previousPage does not go below zero`() {
        // When
        viewModel.previousPage()
        
        // Then
        assertEquals(0, viewModel.uiState.value.currentPage)
    }
    
    @Test
    fun `skipOnboarding marks as completed and triggers navigation`() = runTest {
        // Given
        coEvery { onboardingRepository.setOnboardingCompleted() } returns Unit
        
        // When
        viewModel.skipOnboarding()
        
        // Then
        coVerify { onboardingRepository.setOnboardingCompleted() }
        val state = viewModel.uiState.value
        assertTrue(state.isOnboardingCompleted)
        assertTrue(state.shouldNavigateToAuth)
    }
    
    @Test
    fun `completeOnboarding marks as completed and triggers navigation`() = runTest {
        // Given
        coEvery { onboardingRepository.setOnboardingCompleted() } returns Unit
        
        // When
        viewModel.completeOnboarding()
        
        // Then
        coVerify { onboardingRepository.setOnboardingCompleted() }
        val state = viewModel.uiState.value
        assertTrue(state.isOnboardingCompleted)
        assertTrue(state.shouldNavigateToAuth)
    }
    
    @Test
    fun `onNavigationHandled resets navigation flag`() = runTest {
        // Given
        coEvery { onboardingRepository.setOnboardingCompleted() } returns Unit
        viewModel.skipOnboarding()
        assertTrue(viewModel.uiState.value.shouldNavigateToAuth)
        
        // When
        viewModel.onNavigationHandled()
        
        // Then
        assertFalse(viewModel.uiState.value.shouldNavigateToAuth)
    }
}