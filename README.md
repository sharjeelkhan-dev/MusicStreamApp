# Student Attendance & Analytics App

A modern, high-performance Android application designed to streamline student attendance management. Built with an offline-first approach, the app ensures that attendance can be marked anytime, anywhere, while providing seamless cloud synchronization for data durability.

## 🚀 Key Features

*   **Offline-First Experience**: Mark attendance without an internet connection using local Room Database storage.
*   **Real-time Cloud Sync**: Automatic and manual synchronization with Firebase Firestore to ensure data is never lost.
*   **Biometric Security**: Protect sensitive student data with Fingerprint and Face Unlock integration.
*   **Comprehensive Analytics**: Visualize student performance through attendance percentages, recent session history, and detailed reports.
*   **Class Management**: Easily create, update, and manage multiple classes and sections.
*   **Smart Imports**: Import student lists from external files to get started quickly.
*   **Modern UI/UX**: Immersive edge-to-edge design built with Jetpack Compose and Material 3.
*   **Dark Mode Support**: Fully responsive theme that adapts to system preferences.

## 🛠 Tech Stack

*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Declarative UI)
*   **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles.
*   **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/)
*   **Local Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room)
*   **Backend/Cloud**: 
    *   **Firebase Authentication**: Secure user login and registration.
    *   **Firebase Firestore**: Real-time NoSQL database for cloud synchronization.
*   **Concurrency**: Kotlin Coroutines & Flow for reactive data streams.
*   **Jetpack Libraries**:
    *   Navigation Compose
    *   Biometric API
    *   Core Splashscreen API
    *   DataStore Preferences

## 🏗 Architecture

The project follows a modularized clean architecture pattern:
*   **Data Layer**: Handles database operations (Room DAOs), Firebase service implementations, and repository logic.
*   **Domain Layer**: Contains business logic, models, and repository interfaces (Pure Kotlin).
*   **Presentation Layer**: UI logic using ViewModels and Compose screens.

## ⚙️ Setup & Installation

### Prerequisites
*   Android Studio Ladybug or newer.
*   JDK 17 or higher.
*   A Firebase Project.

### Steps
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/Student-Attendance-App.git
    ```
2.  **Firebase Configuration**:
    *   Create a new project in the [Firebase Console](https://console.firebase.google.com/).
    *   Add an Android app with the package name `com.attendance.app`.
    *   Download the `google-services.json` file and place it in the `app/` directory.
    *   Enable **Email/Password Authentication** and **Cloud Firestore**.
    
3.  **Build & Run**:
    *   Open the project in Android Studio.
    *   Sync Gradle files.
    *   Click **Run** to install the app on your emulator or physical device.
