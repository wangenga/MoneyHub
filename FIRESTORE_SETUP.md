# Firestore Setup Documentation

## Overview

This document describes the Firestore data structure and security configuration for the Personal Finance Management App.

## Data Structure

### Collections Hierarchy

```
users/{userId}
├── email: String
├── displayName: String?
├── photoUrl: String?
├── createdAt: Timestamp
└── lastLoginAt: Timestamp

users/{userId}/transactions/{transactionId}
├── type: String ("INCOME" | "EXPENSE")
├── amount: Number (positive)
├── categoryId: String
├── date: Timestamp
├── paymentMethod: String? (optional)
├── notes: String? (optional)
├── createdAt: Timestamp
└── updatedAt: Timestamp

users/{userId}/categories/{categoryId}
├── name: String (1-50 characters)
├── color: String (hex format: #RRGGBB)
├── iconName: String
├── isDefault: Boolean
├── createdAt: Timestamp
└── updatedAt: Timestamp
```

## Security Rules

The Firestore security rules (`firestore.rules`) implement the following security model:

### Authentication Requirements
- All operations require authenticated users (`request.auth != null`)
- Users can only access their own data (`request.auth.uid == userId`)

### Data Validation
- **Transactions**: Validates required fields, data types, and business rules (positive amounts, valid transaction types)
- **Categories**: Validates required fields, name length (1-50 chars), hex color format, and data types

### Access Patterns
- **Read**: Users can read their own user document, transactions, and categories
- **Write**: Users can create, update, and delete their own data
- **Batch Operations**: Supported through the FirestoreDataSource batch methods

## Data Models

### Firestore Models
- `FirestoreUser`: Maps to domain `User` model
- `FirestoreTransaction`: Maps to domain `Transaction` model  
- `FirestoreCategory`: Maps to domain `Category` model

### Key Differences from Domain Models
- **Timestamps**: Firestore uses `Timestamp` objects, domain uses `Long` (milliseconds)
- **Enums**: Firestore stores enums as strings, domain uses enum types
- **User Context**: `userId` and `syncStatus` are not stored in Firestore (local concerns)

## Deployment

### Security Rules Deployment
```bash
firebase deploy --only firestore:rules
```

### Required Firebase Configuration
1. Enable Firestore in Firebase Console
2. Set up authentication providers (Email/Password, Google)
3. Deploy security rules using Firebase CLI
4. Ensure `google-services.json` is in the `app/` directory

## Usage Examples

### Initialize Firestore Data Source
```kotlin
@Inject
lateinit var firestoreDataSource: FirestoreDataSource
```

### Save Transaction
```kotlin
val result = firestoreDataSource.saveTransaction(userId, transaction)
```

### Batch Operations
```kotlin
val result = firestoreDataSource.saveTransactionsBatch(userId, transactions)
```

## Performance Considerations

- **Indexing**: Firestore automatically indexes on `updatedAt` for incremental sync
- **Batch Size**: Limited to 500 operations per batch
- **Query Limits**: Consider pagination for large datasets
- **Offline Support**: Firestore SDK provides automatic offline caching

## Error Handling

All FirestoreDataSource methods return `Result<T>` types:
- `Result.success(data)` for successful operations
- `Result.failure(exception)` for errors

Common error scenarios:
- Network connectivity issues
- Permission denied (security rules)
- Invalid data format
- Firestore service limits exceeded