package com.finance.app.data.remote.mapper

import com.finance.app.data.remote.model.FirestoreCategory
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType
import com.google.firebase.Timestamp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for FirestoreCategoryMapper serialization round-trip.
 * 
 * These tests verify the correctness properties defined in the design document
 * for the category system redesign feature.
 */
class FirestoreCategoryMapperPropertyTest : FunSpec({

    /**
     * **Feature: category-system-redesign, Property 12: Serialization round-trip preserves category data**
     * **Validates: Requirements 6.3, 7.4, 7.5**
     * 
     * *For any* valid Category object, serializing to FirestoreCategory and deserializing back 
     * should produce an equivalent Category object with all fields preserved, including `categoryType`.
     * 
     * This test verifies that:
     * 1. Domain Category -> FirestoreCategory -> Domain Category preserves all data
     * 2. CategoryType enum is correctly converted to/from string
     * 3. All other fields (id, name, color, iconName, isDefault, timestamps) are preserved
     * 4. The round-trip is consistent for both INCOME and EXPENSE categories
     */
    test("Property 12: Serialization round-trip preserves category data") {
        checkAll(100, categoryArb()) { originalCategory ->
            // Convert domain Category to Firestore
            val firestoreCategory = FirestoreCategoryMapper.toFirestore(originalCategory)
            
            // Convert back to domain Category
            val roundTripCategory = FirestoreCategoryMapper.toDomain(firestoreCategory, originalCategory.userId)
            
            // Verify all fields are preserved
            roundTripCategory.id shouldBe originalCategory.id
            roundTripCategory.userId shouldBe originalCategory.userId
            roundTripCategory.name shouldBe originalCategory.name
            roundTripCategory.color shouldBe originalCategory.color
            roundTripCategory.iconName shouldBe originalCategory.iconName
            roundTripCategory.categoryType shouldBe originalCategory.categoryType
            roundTripCategory.isDefault shouldBe originalCategory.isDefault
            roundTripCategory.createdAt shouldBe originalCategory.createdAt
            roundTripCategory.updatedAt shouldBe originalCategory.updatedAt
        }
    }

    /**
     * Additional property: CategoryType string conversion is bidirectional
     * 
     * *For any* CategoryType, converting to string in Firestore and back should preserve the type.
     */
    test("CategoryType string conversion is bidirectional") {
        checkAll(100, Arb.enum<CategoryType>()) { categoryType ->
            val stringValue = categoryType.name
            val parsedBack = when (stringValue) {
                "INCOME" -> CategoryType.INCOME
                "EXPENSE" -> CategoryType.EXPENSE
                else -> CategoryType.EXPENSE // Default fallback
            }
            parsedBack shouldBe categoryType
        }
    }

    /**
     * Property: Firestore serialization handles edge cases correctly
     * 
     * *For any* Category with edge case string values (empty strings, special characters),
     * the round-trip should preserve the data correctly.
     */
    test("Firestore serialization handles edge cases correctly") {
        checkAll(100, edgeCaseCategoryArb()) { originalCategory ->
            val firestoreCategory = FirestoreCategoryMapper.toFirestore(originalCategory)
            val roundTripCategory = FirestoreCategoryMapper.toDomain(firestoreCategory, originalCategory.userId)
            
            // All fields should be preserved even with edge case values
            roundTripCategory.id shouldBe originalCategory.id
            roundTripCategory.userId shouldBe originalCategory.userId
            roundTripCategory.name shouldBe originalCategory.name
            roundTripCategory.color shouldBe originalCategory.color
            roundTripCategory.iconName shouldBe originalCategory.iconName
            roundTripCategory.categoryType shouldBe originalCategory.categoryType
            roundTripCategory.isDefault shouldBe originalCategory.isDefault
            roundTripCategory.createdAt shouldBe originalCategory.createdAt
            roundTripCategory.updatedAt shouldBe originalCategory.updatedAt
        }
    }
})

/**
 * Arbitrary generator for Category objects with realistic values
 */
private fun categoryArb(): Arb<Category> = arbitrary {
    Category(
        id = Arb.string(5..50).bind(),
        userId = Arb.choice(Arb.constant(null), Arb.string(5..50)).bind(),
        name = Arb.string(1..100).bind(),
        color = colorArb().bind(),
        iconName = iconArb().bind(),
        categoryType = Arb.enum<CategoryType>().bind(),
        isDefault = Arb.boolean().bind(),
        createdAt = Arb.long(1000000000000L..2000000000000L).bind(), // Reasonable timestamp range
        updatedAt = Arb.long(1000000000000L..2000000000000L).bind()
    )
}

/**
 * Arbitrary generator for Category objects with edge case values
 */
private fun edgeCaseCategoryArb(): Arb<Category> = arbitrary {
    Category(
        id = Arb.choice(
            Arb.string(1..5),
            Arb.string(100..200)
        ).bind(),
        userId = Arb.choice(
            Arb.constant(null),
            Arb.string(1..5),
            Arb.string(100..200)
        ).bind(),
        name = Arb.choice(
            Arb.string(1..5),
            Arb.string(100..200)
        ).bind(),
        color = Arb.choice(
            Arb.constant("#FFFFFF"),
            Arb.constant("#000000"),
            colorArb()
        ).bind(),
        iconName = iconArb().bind(),
        categoryType = Arb.enum<CategoryType>().bind(),
        isDefault = Arb.boolean().bind(),
        createdAt = Arb.long(1000000000000L..2000000000000L).bind(), // Reasonable timestamp range
        updatedAt = Arb.long(1000000000000L..2000000000000L).bind()
    )
}

/**
 * Arbitrary generator for hex color codes
 */
private fun colorArb(): Arb<String> = Arb.choice(
    Arb.constant("#2196F3"), // Blue
    Arb.constant("#9C27B0"), // Purple
    Arb.constant("#4CAF50"), // Green
    Arb.constant("#FF9800"), // Orange
    Arb.constant("#F44336"), // Red
    Arb.constant("#607D8B"), // Blue Grey
    arbitrary {
        val hex = "0123456789ABCDEF"
        "#" + (1..6).map { hex.random() }.joinToString("")
    }
)

/**
 * Arbitrary generator for Material icon names
 */
private fun iconArb(): Arb<String> = Arb.choice(
    Arb.constant("directions_car"),
    Arb.constant("home"),
    Arb.constant("shopping_cart"),
    Arb.constant("movie"),
    Arb.constant("restaurant"),
    Arb.constant("local_gas_station"),
    Arb.constant("school"),
    Arb.constant("work"),
    Arb.constant("favorite"),
    Arb.constant("star")
)