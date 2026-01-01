#!/bin/bash

# Install debug APK to connected device
# Make sure your device is connected and USB debugging is enabled

echo "Installing debug APK..."

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "ADB not found. Installing..."
    sudo apt update
    sudo apt install -y android-tools-adb
fi

# Check for connected devices
echo "Checking for connected devices..."
adb devices

# Install the APK
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "Installing app-debug.apk..."
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    echo "Installation complete!"
else
    echo "APK not found. Please build the project first with: ./gradlew assembleDebug"
fi