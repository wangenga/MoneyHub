package com.finance.app.data.repository

import android.content.SharedPreferences
import com.finance.app.domain.repository.OnboardingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OnboardingRepository using SharedPreferences
 */
@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : OnboardingRepository {
    
    companion object {
        private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    }
    
    override suspend fun isOnboardingCompleted(): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.getBoolean(ONBOARDING_COMPLETED_KEY, false)
    }
    
    override suspend fun setOnboardingCompleted() = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putBoolean(ONBOARDING_COMPLETED_KEY, true)
            .apply()
    }
    
    override suspend fun resetOnboarding() = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .remove(ONBOARDING_COMPLETED_KEY)
            .apply()
    }
}