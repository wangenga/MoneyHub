package com.finance.app.ui.navigation

/**
 * Navigation routes for the entire application
 */
object NavigationRoutes {
    // Auth routes
    const val AUTH_GRAPH = "auth_graph"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val BIOMETRIC_LOCK = "biometric_lock"
    
    // Main app routes
    const val MAIN_GRAPH = "main_graph"
    const val HOME = "home"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    
    // Transaction routes
    const val TRANSACTION_LIST = "transaction_list"
    const val ADD_TRANSACTION = "add_transaction"
    const val EDIT_TRANSACTION = "edit_transaction/{transactionId}"
    
    // Category routes
    const val CATEGORY_MANAGEMENT = "category_management"
    const val ADD_CATEGORY = "add_category"
    const val ADD_CATEGORY_FROM_TRANSACTION = "add_category_from_transaction"
    const val EDIT_CATEGORY = "edit_category/{categoryId}"
    
    // Navigation result keys
    const val NEW_CATEGORY_ID_KEY = "new_category_id"
    
    // Onboarding
    const val ONBOARDING = "onboarding"
    
    // Debug routes
    const val CONNECTION_TEST = "connection_test"
    const val DATABASE_DEBUG = "database_debug"
    
    // Deep link routes
    const val DEEP_LINK_TRANSACTION = "finance://transaction/{transactionId}"
    const val DEEP_LINK_ADD_TRANSACTION = "finance://add_transaction"
    const val DEEP_LINK_REPORTS = "finance://reports"
    
    // Helper functions for parameterized routes
    fun editTransaction(transactionId: String) = "edit_transaction/$transactionId"
    fun editCategory(categoryId: String) = "edit_category/$categoryId"
}