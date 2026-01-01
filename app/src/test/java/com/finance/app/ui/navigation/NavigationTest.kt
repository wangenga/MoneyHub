package com.finance.app.ui.navigation

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for navigation routes and structure
 */
class NavigationTest {
    
    @Test
    fun `navigation routes are properly defined`() {
        // Test that all navigation routes are non-empty strings
        assertFalse(NavigationRoutes.AUTH_GRAPH.isEmpty())
        assertFalse(NavigationRoutes.MAIN_GRAPH.isEmpty())
        assertFalse(NavigationRoutes.HOME.isEmpty())
        assertFalse(NavigationRoutes.REPORTS.isEmpty())
        assertFalse(NavigationRoutes.SETTINGS.isEmpty())
        assertFalse(NavigationRoutes.LOGIN.isEmpty())
        assertFalse(NavigationRoutes.REGISTER.isEmpty())
        assertFalse(NavigationRoutes.BIOMETRIC_LOCK.isEmpty())
    }
    
    @Test
    fun `bottom navigation items are properly configured`() {
        val items = BottomNavigationItem.items
        
        // Should have exactly 3 items
        assertEquals(3, items.size)
        
        // Check that all items have proper routes
        val routes = items.map { it.route }
        assertTrue(routes.contains(NavigationRoutes.HOME))
        assertTrue(routes.contains(NavigationRoutes.REPORTS))
        assertTrue(routes.contains(NavigationRoutes.SETTINGS))
        
        // Check that all items have titles
        items.forEach { item ->
            assertFalse(item.title.isEmpty())
        }
    }
    
    @Test
    fun `deep link routes are properly formatted`() {
        // Test deep link format
        assertTrue(NavigationRoutes.DEEP_LINK_TRANSACTION.startsWith("finance://"))
        assertTrue(NavigationRoutes.DEEP_LINK_ADD_TRANSACTION.startsWith("finance://"))
        assertTrue(NavigationRoutes.DEEP_LINK_REPORTS.startsWith("finance://"))
    }
    
    @Test
    fun `parameterized route helpers work correctly`() {
        val transactionId = "test-transaction-123"
        val categoryId = "test-category-456"
        
        val transactionRoute = NavigationRoutes.editTransaction(transactionId)
        val categoryRoute = NavigationRoutes.editCategory(categoryId)
        
        assertTrue(transactionRoute.contains(transactionId))
        assertTrue(categoryRoute.contains(categoryId))
    }
}