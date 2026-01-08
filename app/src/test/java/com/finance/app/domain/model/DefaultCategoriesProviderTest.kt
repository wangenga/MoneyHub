package com.finance.app.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for DefaultCategoriesProvider.
 * 
 * These tests verify that the default expense categories are correctly structured
 * according to the requirements specified in the design document.
 */
class DefaultCategoriesProviderTest : FunSpec({

    test("getDefaultExpenseCategories returns exactly 4 categories") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        
        // Then
        defaultCategories shouldHaveSize 4
    }

    test("all default categories have categoryType = EXPENSE") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        
        // Then
        defaultCategories.forEach { category ->
            category.categoryType shouldBe CategoryType.EXPENSE
        }
    }

    test("all default categories have isDefault = true") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        
        // Then
        defaultCategories.forEach { category ->
            category.isDefault shouldBe true
        }
    }

    test("all default categories have userId = null") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        
        // Then
        defaultCategories.forEach { category ->
            category.userId shouldBe null
        }
    }

    test("each category has unique color") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        
        // Then
        val colors = defaultCategories.map { it.color }
        val uniqueColors = colors.toSet()
        uniqueColors shouldHaveSize 4
        
        // Verify specific expected colors
        colors shouldBe listOf("#2196F3", "#9C27B0", "#4CAF50", "#FF9800")
    }

    test("each category has unique icon") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        
        // Then
        val icons = defaultCategories.map { it.iconName }
        val uniqueIcons = icons.toSet()
        uniqueIcons shouldHaveSize 4
        
        // Verify specific expected icons
        icons shouldBe listOf("directions_car", "home", "shopping_cart", "movie")
    }

    test("categories have expected names and fixed IDs") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        
        // Then
        val categoryMap = defaultCategories.associateBy { it.id }
        
        categoryMap["default_transport"]?.name shouldBe "Transport"
        categoryMap["default_rent"]?.name shouldBe "Rent"
        categoryMap["default_groceries"]?.name shouldBe "Groceries"
        categoryMap["default_entertainment"]?.name shouldBe "Entertainment"
    }

    test("categories have proper color-icon-name combinations") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        val categoryMap = defaultCategories.associateBy { it.id }
        
        // Then - verify Transport (blue, car icon)
        val transport = categoryMap["default_transport"]!!
        transport.color shouldBe "#2196F3"
        transport.iconName shouldBe "directions_car"
        transport.name shouldBe "Transport"
        
        // Then - verify Rent (purple, home icon)
        val rent = categoryMap["default_rent"]!!
        rent.color shouldBe "#9C27B0"
        rent.iconName shouldBe "home"
        rent.name shouldBe "Rent"
        
        // Then - verify Groceries (green, shopping cart icon)
        val groceries = categoryMap["default_groceries"]!!
        groceries.color shouldBe "#4CAF50"
        groceries.iconName shouldBe "shopping_cart"
        groceries.name shouldBe "Groceries"
        
        // Then - verify Entertainment (orange, movie icon)
        val entertainment = categoryMap["default_entertainment"]!!
        entertainment.color shouldBe "#FF9800"
        entertainment.iconName shouldBe "movie"
        entertainment.name shouldBe "Entertainment"
    }

    test("all categories have valid timestamps") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        
        // Then
        defaultCategories.forEach { category ->
            category.createdAt shouldNotBe 0L
            category.updatedAt shouldNotBe 0L
            category.createdAt shouldBe category.updatedAt // Should be same for new categories
        }
    }

    test("categories have non-empty required fields") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        
        // Then
        defaultCategories.forEach { category ->
            category.id.isNotBlank() shouldBe true
            category.name.isNotBlank() shouldBe true
            category.color.isNotBlank() shouldBe true
            category.iconName.isNotBlank() shouldBe true
        }
    }
})