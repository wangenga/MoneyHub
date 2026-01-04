package com.finance.app.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.finance.app.domain.biometric.BiometricAuthenticator
import com.finance.app.domain.repository.SettingsRepository
import com.finance.app.domain.repository.SyncRepository
import com.finance.app.ui.theme.ThemeManager
import com.finance.app.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeManager: ThemeManager,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val syncRepository: SyncRepository
) : SettingsRepository {
    
    override fun getThemeMode(): Flow<ThemeMode> {
        return themeManager.themeMode
    }
    
    override suspend fun setThemeMode(mode: ThemeMode) {
        themeManager.setThemeMode(mode)
    }
    
    override fun isDynamicColorEnabled(): Flow<Boolean> {
        return themeManager.isDynamicColorEnabled
    }
    
    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        themeManager.setDynamicColorEnabled(enabled)
    }
    
    override fun isBiometricLockEnabled(): Flow<Boolean> {
        // Create a flow that emits the current biometric state
        return flowOf(biometricAuthenticator.isBiometricEnabled())
    }
    
    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        biometricAuthenticator.setBiometricEnabled(enabled)
    }
    
    override fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }
    
    override suspend fun getLastSyncTimestamp(): Long? {
        return syncRepository.getLastSyncTimestamp()
    }
    
    override suspend fun triggerManualSync(): Result<Unit> {
        return syncRepository.forceSyncAll()
    }
}