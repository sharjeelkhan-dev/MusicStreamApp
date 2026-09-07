# 🎵 MusicStream
![Status](https://img.shields.io/badge/Code-181717?style=for-the-badge&logo=github&logoColor=white) ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white) ![AI](https://img.shields.io/badge/Google_Gemini-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)  ![Media3](https://img.shields.io/badge/Media3-ExoPlayer-blue?style=for-the-badge&logo=google&logoColor=white)

> A high-performance, feature-rich Android music application built with Jetpack Compose and Clean Architecture, designed to deliver an uninterrupted, premium streaming experience.

| Subsystem | Technical Execution Architecture |
| :--- | :--- |
| 🤖 **AI-Powered Commands** | Implemented Gemini AI-driven song summarization and lyrics explanation features, providing users with deeper artistic context directly within the playback interface. |
| **🚀 Dual-Engine Streaming** | Seamlessly combines search results and streams from YouTube (via Piped/NewPipe Extractor) and Saavn for a massive global library. |
| **🎧 Pro Audio Pipeline** | Fully powered by **Android Media3 (ExoPlayer)** with native integration of high-quality audio formats and custom audio sessions. |
| **🎚️ Advanced Audio FX** | Built-in high-fidelity Equalizer, Bass Boost, and Virtualizer components for dynamic, personalized sound engineering. |
| **📥 Offline Downloader** | Robust offline downloading system featuring precise download progress tracking, background threading, and offline playback. |
| **🎨 Palette Dynamic Themes** | Material 3 interface that dynamically extracts prominent colors from the playing track's album art to recolor the entire app UI. |
| **🛠️ Resilient Networking** | Custom proxy rotation logic with a built-in failover mechanism across multiple Piped instances to guarantee zero-downtime streaming. |
| 🎨 **Asset Attribution** | Core system actions, navigation nodes, and modern news channel vector elements curated via [Uxwing](https://uxwing.com/). |

<details>
<summary><b>✨ View Interface Design (Click to Expand)</b></summary>
<br/>
<table width="100%">
  <!-- Row 1 -->
  <tr>
    <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/059e1545-8280-43cd-b7a1-bbf82effc153" width="100%" alt="Screen 1" />
      <br/><sub><b>Interface View 1</b></sub>
    </td>
    <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/df2f9a15-04e7-4864-8d5b-47393f7a9e3b" width="100%" alt="Screen 2" />
      <br/><sub><b>Interface View 2</b></sub>
    </td>
    <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/0dd9e913-42f1-442e-9c7e-ad0706b03e70" width="100%" alt="Screen 2" />
      <br/><sub><b>Interface View 2</b></sub>
    </td>
  
  </tr>
  <!-- Row 2 -->
  <tr>
    <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/b342d87c-4d00-49be-9689-521ea144479f" width="100%" alt="Screen 4" />
      <br/><sub><b>Interface View 4</b></sub>
    </td>
    <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/750f52a4-f4d9-407e-ab7f-8182b8af7c10" width="100%" alt="Screen 5" />
      <br/><sub><b>Interface View 5</b></sub>
    </td>
    <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/b4df47cc-c5bf-43eb-b108-db71b13cc207" width="100%" alt="Screen 6" />
      <br/><sub><b>Interface View 6</b></sub>
    </td>
  </tr>
  <!-- Row 3 -->
  <tr>
    <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/8357d7f0-6b69-4d3c-a531-3ff25a2c7bf0" width="100%" alt="Screen 7" />
      <br/><sub><b>Interface View 7</b></sub>
    </td>
    <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/86c02a87-7a27-4fea-9e83-24dbc0a83bfc" width="100%" alt="Screen 8" />
      <br/><sub><b>Interface View 8</b></sub>
    </td>
     <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/633ca5e1-962c-4082-a4e4-325a762e3e1c" width="100%" alt="Screen 9" />
      <br/><sub><b>Interface View 9</b></sub>
    </td>
  </tr>
    <!-- Row 4 -->
  <tr>
    <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/3e7813cf-b606-4798-8b4a-4aff1876e945" width="100%" alt="Screen 10" />
      <br/><sub><b>Interface View 10</b></sub>
    </td>
    <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/59b4658c-2507-43e6-a43b-a172607164d3" width="100%" alt="Screen 11" />
      <br/><sub><b>Interface View 11</b></sub>
    </td>
     <td width="33.3%" align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/536d1365-5794-4e95-a953-01fd1fc536ad" width="100%" alt="Screen 12" />
      <br/><sub><b>Interface View 12</b></sub>
    </td>
  </tr>
</table>
</details>

---

## 🛠 Setup & Installation

### 📋 Prerequisites
*   Android Studio Ladybug (or newer)
*   JDK 17 or higher
*   Android SDK 26 (Android 8.0) or higher
