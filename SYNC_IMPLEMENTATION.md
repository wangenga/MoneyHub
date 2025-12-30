# Synchronization Implementation Documentation

## Overview

This document describes the synchronization logic implementation for the Personal Finance Management App. The synchronization system ensures data consistency between the local Room database and Firebase Firestore.

## Architecture

### Components

1. **SyncRepository Interface** (`domain/repository/SyncRepository.kt`)
   - Defines the contract for synchronization operations
   - Provides methods for syncing transactions, categories, and observing sync status

2. **SyncRepositoryImpl** (`data/repository/SyncRepositoryImpl.kt`)
   - Implements the synchronization logic with conflict resolution
   - Handles exponential backoff retry strategy
   - Manages sync state and timestamps

3. **SyncDataUseCase** (`domain/usecase/SyncDataUseCase.kt`)
   - Provides a clean interface for UI layer to trigger sync operations
   - Encapsulates business logic for different sync scenarios

## Synchronization Strategy

### Conflict Resolution
- **Last-Write-Wins**: Uses timestamp comparison (`updatedAt` field) to resolve conflicts
- Remote data with newer timestamps overwrites local data
- Local data with newer or equal timestamps is preserved (already uploaded)

### Sync Process

#### Transaction Synchronization
1. **Upload Phase**: Upload all local transactions with `PENDING` sync status to Firestore
2. **Mark as Synced**: Update local transactions to `SYNCED` status after successful upload
3. **Download Phase**: Fetch remote transactions updated after last sync timestamp
4. **Conflict Resolution**: Compare timestamps and update local data if remote is newer
5. **Insert New**: Add new remote transactions not present locally

#### Category Synchronization
1. **Upload Phase**: Upload all user categories to Firestore
2. **Download Phase**: Fetch remote categories updated after last sync timestamp
3. **Conflict Resolution**: Compare timestamps and update local data if remote is newer
4. **Insert New**: Add new remote categories not present locally

### Retry Strategy
- **Exponential Backoff**: Base delay of 1 second, doubles with each retry
- **Maximum Attempts**: 3 retry attempts before failing
- **Maximum Delay**: Capped at 30 seconds
- **Delay Formula**: `min(1000ms * 2^attempt, 30000ms)`

## Sync States

```kotlin
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val timestamp: Long) : SyncState()
    data class Error(val message: String, val retryCount: Int = 0) : SyncState()
}
```

## Usage Examples

### Basic Synchronization
```kotlin
@Inject
lateinit var syncDataUseCase: SyncDataUseCase

// Sync all data
val result = syncDataUseCase.syncAll()

// Force full sync (ignores last sync timestamp)
val result = syncDataUseCase.forceSyncAll()

// Sync only transactions
val result = syncDataUseCase.syncTransactions()
```

### Observing Sync Status
```kotlin
syncDataUseCase.observeSyncStatus().collect { state ->
    when (state) {
        is SyncState.Idle -> // Show idle state
        is SyncState.Syncing -> // Show loading indicator
        is SyncState.Success -> // Show success message
        is SyncState.Error -> // Show error message
    }
}
```

### Check Last Sync Time
```kotlin
val lastSync = syncDataUseCase.getLastSyncTimestamp()
if (lastSync != null) {
    val lastSyncDate = Date(lastSync)
    // Display last sync time to user
}
```

## Data Flow

### Local to Remote (Upload)
1. Query local database for transactions with `SyncStatus.PENDING`
2. Batch upload to Firestore using `FirestoreDataSource.saveTransactionsBatch()`
3. Update local records to `SyncStatus.SYNCED` on success
4. Upload user categories (all categories for the user)

### Remote to Local (Download)
1. Query Firestore for data updated after `lastSyncTimestamp`
2. For each remote record:
   - If not exists locally: Insert new record
   - If exists locally: Compare `updatedAt` timestamps
   - If remote is newer: Update local record
   - If local is newer/equal: Keep local record

## Error Handling

### Network Errors
- Automatic retry with exponential backoff
- Sync state updated to show error with retry count
- User can manually retry sync

### Authentication Errors
- Sync fails immediately if user not authenticated
- Error message indicates authentication required

### Data Validation Errors
- Firestore security rules validate data format
- Invalid data uploads are rejected
- Error messages help identify validation issues

## Performance Considerations

### Incremental Sync
- Only syncs data modified since last successful sync
- Uses `updatedAt` timestamp for efficient querying
- Reduces network usage and sync time

### Batch Operations
- Uses Firestore batch writes for multiple records
- Maximum 500 operations per batch (Firestore limit)
- Reduces number of network requests

### Local Caching
- Stores last sync timestamp in SharedPreferences
- Avoids unnecessary full syncs
- Enables offline-first architecture

## Security

### Authentication
- All sync operations require authenticated user
- User can only sync their own data (enforced by Firestore rules)

### Data Validation
- Firestore security rules validate data structure
- Prevents malicious or malformed data uploads
- Ensures data integrity across devices

## Integration Points

### Repository Layer
- `TransactionRepositoryImpl`: Marks transactions as `PENDING` on create/update/delete
- `CategoryRepositoryImpl`: Categories are synced without explicit sync status

### UI Layer
- Use `SyncDataUseCase` to trigger sync operations
- Observe `SyncState` to show sync progress to users
- Display last sync timestamp in settings

### Background Sync
- Ready for integration with WorkManager (Task 14)
- Sync operations are suspend functions suitable for background execution

## Testing Considerations

### Unit Tests
- Mock `FirestoreDataSource` for repository tests
- Test conflict resolution logic with different timestamp scenarios
- Test retry mechanism with simulated failures

### Integration Tests
- Test with real Firestore emulator
- Verify data consistency after sync operations
- Test network failure scenarios

## Future Enhancements

### Delta Sync
- Track specific field changes for more efficient sync
- Reduce bandwidth usage for large datasets

### Conflict Resolution Options
- Allow user to choose conflict resolution strategy
- Implement merge strategies for specific data types

### Sync Scheduling
- Intelligent sync scheduling based on user activity
- Adaptive sync frequency based on data change patterns