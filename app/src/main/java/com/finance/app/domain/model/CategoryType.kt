package com.finance.app.domain.model

/**
 * Enumeration distinguishing between income and expense categories.
 * 
 * - INCOME: Categories used for income transactions (user-created only)
 * - EXPENSE: Categories used for expense transactions (includes system defaults and user-created)
 */
enum class CategoryType {
    INCOME,
    EXPENSE
}
