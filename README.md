# 🎵 MusicStream

**MusicStream** is a high-performance, feature-rich Android music application built using modern Android development practices. It provides a seamless streaming experience by integrating multiple music sources, advanced audio effects, and offline capabilities.

## ✨ Key Features

- **🚀 Dual Streaming Engine:** Combines results from YouTube (via NewPipe/Piped) and Saavn for a massive global library.
- **🎧 Pro Audio Experience:** Powered by **Media3 (ExoPlayer)** with support for high-quality audio formats.
- **🎚️ Advanced Audio FX:** Built-in Equalizer, Bass Boost, and Virtualizer for personalized sound.
- **📥 Offline Downloads:** Robust downloading system with progress tracking and offline playback support.
- **🔍 Smart Search:** Lightning-fast search with debouncing and history management.
- **🎨 Dynamic Theming:** Material 3 UI that dynamically adapts its color scheme based on the current track's album art using the Palette API.
- **🛠️ Resilient Networking:** Custom Piped instance rotation and proxy logic to ensure high uptime for YouTube streams.
- **📁 Playlist Management:** Create, manage, and curate your own music collections.

## 🛠️ Tech Stack

*   **UI:** Jetpack Compose (100% Declarative UI)
*   **Architecture:** Clean Architecture + MVVM (Model-View-ViewModel)
*   **Asynchronous:** Kotlin Coroutines & Flow
*   **Dependency Injection:** Hilt
*   **Networking:** Retrofit & OkHttp (with custom Interceptors)
*   **Database:** Room (SQLite)
*   **Media Handling:** Android Media3 (ExoPlayer/Session)
*   **Image Loading:** Coil
*   **Extraction:** NewPipe Extractor (Custom Piped-integrated Implementation)

## 🏗️ Architecture

The project follows the **Clean Architecture** pattern to ensure testability and scalability:

- **Data Layer:** Handles API calls, local database (Room), and repository implementations.
- **Domain Layer:** Contains business logic, Models, and Repository Interfaces.
- **Presentation Layer:** Jetpack Compose UI, ViewModels, and UI State management.

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- JDK 17+
- Android SDK 26 (Android 8.0) or higher.

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/music-stream-app.git
   ```
2. Open the project in Android Studio.
3. Sync Project with Gradle Files.
4. Run the app on an emulator or a physical device.

## 🛡️ Privacy & Reliability
MusicStream focuses on user privacy. It uses Piped instances to proxy YouTube requests, ensuring that your listening habits are not directly tracked while bypassing regional restrictions and IP-based blocking.

## 📜 Disclaimer
This project is for educational purposes only. The music content provided is sourced from public APIs and web extractors. Please respect the copyright of the artists and platforms.
