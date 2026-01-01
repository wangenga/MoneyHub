#!/bin/bash

# Install debug APK to connected device
# Make sure your device is connected and USB debugging is enabled

echo "Installing debug APK..."

# Set JAVA_HOME for consistent builds
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "ADB not found. Installing..."
    sudo apt update
    sudo apt install -y adb
fi

# Check for connected devices
echo "Checking for connected devices..."
adb devices

# Build the APK if it doesn't exist or is outdated
if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "APK not found. Building..."
    ./gradlew assembleDebug
fi

# Install the APK
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "Installing app-debug.apk..."
    # Try to uninstall first to avoid signature conflicts
    adb uninstall com.finance.app 2>/dev/null || true
    adb install app/build/outputs/apk/debug/app-debug.apk
    echo "Installation complete!"
else
    echo "APK not found. Build failed."
    exit 1
fi