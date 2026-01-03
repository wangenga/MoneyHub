package com.finance.app.domain.repository

import com.finance.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing app settings and preferences
 */
interface SettingsRepository {
    
    /**
     * Get the current theme mode
     */
    fun getThemeMode(): Flow<ThemeMode>
    
    /**
     * Set the theme mode
     */
    suspend fun setThemeMode(mode: ThemeMode)
    
    /**
     * Get dynamic color preference
     */
    fun isDynamicColorEnabled(): Flow<Boolean>
    
    /**
     * Set dynamic color preference
     */
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    
    /**
     * Get biometric lock preference
     */
    fun isBiometricLockEnabled(): Flow<Boolean>
    
    /**
     * Set biometric lock preference
     */
    suspend fun setBiometricLockEnabled(enabled: Boolean)
    
    /**
     * Get app version name
     */
    fun getAppVersion(): String
    
    /**
     * Get last sync timestamp
     */
    suspend fun getLastSyncTimestamp(): Long?
    
    /**
     * Trigger manual sync
     */
    suspend fun triggerManualSync(): Result<Unit>
}