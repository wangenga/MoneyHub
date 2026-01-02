package com.finance.app.domain.repository

/**
 * Repository for managing onboarding state and preferences
 */
interface OnboardingRepository {
    /**
     * Check if the user has completed onboarding
     */
    suspend fun isOnboardingCompleted(): Boolean
    
    /**
     * Mark onboarding as completed
     */
    suspend fun setOnboardingCompleted()
    
    /**
     * Reset onboarding state (for testing purposes)
     */
    suspend fun resetOnboarding()
}