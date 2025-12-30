package com.finance.app.ui.transaction

/**
 * Navigation routes for transaction screens
 */
object TransactionRoutes {
    const val TRANSACTION_LIST = "transaction_list"
    const val ADD_TRANSACTION = "add_transaction"
    const val EDIT_TRANSACTION = "edit_transaction/{transactionId}"
    
    fun editTransaction(transactionId: String) = "edit_transaction/$transactionId"
}
