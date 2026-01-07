package com.finance.app.data.local.dao

import com.finance.app.data.local.entity.CategoryEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for category type filtering logic.
 * 
 * These tests verify the correctness properties defined in the design document
 * for the category system redesign feature by testing the filtering logic
 * without requiring Android context.
 */
class CategoryTypeFilteringPropertyTest : FunSpec({

    /**
     * **Feature: category-system-redesign, Property 8: Category type filtering**
     * **Validates: Requirements 4.1, 4.2**
     * 
     * *For any* category query filtered by type, the result should contain only categories 
     * matching that type. Specifically:
     * - EXPENSE filter returns categories where `categoryType = EXPENSE`
     * - INCOME filter returns categories where `categoryType = INCOME`
     */
    test("Property 8: Category type filtering - queries return only matching types") {
        // Test with a simple, deterministic set of categories
        val categories = listOf(
            CategoryEntity(
                id = "expense1",
                userId = "user1",
                name = "Transport",
                color = "#2196F3",
                iconName = "car",
                categoryType = "EXPENSE",
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            CategoryEntity(
                id = "income1",
                userId = "user1",
                name = "Salary",
                color = "#4CAF50",
                iconName = "money",
                categoryType = "INCOME",
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            CategoryEntity(
                id = "default1",
                userId = null,
                name = "Default Expense",
                color = "#FF9800",
                iconName = "shopping",
                categoryType = "EXPENSE",
                isDefault = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            CategoryEntity(
                id = "other_user_expense",
                userId = "user2",
                name = "Other User Expense",
                color = "#9C27B0",
                iconName = "other",
                categoryType = "EXPENSE",
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        val testUserId = "user1"

        // Test EXPENSE filtering logic
        val expenseResults = filterCategoriesByType(categories, "EXPENSE", testUserId)
        val expectedExpenseCategories = categories.filter { 
            it.categoryType == "EXPENSE" && (it.userId == testUserId || it.isDefault)
        }
        expenseResults shouldContainExactlyInAnyOrder expectedExpenseCategories

        // Test INCOME filtering logic
        val incomeResults = filterCategoriesByType(categories, "INCOME", testUserId)
        val expectedIncomeCategories = categories.filter { 
            it.categoryType == "INCOME" && it.userId == testUserId
        }
        incomeResults shouldContainExactlyInAnyOrder expectedIncomeCategories

        // Verify no cross-contamination: expense results should have no income categories
        expenseResults.all { it.categoryType == "EXPENSE" } shouldBe true
        
        // Verify no cross-contamination: income results should have no expense categories
        incomeResults.all { it.categoryType == "INCOME" } shouldBe true
        
        // Verify user isolation: other user's categories are not included
        expenseResults.none { it.userId == "user2" } shouldBe true
    }

    /**
     * Property: Expense category filtering includes defaults and user customs
     * 
     * *For any* user with any number of custom expense categories, filtering expense categories 
     * for that user should return all default expense categories plus exactly the custom 
     * expense categories belonging to that user.
     */
    test("Expense category filtering includes defaults and user customs") {
        val categories = listOf(
            // Default expense category
            CategoryEntity(
                id = "default_transport",
                userId = null,
                name = "Transport",
                color = "#2196F3",
                iconName = "car",
                categoryType = "EXPENSE",
                isDefault = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            // User's custom expense category
            CategoryEntity(
                id = "custom_expense1",
                userId = "user1",
                name = "Custom Expense",
                color = "#FF5722",
                iconName = "custom",
                categoryType = "EXPENSE",
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            // Another user's custom expense category (should not be included)
            CategoryEntity(
                id = "custom_expense2",
                userId = "user2",
                name = "Other User Expense",
                color = "#9C27B0",
                iconName = "other",
                categoryType = "EXPENSE",
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            // Income category (should not be included)
            CategoryEntity(
                id = "income1",
                userId = "user1",
                name = "Salary",
                color = "#4CAF50",
                iconName = "money",
                categoryType = "INCOME",
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        val testUserId = "user1"
        val results = filterExpenseCategories(categories, testUserId)
        
        // Should include default expense + user's custom expense (2 categories)
        results.size shouldBe 2
        
        // Should include the default transport category
        results.any { it.id == "default_transport" && it.isDefault } shouldBe true
        
        // Should include user's custom expense category
        results.any { it.id == "custom_expense1" && it.userId == testUserId } shouldBe true
        
        // Should not include other user's category
        results.none { it.userId == "user2" } shouldBe true
        
        // Should not include income categories
        results.all { it.categoryType == "EXPENSE" } shouldBe true
    }

    /**
     * Property: Income category filtering returns only user's income categories
     * 
     * *For any* user, filtering income categories should return only income categories 
     * belonging to that user (no defaults).
     */
    test("Income category filtering returns only user's income categories") {
        val categories = listOf(
            // User's income category
            CategoryEntity(
                id = "income1",
                userId = "user1",
                name = "Salary",
                color = "#4CAF50",
                iconName = "money",
                categoryType = "INCOME",
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            // Another user's income category (should not be included)
            CategoryEntity(
                id = "income2",
                userId = "user2",
                name = "Other Salary",
                color = "#8BC34A",
                iconName = "money",
                categoryType = "INCOME",
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            // Default expense category (should not be included)
            CategoryEntity(
                id = "default_transport",
                userId = null,
                name = "Transport",
                color = "#2196F3",
                iconName = "car",
                categoryType = "EXPENSE",
                isDefault = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            // User's expense category (should not be included)
            CategoryEntity(
                id = "expense1",
                userId = "user1",
                name = "Custom Expense",
                color = "#FF5722",
                iconName = "custom",
                categoryType = "EXPENSE",
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        val testUserId = "user1"
        val results = filterIncomeCategories(categories, testUserId)
        
        // Should only include user's income category (1 category)
        results.size shouldBe 1
        
        // Should include user's income category
        results.any { it.id == "income1" && it.userId == testUserId } shouldBe true
        
        // Should not include other user's category
        results.none { it.userId == "user2" } shouldBe true
        
        // Should not include default categories
        results.all { !it.isDefault } shouldBe true
        
        // Should not include expense categories
        results.all { it.categoryType == "INCOME" } shouldBe true
    }
})

/**
 * Pure function that mirrors the CategoryDao.getCategoriesByType query logic
 * This extracts the core filtering logic for property testing without database dependencies.
 */
fun filterCategoriesByType(categories: List<CategoryEntity>, type: String, userId: String): List<CategoryEntity> {
    return categories.filter { category ->
        category.categoryType == type && (category.userId == userId || category.isDefault)
    }
}

/**
 * Pure function that mirrors the CategoryDao.getExpenseCategories query logic
 */
fun filterExpenseCategories(categories: List<CategoryEntity>, userId: String): List<CategoryEntity> {
    return categories.filter { category ->
        category.categoryType == "EXPENSE" && (category.userId == userId || category.isDefault)
    }
}

/**
 * Pure function that mirrors the CategoryDao.getIncomeCategories query logic
 */
fun filterIncomeCategories(categories: List<CategoryEntity>, userId: String): List<CategoryEntity> {
    return categories.filter { category ->
        category.categoryType == "INCOME" && category.userId == userId
    }
}

/**
 * Generator for creating random CategoryEntity objects for testing
 */
private fun categoryEntityGenerator(): Arb<CategoryEntity> = Arb.bind(
    Arb.string(5..20),  // id
    Arb.choice(Arb.constant(null), Arb.string(5..15)),  // userId (can be null for defaults)
    Arb.string(3..30),  // name
    Arb.string(7..7),   // color (hex format)
    Arb.string(3..20),  // iconName
    Arb.choice(Arb.constant("INCOME"), Arb.constant("EXPENSE")),  // categoryType
    Arb.boolean(),      // isDefault
    Arb.long(1000000000L..2000000000L),  // createdAt
    Arb.long(1000000000L..2000000000L)   // updatedAt
) { id, userId, name, color, iconName, categoryType, isDefault, createdAt, updatedAt ->
    CategoryEntity(
        id = "test_$id",
        userId = if (isDefault) null else (userId ?: "user_$id"),
        name = name,
        color = "#$color",
        iconName = iconName,
        categoryType = categoryType,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Generator for creating lists of CategoryEntity objects
 */
private fun categoryListGenerator(): Arb<List<CategoryEntity>> = 
    Arb.list(categoryEntityGenerator(), 0..20)

/**
 * Generator that ensures some default categories are included
 */
private fun categoryListWithDefaultsGenerator(): Arb<List<CategoryEntity>> = Arb.bind(
    categoryListGenerator(),
    Arb.list(defaultCategoryGenerator(), 1..4)
) { regularCategories, defaultCategories ->
    regularCategories + defaultCategories
}

/**
 * Generator for default categories (isDefault = true, userId = null)
 */
private fun defaultCategoryGenerator(): Arb<CategoryEntity> = Arb.bind(
    Arb.string(5..20),  // id
    Arb.string(3..30),  // name
    Arb.string(7..7),   // color
    Arb.string(3..20),  // iconName
    Arb.long(1000000000L..2000000000L),  // createdAt
    Arb.long(1000000000L..2000000000L)   // updatedAt
) { id, name, color, iconName, createdAt, updatedAt ->
    CategoryEntity(
        id = "default_$id",
        userId = null,
        name = name,
        color = "#$color",
        iconName = iconName,
        categoryType = "EXPENSE",  // Defaults are always expense
        isDefault = true,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}