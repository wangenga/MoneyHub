package com.finance.app.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll

/**
 * Property-based tests for CategoryType enum constraint.
 * 
 * These tests verify the correctness properties defined in the design document
 * for the category system redesign feature.
 */
class CategoryTypePropertyTest : FunSpec({

    /**
     * **Feature: category-system-redesign, Property 13: CategoryType enum constraint**
     * **Validates: Requirements 7.1**
     * 
     * *For any* category stored in the database, the `categoryType` field must be exactly 
     * one of: "INCOME" or "EXPENSE".
     * 
     * This test verifies that:
     * 1. The CategoryType enum has exactly two values
     * 2. Those values are INCOME and EXPENSE
     * 3. Any randomly selected CategoryType is always one of these two valid values
     */
    test("Property 13: CategoryType enum constraint - categoryType must be INCOME or EXPENSE") {
        // Verify the enum has exactly the expected values
        val allValues = CategoryType.entries
        allValues.size shouldBe 2
        allValues shouldContainExactlyInAnyOrder listOf(CategoryType.INCOME, CategoryType.EXPENSE)
        
        // Property test: For any CategoryType value, it must be either INCOME or EXPENSE
        checkAll(100, Arb.enum<CategoryType>()) { categoryType ->
            val isValidType = categoryType == CategoryType.INCOME || categoryType == CategoryType.EXPENSE
            isValidType shouldBe true
        }
    }

    /**
     * Additional property: CategoryType string representation is consistent
     * 
     * *For any* CategoryType, converting to string and back should preserve the value.
     * This ensures the enum can be safely serialized/deserialized.
     */
    test("CategoryType string representation round-trip is consistent") {
        checkAll(100, Arb.enum<CategoryType>()) { categoryType ->
            val stringValue = categoryType.name
            val parsedBack = CategoryType.valueOf(stringValue)
            parsedBack shouldBe categoryType
        }
    }

    /**
     * Property: CategoryType string values match expected database values
     * 
     * *For any* CategoryType, its string name should be exactly "INCOME" or "EXPENSE"
     * as these are the values stored in the database.
     */
    test("CategoryType string names match expected database values") {
        checkAll(100, Arb.enum<CategoryType>()) { categoryType ->
            val stringValue = categoryType.name
            val isValidString = stringValue == "INCOME" || stringValue == "EXPENSE"
            isValidString shouldBe true
        }
    }
})
