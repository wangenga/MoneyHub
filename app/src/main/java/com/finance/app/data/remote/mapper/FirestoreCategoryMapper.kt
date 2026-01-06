package com.finance.app.data.remote.mapper

import com.finance.app.data.remote.model.FirestoreCategory
import com.finance.app.data.remote.util.TimestampUtils
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.CategoryType

/**
 * Mapper between domain Category and Firestore Category models
 */
object FirestoreCategoryMapper {
    
    /**
     * Convert domain Category to Firestore Category
     */
    fun toFirestore(category: Category): FirestoreCategory {
        return FirestoreCategory(
            id = category.id,
            name = category.name,
            color = category.color,
            iconName = category.iconName,
            isDefault = category.isDefault,
            createdAt = TimestampUtils.longToTimestamp(category.createdAt),
            updatedAt = TimestampUtils.longToTimestamp(category.updatedAt)
        )
    }
    
    /**
     * Convert Firestore Category to domain Category
     * Note: userId is not stored in Firestore for categories as they can be user-specific or default
     * Note: categoryType will be fully mapped in task 3 when FirestoreCategory is updated
     */
    fun toDomain(firestoreCategory: FirestoreCategory, userId: String?): Category {
        return Category(
            id = firestoreCategory.id,
            userId = userId,
            name = firestoreCategory.name,
            color = firestoreCategory.color,
            iconName = firestoreCategory.iconName,
            categoryType = CategoryType.EXPENSE, // Default for backward compatibility until Firestore model is updated
            isDefault = firestoreCategory.isDefault,
            createdAt = TimestampUtils.timestampToLong(firestoreCategory.createdAt),
            updatedAt = TimestampUtils.timestampToLong(firestoreCategory.updatedAt)
        )
    }
}