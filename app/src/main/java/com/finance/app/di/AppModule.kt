package com.finance.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for application-level dependencies
 * Additional bindings will be added in subsequent tasks
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Dependency bindings will be added in subsequent tasks
}
