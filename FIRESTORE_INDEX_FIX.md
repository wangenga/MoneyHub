# Firestore Index Fix

## Problem
The app is showing a sync error: "FAILED_PRECONDITION: The query requires an index."

## Solution

### Option 1: Deploy Indexes via Firebase CLI (Recommended)

1. Make sure Firebase CLI is installed:
   ```bash
   npm install -g firebase-tools
   ```

2. Login to Firebase:
   ```bash
   firebase login
   ```

3. Initialize Firebase in your project (if not already done):
   ```bash
   firebase init firestore
   ```
   - Select your project: `financeapp-22b8b`
   - Use `firestore.rules` for rules
   - Use `firestore.indexes.json` for indexes

4. Deploy the indexes:
   ```bash
   ./deploy-firestore-indexes.sh
   ```
   OR
   ```bash
   firebase deploy --only firestore:indexes
   ```

### Option 2: Create Indexes Manually via Firebase Console

Click the link in the error message or visit:
https://console.firebase.google.com/project/financeapp-22b8b/firestore/indexes

Then create these indexes:

1. **budgets collection**:
   - Field: `updatedAt` (Ascending)
   
2. **budgets collection** (composite):
   - Field: `year` (Descending)
   - Field: `month` (Descending)

3. **transactions collection**:
   - Field: `updatedAt` (Descending)

4. **categories collection**:
   - Field: `updatedAt` (Ascending)

5. **recurring_transactions collection**:
   - Field: `updatedAt` (Descending)

### Option 3: Use the Error Link

The easiest way is to click "Retry" in the app, which will show the error again with a direct link to create the specific index that's failing. Click that link and Firebase will auto-create the index for you.

## Index Creation Time

- Indexes can take 5-15 minutes to build
- You'll receive an email when they're ready
- The app will work once all indexes are created

## Google Play Protect Warning

The second error (Google Play Protect) is normal for debug builds. You can:
- Click "Don't send" or "Send this time"
- This won't affect your app functionality
- Production builds signed with a release key won't show this warning
