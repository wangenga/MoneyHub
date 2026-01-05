#!/bin/bash

echo "🔧 Personal Finance App - Connection Debugging Script"
echo "=================================================="

# Check if device is connected
echo "📱 Checking connected devices..."
adb devices

# Build the app
echo "🔨 Building debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    
    # Install the app
    echo "📲 Installing app..."
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    
    if [ $? -eq 0 ]; then
        echo "✅ App installed successfully!"
        
        # Launch the app
        echo "🚀 Launching app..."
        adb shell am start -n com.finance.app/.MainActivity
        
        echo ""
        echo "📋 Debugging Tips:"
        echo "1. Open the app and go to Settings > Debug > Connection Test"
        echo "2. Run the connection tests to see specific error messages"
        echo "3. Check logcat for detailed error information:"
        echo "   adb logcat | grep -E '(Firebase|Firestore|Auth|Network|FinanceApp)'"
        echo ""
        echo "🔍 Common Issues to Check:"
        echo "• Internet connection on device"
        echo "• Google Play Services installed and updated"
        echo "• Firebase project is active"
        echo "• Firestore and Authentication are enabled in Firebase Console"
        echo "• Certificate hash matches Firebase configuration"
        
    else
        echo "❌ Failed to install app"
    fi
else
    echo "❌ Build failed"
fi