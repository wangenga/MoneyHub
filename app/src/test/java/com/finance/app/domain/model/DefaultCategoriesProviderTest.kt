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

    test("default categories have correct structure and properties") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        
        // Then - verify count and basic properties
        defaultCategories shouldHaveSize 4
        defaultCategories.forEach { category ->
            category.categoryType shouldBe CategoryType.EXPENSE
            category.isDefault shouldBe true
            category.userId shouldBe null
            category.createdAt shouldNotBe 0L
            category.updatedAt shouldNotBe 0L
        }
    }

    test("categories have unique colors and icons") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        
        // Then
        val colors = defaultCategories.map { it.color }
        val icons = defaultCategories.map { it.iconName }
        
        colors.toSet() shouldHaveSize 4
        icons.toSet() shouldHaveSize 4
        
        colors shouldBe listOf("#2196F3", "#9C27B0", "#4CAF50", "#FF9800")
        icons shouldBe listOf("T", "R", "G", "E")
    }

    test("categories have expected IDs and names") {
        // Given
        val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
        val categoryMap = defaultCategories.associateBy { it.id }
        
        // Then
        categoryMap["default_transport"]?.name shouldBe "Transport"
        categoryMap["default_rent"]?.name shouldBe "Rent"
        categoryMap["default_groceries"]?.name shouldBe "Groceries"
        categoryMap["default_entertainment"]?.name shouldBe "Entertainment"
    }
})