package com.finance.app.data.repository

import android.content.SharedPreferences
import com.finance.app.domain.repository.OnboardingRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class OnboardingRepositoryImplTest {
    
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var onboardingRepository: OnboardingRepository
    
    @Before
    fun setup() {
        sharedPreferences = mockk()
        editor = mockk()
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit
        
        onboardingRepository = OnboardingRepositoryImpl(sharedPreferences)
    }
    
    @Test
    fun `isOnboardingCompleted returns false by default`() = runTest {
        // Given
        every { sharedPreferences.getBoolean("onboarding_completed", false) } returns false
        
        // When
        val result = onboardingRepository.isOnboardingCompleted()
        
        // Then
        assertFalse(result)
        verify { sharedPreferences.getBoolean("onboarding_completed", false) }
    }
    
    @Test
    fun `isOnboardingCompleted returns true when completed`() = runTest {
        // Given
        every { sharedPreferences.getBoolean("onboarding_completed", false) } returns true
        
        // When
        val result = onboardingRepository.isOnboardingCompleted()
        
        // Then
        assertTrue(result)
        verify { sharedPreferences.getBoolean("onboarding_completed", false) }
    }
    
    @Test
    fun `setOnboardingCompleted stores true value`() = runTest {
        // When
        onboardingRepository.setOnboardingCompleted()
        
        // Then
        verify { editor.putBoolean("onboarding_completed", true) }
        verify { editor.apply() }
    }
    
    @Test
    fun `resetOnboarding removes the key`() = runTest {
        // When
        onboardingRepository.resetOnboarding()
        
        // Then
        verify { editor.remove("onboarding_completed") }
        verify { editor.apply() }
    }
}