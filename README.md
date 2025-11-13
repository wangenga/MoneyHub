# Personal Finance Management App

An Android application for tracking income, expenses, and budgets with insightful analytics and secure cloud synchronization.

## Project Structure

This project follows clean architecture principles with the following layers:

### UI Layer (`ui/`)
- Jetpack Compose screens and components
- ViewModels for state management
- Material 3 theming

### Domain Layer (`domain/`)
- Business logic and use cases
- Domain models
- Repository interfaces

### Data Layer (`data/`)
- Repository implementations
- Local data sources (Room)
- Remote data sources (Firebase)
- Data mappers

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material Design 3
- **Local Database**: Room with SQLCipher encryption
- **Cloud Database**: Firebase Firestore
- **Authentication**: Firebase Auth
- **Dependency Injection**: Hilt
- **Background Work**: WorkManager
- **Charts**: Vico
- **Biometric Auth**: AndroidX Biometric

## Setup Instructions

### Prerequisites
1. Android Studio Hedgehog or later
2. JDK 17
3. Android SDK 34

### Firebase Configuration
1. Create a Firebase project at https://console.firebase.google.com
2. Add an Android app with package name `com.finance.app`
3. Download the `google-services.json` file
4. Replace the placeholder `app/google-services.json` with your actual configuration file
5. Enable Firebase Authentication (Email/Password and Google Sign-In)
6. Enable Cloud Firestore

### Build and Run
1. Clone the repository
2. Open the project in Android Studio
3. Replace the placeholder `google-services.json` with your actual Firebase configuration
4. Sync Gradle files
5. Build and run on an emulator or physical device

## Project Status

This project is currently in development.
