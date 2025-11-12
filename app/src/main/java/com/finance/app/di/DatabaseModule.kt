package com.finance.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for database dependencies
 * Room database setup will be added in task 2
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // Database bindings will be added in task 2
}
