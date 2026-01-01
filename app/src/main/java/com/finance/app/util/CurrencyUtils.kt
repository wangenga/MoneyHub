package com.finance.app.util

import com.finance.app.domain.model.TransactionType
import java.text.NumberFormat
import java.util.*

/**
 * Utility class for currency formatting
 */
object CurrencyUtils {
    
    /**
     * The currency code used throughout the app
     */
    const val CURRENCY_CODE = "KES" // Kenyan Shilling
    const val CURRENCY_SYMBOL = "KSh"
    
    /**
     * Format amount with KSh currency symbol
     */
    fun formatAmount(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return "$CURRENCY_SYMBOL ${formatter.format(amount)}"
    }
    
    /**
     * Format amount with transaction type prefix (+ for income, - for expense)
     */
    fun formatAmountWithType(amount: Double, type: TransactionType): String {
        val formatted = formatAmount(amount)
        return when (type) {
            TransactionType.INCOME -> "+$formatted"
            TransactionType.EXPENSE -> "-$formatted"
        }
    }
    
    /**
     * Get currency formatter for consistent formatting across the app
     */
    fun getCurrencyFormatter(): NumberFormat {
        return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
}