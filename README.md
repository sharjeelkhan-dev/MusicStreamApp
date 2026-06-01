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

### Prerequisites
*   Android Studio Ladybug or newer.
*   JDK 17 or higher.
*   A Firebase Project.
    
3.  **Build & Run**:
    *   Open the project in Android Studio.
    *   Sync Gradle files.
    *   Click **Run** to install the app on your emulator or physical device.

<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/65cdfd96-07d5-474a-9ea0-3ebcc83d527c" />
<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/1f076d8d-44ce-4464-a6ad-56fae404f5e7" />
<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/70faec7f-5e8a-4a5d-9d53-631329355aa6" />
<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/ddd986e1-2734-48df-845b-20684bf3a941" />
