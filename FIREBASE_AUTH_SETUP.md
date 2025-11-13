# Firebase Authentication Setup Guide

This document provides detailed instructions for configuring Firebase Authentication for the Personal Finance Management App.

## Firebase Console Configuration

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project" or select an existing project
3. Follow the setup wizard to create your project

### 2. Add Android App
1. In the Firebase Console, click the Android icon to add an Android app
2. Enter the package name: `com.finance.app`
3. (Optional) Enter app nickname: "Personal Finance App"
4. (Optional) Enter SHA-1 certificate fingerprint for Google Sign-In
   - For debug builds, get SHA-1 from Android Studio or using:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
5. Download the `google-services.json` file
6. Replace `app/google-services.json` with the downloaded file

### 3. Enable Authentication Methods

#### Email/Password Authentication
1. In Firebase Console, go to **Authentication** > **Sign-in method**
2. Click on **Email/Password**
3. Enable the **Email/Password** toggle
4. Click **Save**

#### Google Sign-In Authentication
1. In Firebase Console, go to **Authentication** > **Sign-in method**
2. Click on **Google**
3. Enable the **Google** toggle
4. Select a support email for your project
5. Click **Save**

### 4. Configure OAuth Consent Screen (for Google Sign-In)
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your Firebase project
3. Navigate to **APIs & Services** > **OAuth consent screen**
4. Configure the consent screen with your app information
5. Add test users if needed during development

### 5. Get Web Client ID (for Google Sign-In)
1. In Firebase Console, go to **Project Settings** > **General**
2. Scroll down to **Your apps** section
3. Find the **Web client ID** under OAuth 2.0 Client IDs
4. Copy this ID - you'll need it in your Android app for Google Sign-In

## Android App Configuration

### 1. Update google-services.json
Replace the placeholder file at `app/google-services.json` with your actual Firebase configuration file.

### 2. Google Sign-In Integration
When implementing the Google Sign-In UI (in a future task), you'll need to:

1. Add the Web Client ID to your sign-in configuration:
```kotlin
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken("YOUR_WEB_CLIENT_ID_HERE")
    .requestEmail()
    .build()
```

2. The Web Client ID can be found in your `google-services.json` file under:
   - `client[0].oauth_client[].client_id` where `client_type` is 3

### 3. SHA-1 Certificate Fingerprint
For Google Sign-In to work, you need to add your SHA-1 certificate fingerprint:

**Debug Certificate:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Release Certificate:**
```bash
keytool -list -v -keystore /path/to/your/keystore.jks -alias your-key-alias
```

Add the SHA-1 fingerprint in Firebase Console:
1. Go to **Project Settings** > **Your apps**
2. Select your Android app
3. Click **Add fingerprint**
4. Paste the SHA-1 certificate fingerprint

## Testing Authentication

### Test Email/Password Authentication
1. Run the app
2. Navigate to the registration screen
3. Enter a test email and password
4. Verify the user is created in Firebase Console under **Authentication** > **Users**

### Test Google Sign-In
1. Run the app
2. Click the Google Sign-In button
3. Select a Google account
4. Verify the user is created in Firebase Console under **Authentication** > **Users**

## Security Considerations

### Firestore Security Rules
When you set up Firestore (in a future task), ensure you have proper security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      match /transactions/{transactionId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      match /categories/{categoryId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

### Password Requirements
Firebase Authentication enforces a minimum password length of 6 characters by default. Consider implementing additional client-side validation for stronger passwords.

## Troubleshooting

### Common Issues

**Issue: Google Sign-In fails with "Developer Error"**
- Solution: Verify SHA-1 certificate fingerprint is added to Firebase Console
- Solution: Ensure Web Client ID is correctly configured

**Issue: Email/Password sign-in fails**
- Solution: Verify Email/Password authentication is enabled in Firebase Console
- Solution: Check that the email format is valid

**Issue: "google-services.json not found" error**
- Solution: Ensure `google-services.json` is placed in the `app/` directory
- Solution: Sync Gradle files after adding the file

**Issue: Network errors during authentication**
- Solution: Verify device has internet connectivity
- Solution: Check Firebase project status in Firebase Console

## Additional Resources

- [Firebase Authentication Documentation](https://firebase.google.com/docs/auth)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)

## Implementation Status

✅ Task 3 Complete: Firebase Authentication module implemented
- AuthRepository interface defined
- AuthRepositoryImpl with Firebase Auth integration
- Email/Password sign-in functionality
- Email/Password registration functionality
- Google Sign-In integration
- Authentication state Flow
- Sign-out functionality
- Hilt dependency injection configured

**Next Steps:**
- Task 4: Implement biometric authentication
- Task 5: Build authentication UI screens
