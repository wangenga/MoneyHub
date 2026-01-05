package com.finance.app.ui.accessibility

import com.finance.app.ui.accessibility.AccessibilityUtils
import com.finance.app.ui.accessibility.createFinancialContentDescription
import com.finance.app.ui.accessibility.createTransactionContentDescription
import com.finance.app.ui.accessibility.createFormFieldContentDescription
import com.finance.app.ui.accessibility.createToggleContentDescription
import com.finance.app.ui.accessibility.createProgressContentDescription
import com.finance.app.ui.accessibility.createNavigationContentDescription
import com.finance.app.ui.accessibility.createDataVisualizationContentDescription
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for accessibility utilities and content description generation
 */
class AccessibilityTest {

    @Test
    fun testFinancialContentDescription() {
        val description = createFinancialContentDescription(
            title = "Total Balance",
            amount = "KSh 1,234.56",
            additionalInfo = "This month"
        )
        
        assertEquals("Total Balance: KSh 1,234.56, This month", description)
    }

    @Test
    fun testTransactionContentDescription() {
        val description = createTransactionContentDescription(
            category = "Food & Dining",
            amount = "KSh -25.50",
            date = "Dec 15, 2023",
            notes = "Lunch at restaurant",
            actionHint = "Double tap to edit"
        )
        
        assertTrue(description.contains("Transaction: Food & Dining"))
        assertTrue(description.contains("KSh -25.50"))
        assertTrue(description.contains("Dec 15, 2023"))
        assertTrue(description.contains("Note: Lunch at restaurant"))
        assertTrue(description.contains("Double tap to edit"))
    }

    @Test
    fun testFormFieldContentDescription() {
        val description = createFormFieldContentDescription(
            label = "Amount",
            value = "100.50",
            isError = false,
            errorMessage = null,
            isRequired = true
        )
        
        assertTrue(description.contains("Amount, required"))
        assertTrue(description.contains("current value: 100.50"))
    }

    @Test
    fun testFormFieldContentDescriptionWithError() {
        val description = createFormFieldContentDescription(
            label = "Amount",
            value = "",
            isError = true,
            errorMessage = "Amount is required",
            isRequired = true
        )
        
        assertTrue(description.contains("Amount, required"))
        assertTrue(description.contains("empty"))
        assertTrue(description.contains("error: Amount is required"))
    }

    @Test
    fun testToggleContentDescription() {
        val selectedDescription = createToggleContentDescription(
            label = "Income",
            isSelected = true,
            groupContext = "Transaction type"
        )
        
        assertTrue(selectedDescription.contains("Transaction type: Income, selected"))
        assertTrue(selectedDescription.contains("Double tap to deselect"))
        
        val unselectedDescription = createToggleContentDescription(
            label = "Expense",
            isSelected = false,
            groupContext = "Transaction type"
        )
        
        assertTrue(unselectedDescription.contains("Transaction type: Expense, not selected"))
        assertTrue(unselectedDescription.contains("Double tap to select"))
    }

    @Test
    fun testProgressContentDescription() {
        val description = createProgressContentDescription(
            operation = "Syncing data",
            progress = 75
        )
        
        assertEquals("Syncing data in progress, 75 percent complete", description)
    }

    @Test
    fun testNavigationContentDescription() {
        val description = createNavigationContentDescription(
            destination = "Analytics",
            currentLocation = "Home",
            hasNotification = true
        )
        
        assertTrue(description.contains("Navigate to Analytics"))
        assertTrue(description.contains("has notifications"))
    }

    @Test
    fun testDataVisualizationContentDescription() {
        val description = createDataVisualizationContentDescription(
            chartType = "Pie chart",
            title = "Spending by Category",
            summary = "Shows distribution across 5 categories",
            dataCount = 5
        )
        
        assertTrue(description.contains("Pie chart titled Spending by Category"))
        assertTrue(description.contains("Shows distribution across 5 categories"))
        assertTrue(description.contains("Contains 5 data points"))
        assertTrue(description.contains("Swipe right for detailed data breakdown"))
    }
}