package com.finance.app.domain.model

/**
 * Provider for system-defined default expense categories.
 * 
 * This object provides the four default expense categories that are available to all users:
 * Transport, Rent, Groceries, and Entertainment. Each category has predefined colors and icons
 * to provide a consistent user experience.
 */
object DefaultCategoriesProvider {
    
    /**
     * Returns the list of default expense categories available to all users.
     * 
     * The categories include:
     * - Transport (blue, car icon)
     * - Rent (purple, home icon) 
     * - Groceries (green, shopping cart icon)
     * - Entertainment (orange, movie icon)
     * 
     * @return List of 4 default expense categories with fixed IDs, colors, and icons
     */
    fun getDefaultExpenseCategories(): List<Category> {
        val currentTime = System.currentTimeMillis()
        
        return listOf(
            Category(
                id = "default_transport",
                userId = null,
                name = "Transport",
                color = "#2196F3",  // Blue
                iconName = "directions_car",
                categoryType = CategoryType.EXPENSE,
                isDefault = true,
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            Category(
                id = "default_rent",
                userId = null,
                name = "Rent",
                color = "#9C27B0",  // Purple
                iconName = "home",
                categoryType = CategoryType.EXPENSE,
                isDefault = true,
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            Category(
                id = "default_groceries",
                userId = null,
                name = "Groceries",
                color = "#4CAF50",  // Green
                iconName = "shopping_cart",
                categoryType = CategoryType.EXPENSE,
                isDefault = true,
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            Category(
                id = "default_entertainment",
                userId = null,
                name = "Entertainment",
                color = "#FF9800",  // Orange
                iconName = "movie",
                categoryType = CategoryType.EXPENSE,
                isDefault = true,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        )
    }
}