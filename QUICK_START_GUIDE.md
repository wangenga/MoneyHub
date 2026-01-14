# Quick Start Guide - Finance App

## ✅ Current Status

Your app is now **fully installed and running** on your Pixel 8a device!

### What's Been Done

1. ✅ **App Built Successfully** - Clean build with no errors
2. ✅ **App Installed** - Installed on device (Pixel 8a - Android 16)
3. ✅ **App Launched** - Currently running on your device
4. ✅ **Firestore Indexes Deployed** - All required database indexes are now live

## 🚀 Quick Commands

### Build and Install
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or install directly
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Launch App
```bash
# Launch the app
adb shell am start -n com.finance.app/.MainActivity
```

### Check Device Connection
```bash
# List connected devices
adb devices

# Check if app is installed
adb shell pm path com.finance.app
```

### Uninstall (if needed)
```bash
adb uninstall com.finance.app
```

## 📱 App Features

Your finance app includes:

- **Authentication**: Email/Password and Google Sign-In
- **Biometric Lock**: Fingerprint/Face authentication
- **Transactions**: Add, edit, delete income/expense transactions
- **Categories**: Custom categories with colors and icons
- **Budgets**: Set and track monthly budgets per category
- **Recurring Transactions**: Automated recurring income/expenses
- **Analytics**: Charts and insights on spending patterns
- **Cloud Sync**: Real-time sync with Firebase Firestore
- **Offline Support**: Works offline with local SQLite database
- **Encryption**: Database encryption with SQLCipher
- **Dark Mode**: Dynamic theming support

## 🔧 Troubleshooting

### Google Play Protect Warning
- This is normal for debug builds
- Click "Don't send" - it won't affect functionality
- Production builds won't show this warning

### Sync Issues
- All Firestore indexes are now deployed
- Sync should work immediately
- If you see index errors, wait 5-10 minutes for indexes to build

### App Not Launching
```bash
# Check logcat for errors
adb logcat | grep -i finance

# Clear app data and restart
adb shell pm clear com.finance.app
adb shell am start -n com.finance.app/.MainActivity
```

### Build Errors
```bash
# Clean and rebuild
./gradlew clean build

# Check for dependency issues
./gradlew dependencies
```

## 📊 Firebase Console

- **Project**: financeapp-22b8b
- **Console**: https://console.firebase.google.com/project/financeapp-22b8b/overview
- **Firestore**: https://console.firebase.google.com/project/financeapp-22b8b/firestore
- **Authentication**: https://console.firebase.google.com/project/financeapp-22b8b/authentication

## 🎯 Next Steps

1. **Create an Account**: Open the app and register with email/password
2. **Add Categories**: Set up your expense and income categories
3. **Add Transactions**: Start tracking your finances
4. **Set Budgets**: Create monthly budgets for each category
5. **Enable Biometric**: Set up fingerprint/face unlock in settings
6. **Test Sync**: Sign in on another device to test cloud sync

## 📝 Development Workflow

```bash
# Make code changes, then:
./gradlew installDebug && adb shell am start -n com.finance.app/.MainActivity

# Or use the convenience script:
./install-debug.sh
```

## 🔐 Security Features

- **Encrypted Database**: SQLCipher encryption for local data
- **Secure Storage**: Encrypted SharedPreferences for sensitive data
- **Biometric Auth**: Hardware-backed biometric authentication
- **Firebase Auth**: Secure cloud authentication
- **Firestore Rules**: Server-side security rules enforced

## 📦 App Details

- **Package**: com.finance.app
- **Version**: 1.0 (versionCode 1)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 33 (Android 13)
- **Build Type**: Debug

Enjoy your fully working finance app! 🎉
