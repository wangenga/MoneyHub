package com.finance.app.data.repository

import com.finance.app.data.local.dao.CategoryDao
import com.finance.app.data.local.entity.CategoryEntity
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType
import com.finance.app.domain.model.DefaultCategoriesProvider
import com.finance.app.domain.sync.SyncScheduler
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Property-based tests for CategoryRepository implementation.
 * 
 * These tests verify the correctness properties defined in the design document
 * for the category system redesign feature.
 */
class CategoryRepositoryPropertyTest : FunSpec({

    /**
     * **Feature: category-system-redesign, Property 1: Expense category query includes defaults and user customs**
     * **Validates: Requirements 1.2, 2.3**
     * 
     * *For any* user with any number of custom expense categories, querying expense categories 
     * for that user should return all 4 default expense categories plus exactly the custom 
     * expense categories belonging to that user.
     */
    test("Property 1: Expense category query includes defaults and user customs") {
        checkAll(100, userIdArb(), customExpenseCategoriesArb()) { userId, customCategories ->
            runBlocking {
                // Setup mocks
                val categoryDao = mockk<CategoryDao>()
                val syncScheduler = mockk<SyncScheduler>()
                val repository = CategoryRepositoryImpl(categoryDao, syncScheduler)
                
                // Create expected entities: defaults + user's custom expense categories
                val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
                val defaultEntities = defaultCategories.map { it.toEntity() }
                val customEntities = customCategories.map { it.toEntity() }
                val allExpenseEntities = defaultEntities + customEntities
                
                // Mock the DAO to return the expected entities
                every { categoryDao.getExpenseCategories(userId) } returns flowOf(allExpenseEntities)
                
                // Execute the query
                val result = repository.getExpenseCategories(userId).first()
                
                // Verify the result contains all defaults
                val defaultIds = defaultCategories.map { it.id }
                val resultIds = result.map { it.id }
                resultIds shouldContainAll defaultIds
                
                // Verify the result contains exactly the user's custom categories
                val customIds = customCategories.map { it.id }
                val customResultIds = result.filter { !it.isDefault }.map { it.id }
                customResultIds shouldContainAll customIds
                
                // Verify total count
                result shouldHaveSize (defaultCategories.size + customCategories.size)
                
                // Verify all returned categories are expense type
                result.forEach { category ->
                    category.categoryType shouldBe CategoryType.EXPENSE
                }
            }
        }
    }

    /**
     * **Feature: category-system-redesign, Property 7: Income category query returns only user's income categories**
     * **Validates: Requirements 3.2**
     * 
     * *For any* user, querying income categories should return only categories where 
     * `categoryType = INCOME` AND `userId = <that user's id>`, with no default categories included.
     */
    test("Property 7: Income category query returns only user's income categories") {
        checkAll(100, userIdArb(), customIncomeCategoriesArb()) { userId, incomeCategories ->
            runBlocking {
                // Setup mocks
                val categoryDao = mockk<CategoryDao>()
                val syncScheduler = mockk<SyncScheduler>()
                val repository = CategoryRepositoryImpl(categoryDao, syncScheduler)
                
                // Create expected entities: only user's income categories (no defaults)
                // Update the categories to have the correct userId
                val userIncomeCategories = incomeCategories.map { it.copy(userId = userId) }
                val incomeEntities = userIncomeCategories.map { it.toEntity() }
                
                // Mock the DAO to return only income categories for this user
                every { categoryDao.getIncomeCategories(userId) } returns flowOf(incomeEntities)
                
                // Execute the query
                val result = repository.getIncomeCategories(userId).first()
                
                // Verify no default categories are included
                result.forEach { category ->
                    category.isDefault shouldBe false
                    category.userId shouldBe userId
                    category.categoryType shouldBe CategoryType.INCOME
                }
                
                // Verify exactly the expected income categories are returned
                val expectedIds = userIncomeCategories.map { it.id }
                val resultIds = result.map { it.id }
                resultIds shouldContainAll expectedIds
                result shouldHaveSize userIncomeCategories.size
            }
        }
    }

    /**
     * **Feature: category-system-redesign, Property 2: Default category deletion protection**
     * **Validates: Requirements 1.4**
     * 
     * *For any* category where `isDefault = true`, attempting to delete that category should fail 
     * and the category should remain in the database unchanged.
     */
    test("Property 2: Default category deletion protection") {
        checkAll(100, defaultCategoryIdArb()) { defaultCategoryId ->
            runBlocking {
                // Setup mocks
                val categoryDao = mockk<CategoryDao>()
                val syncScheduler = mockk<SyncScheduler>()
                val repository = CategoryRepositoryImpl(categoryDao, syncScheduler)
                
                // Mock that no transactions exist for this category
                coEvery { categoryDao.getTransactionCountForCategory(defaultCategoryId) } returns 0
                
                // Mock deleteCustomCategory to return 0 (indicating no rows deleted, i.e., default category)
                coEvery { categoryDao.deleteCustomCategory(defaultCategoryId) } returns 0
                
                // Attempt to delete the default category
                val result = repository.deleteCategory(defaultCategoryId)
                
                // Verify the deletion failed
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<IllegalOperationException>()
                
                // Verify deleteCustomCategory was called (which protects defaults)
                coVerify { categoryDao.deleteCustomCategory(defaultCategoryId) }
                
                // Verify sync was not scheduled for failed deletion
                verify(exactly = 0) { syncScheduler.schedulePostOperationSync() }
            }
        }
    }

    /**
     * **Feature: category-system-redesign, Property 10: Default category edit protection**
     * **Validates: Requirements 5.2**
     * 
     * *For any* category where `isDefault = true`, attempting to update that category should fail 
     * and the category should remain unchanged.
     */
    test("Property 10: Default category edit protection") {
        checkAll(100, defaultCategoryArb()) { defaultCategory ->
            runBlocking {
                // Setup mocks
                val categoryDao = mockk<CategoryDao>()
                val syncScheduler = mockk<SyncScheduler>()
                val repository = CategoryRepositoryImpl(categoryDao, syncScheduler)
                
                // Attempt to update the default category
                val result = repository.updateCategory(defaultCategory)
                
                // Verify the update failed
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<IllegalOperationException>()
                
                // Verify DAO update was never called
                coVerify(exactly = 0) { categoryDao.update(any()) }
                
                // Verify sync was not scheduled for failed update
                verify(exactly = 0) { syncScheduler.schedulePostOperationSync() }
            }
        }
    }

    /**
     * **Feature: category-system-redesign, Property 4: Category validation requires non-empty fields**
     * **Validates: Requirements 2.2**
     * 
     * *For any* category creation attempt where name, color, or iconName is empty or whitespace-only, 
     * the creation should fail with a validation error.
     */
    test("Property 4: Category validation requires non-empty fields") {
        checkAll(100, invalidCategoryArb()) { invalidCategory ->
            runBlocking {
                // Setup mocks
                val categoryDao = mockk<CategoryDao>()
                val syncScheduler = mockk<SyncScheduler>()
                val repository = CategoryRepositoryImpl(categoryDao, syncScheduler)
                
                // Attempt to insert the invalid category
                val result = repository.insertCategory(invalidCategory)
                
                // Verify the insertion failed with validation error
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
                
                // Verify DAO insert was never called
                coVerify(exactly = 0) { categoryDao.insert(any()) }
                
                // Verify sync was not scheduled for failed insertion
                verify(exactly = 0) { syncScheduler.schedulePostOperationSync() }
            }
        }
    }

    /**
     * **Feature: category-system-redesign, Property 3: Custom category user isolation**
     * **Validates: Requirements 2.1, 3.1**
     * 
     * *For any* custom category (where `isDefault = false`) created by user A, querying categories 
     * as user B (where A ≠ B) should not include that category in the results.
     */
    test("Property 3: Custom category user isolation") {
        checkAll(100, userIdArb(), userIdArb(), customCategoryArb()) { userA, userB, customCategory ->
            // Ensure users are different
            if (userA != userB) {
                runBlocking {
                    // Setup mocks
                    val categoryDao = mockk<CategoryDao>()
                    val syncScheduler = mockk<SyncScheduler>()
                    val repository = CategoryRepositoryImpl(categoryDao, syncScheduler)
                    
                    // Create category for user A
                    val categoryForUserA = customCategory.copy(userId = userA, isDefault = false)
                    
                    // Mock DAO to return only default categories for user B (no custom categories)
                    val defaultCategories = DefaultCategoriesProvider.getDefaultExpenseCategories()
                    val defaultEntities = defaultCategories.map { it.toEntity() }
                    every { categoryDao.getAllCategoriesForUser(userB) } returns flowOf(defaultEntities)
                    
                    // Query categories as user B
                    val categoriesForUserB = repository.getAllCategories(userB).first()
                    
                    // Verify user B cannot see user A's custom category
                    categoriesForUserB.none { it.id == categoryForUserA.id } shouldBe true
                    categoriesForUserB.none { it.userId == userA } shouldBe true
                    
                    // Verify user B only sees default categories (userId = null)
                    categoriesForUserB.all { it.isDefault || it.userId == userB } shouldBe true
                }
            }
        }
    }

    /**
     * **Feature: category-system-redesign, Property 6: User isolation with identical names**
     * **Validates: Requirements 2.5**
     * 
     * *For any* two users A and B who each create a custom category with the same name, 
     * both categories should exist independently in the database with their respective user identifiers.
     */
    test("Property 6: User isolation with identical names") {
        checkAll(100, userIdArb(), userIdArb(), Arb.string(1..50, Arb.alphanumeric())) { userA, userB, categoryName ->
            // Ensure users are different
            if (userA != userB) {
                runBlocking {
                    // Setup mocks
                    val categoryDao = mockk<CategoryDao>()
                    val syncScheduler = mockk<SyncScheduler>()
                    val repository = CategoryRepositoryImpl(categoryDao, syncScheduler)
                    
                    // Create categories with identical names for both users
                    val currentTime = System.currentTimeMillis()
                    val categoryA = Category(
                        id = "category_a_${userA}",
                        userId = userA,
                        name = categoryName,
                        color = "#FF0000",
                        iconName = "test_icon",
                        categoryType = CategoryType.EXPENSE,
                        isDefault = false,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                    val categoryB = Category(
                        id = "category_b_${userB}",
                        userId = userB,
                        name = categoryName,
                        color = "#00FF00",
                        iconName = "test_icon",
                        categoryType = CategoryType.EXPENSE,
                        isDefault = false,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                    
                    // Mock successful insertions
                    coEvery { categoryDao.insert(any()) } returns Unit
                    every { syncScheduler.schedulePostOperationSync() } returns Unit
                    
                    // Insert both categories
                    val resultA = repository.insertCategory(categoryA)
                    val resultB = repository.insertCategory(categoryB)
                    
                    // Both insertions should succeed
                    resultA.isSuccess shouldBe true
                    resultB.isSuccess shouldBe true
                    
                    // Verify both categories were inserted with their respective user IDs
                    coVerify { categoryDao.insert(match { it.userId == userA && it.name == categoryName }) }
                    coVerify { categoryDao.insert(match { it.userId == userB && it.name == categoryName }) }
                    
                    // Verify sync was scheduled for both insertions
                    verify(exactly = 2) { syncScheduler.schedulePostOperationSync() }
                }
            }
        }
    }
})

// Arbitraries for generating test data

private fun userIdArb(): Arb<String> = Arb.string(5..20, Arb.alphanumeric())

private fun customExpenseCategoriesArb(): Arb<List<Category>> = 
    Arb.list(categoryArb(CategoryType.EXPENSE, isDefault = false), 0..10)

private fun customIncomeCategoriesArb(): Arb<List<Category>> = 
    Arb.list(categoryArb(CategoryType.INCOME, isDefault = false), 0..10)

private fun defaultCategoryIdArb(): Arb<String> = Arb.choice(
    Arb.constant("default_transport"),
    Arb.constant("default_rent"),
    Arb.constant("default_groceries"),
    Arb.constant("default_entertainment")
)

private fun defaultCategoryArb(): Arb<Category> = Arb.bind(
    defaultCategoryIdArb(),
    Arb.string(1..50, Arb.alphanumeric()),
    Arb.string(6, Arb.alphanumeric()).map { "#$it" },
    Arb.string(1..20, Arb.alphanumeric())
) { id, name, color, iconName ->
    Category(
        id = id,
        userId = null,
        name = name,
        color = color,
        iconName = iconName,
        categoryType = CategoryType.EXPENSE,
        isDefault = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

private fun categoryArb(
    categoryType: CategoryType,
    isDefault: Boolean = false
): Arb<Category> = Arb.bind(
    Arb.string(5..20, Arb.alphanumeric()),
    if (isDefault) Arb.constant(null) else Arb.string(5..20, Arb.alphanumeric()),
    Arb.string(1..50, Arb.alphanumeric()),
    Arb.string(6, Arb.alphanumeric()).map { "#$it" },
    Arb.string(1..20, Arb.alphanumeric())
) { id, userId, name, color, iconName ->
    Category(
        id = id,
        userId = userId,
        name = name,
        color = color,
        iconName = iconName,
        categoryType = categoryType,
        isDefault = isDefault,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

private fun invalidCategoryArb(): Arb<Category> = Arb.choice(
    // Category with empty name
    categoryArb(CategoryType.EXPENSE).map { it.copy(name = "") },
    // Category with whitespace-only name
    categoryArb(CategoryType.EXPENSE).map { it.copy(name = "   ") },
    // Category with empty color
    categoryArb(CategoryType.EXPENSE).map { it.copy(color = "") },
    // Category with whitespace-only color
    categoryArb(CategoryType.EXPENSE).map { it.copy(color = "  ") },
    // Category with empty iconName
    categoryArb(CategoryType.EXPENSE).map { it.copy(iconName = "") },
    // Category with whitespace-only iconName
    categoryArb(CategoryType.EXPENSE).map { it.copy(iconName = "   ") }
)

private fun customCategoryArb(): Arb<Category> = Arb.choice(
    categoryArb(CategoryType.EXPENSE, isDefault = false),
    categoryArb(CategoryType.INCOME, isDefault = false)
)

// Extension function to convert Category to CategoryEntity
private fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    userId = userId,
    name = name,
    color = color,
    iconName = iconName,
    categoryType = categoryType.name,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt
)