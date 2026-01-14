package com.finance.app.ui.transaction

/**
 * Navigation routes for transaction screens
 */
object TransactionRoutes {
    const val TRANSACTION_LIST = "transaction_list"
    const val ADD_TRANSACTION = "add_transaction"
    const val EDIT_TRANSACTION = "edit_transaction/{transactionId}"
    const val RECURRING_TRANSACTION_LIST = "recurring_transaction_list"
    const val ADD_RECURRING_TRANSACTION = "add_recurring_transaction"
    const val EDIT_RECURRING_TRANSACTION = "edit_recurring_transaction/{recurringTransactionId}"
    
    fun editTransaction(transactionId: String) = "edit_transaction/$transactionId"
    fun editRecurringTransaction(recurringTransactionId: String) = "edit_recurring_transaction/$recurringTransactionId"
}
