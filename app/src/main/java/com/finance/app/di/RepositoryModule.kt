package com.finance.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for repository dependencies
 * Repository bindings will be added in subsequent tasks
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repository bindings will be added in subsequent tasks
}
