package com.finance.app.di

import com.finance.app.data.biometric.BiometricAuthenticatorImpl
import com.finance.app.domain.biometric.BiometricAuthenticator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing biometric authentication dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BiometricModule {
    
    @Binds
    @Singleton
    abstract fun bindBiometricAuthenticator(
        biometricAuthenticatorImpl: BiometricAuthenticatorImpl
    ): BiometricAuthenticator
}
