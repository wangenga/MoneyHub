package com.finance.app.util

/**
 * Application-wide constants
 */
object Constants {
    // Database
    const val DATABASE_NAME = "finance_database"
    
    // Preferences
    const val PREFS_NAME = "finance_prefs"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    const val KEY_THEME_MODE = "theme_mode"
    
    // Sync
    const val SYNC_WORK_NAME = "finance_sync_work"
    const val SYNC_INTERVAL_HOURS = 24L
    const val FOREGROUND_SYNC_DELAY_SECONDS = 30L
    
    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_TRANSACTIONS = "transactions"
    const val COLLECTION_CATEGORIES = "categories"
    
    // Pagination
    const val TRANSACTIONS_PAGE_SIZE = 50
    
    // Biometric
    const val MAX_BIOMETRIC_ATTEMPTS = 3
}
