## 🚀 Featured Project 2: Smart Student Attendance & Analytics App

A modern, high-performance Android application designed to streamline student attendance management using an offline-first architecture for absolute data durability.

### ✨ Key Features
*   **💾 Offline-First Experience:** Mark attendance without an internet connection using local Room Database storage.
*   **🔄 Real-time Cloud Sync:** Automatic and manual synchronization with Firebase Firestore to ensure data is never lost.
*   **⚡AI Parsing Engine:** Integrated "Google Gemini 3.5 Flash" for processing natural language scheduling logic & Take Attendance Daily.
*   **🔒 Biometric Security:** Protect sensitive student data with Fingerprint and Face Unlock integration.
*   **📊 Comprehensive Analytics:** Visualize student performance through attendance percentages, recent session history, and detailed reports.
*   **🏫 Class Management:** Easily create, update, and manage multiple classes, sections, and student lists.
*   **🎨 Modern UI/UX:** Immersive edge-to-edge design built with Jetpack Compose, Material 3, and Dark Mode support.

### 🛠 Tech Stack & Architecture
*   **Language & UI:** Kotlin | Jetpack Compose (Declarative UI)
*   **Architecture & Design:** Clean Architecture + MVVM Pattern
*   **Dependency Injection:** Dagger Hilt
*   **Local & Cloud Database:** Room Persistence Library | Firebase Firestore (NoSQL)
*   **Jetpack Libraries:** DataStore Preferences | Biometric API | Core Splashscreen API | Navigation Compose
*   **Concurrency:** Kotlin Coroutines & Flow for reactive data streams

### 🎬 Project Showcase & Security Note
> 🔒 **Security Notice:** The source code of this repository is kept **Private** to safeguard enterprise-level Firebase configuration details and secure local biometrics logic. 
*   **[📸 View Application Screenshots / UI Flow](

<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/756269f4-5b21-4e5e-b781-c3d22fbe54d1" />
<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/7ddd222b-a5d6-45a9-8840-24b5343fadb8" />
<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/1692f15c-9af5-4969-a710-3d86a33dcf28" />
<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/1f86572b-e9a8-420d-a904-a4966bd59517" />
<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/2b8325ed-24c7-4b11-a6ac-1d137345ec80" />
<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/6615c639-e08e-4026-862b-65521b84675e" />
<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/a3f722a9-51eb-4afa-8b93-9a23cd066ce4" />
<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/043df7a1-4c58-46b7-9942-ce2ac76379a9" />
<img width="720" height="1600" alt="image" src="https://github.com/user-attachments/assets/3f945c09-b99d-4573-8279-9720f4079384" />

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
