## AuraFit: An On-Device, Multi-Modal AI Workout Coach
### Status: Samsung PRISM GenAI Hackathon 2025 Submission

Platform: Android (Kotlin)

### Core Technologies: MediaPipe, Samsung Health SDK, Android Room, Gemma3b (LLM)

## Table of Contents

1. [Overview](#overview)
2. [Core Features](#core-features)
3. [System Architecture](#system-architecture)
4. [Tech Stack](#tech-stack)
5. [Project Structure](#project-structure)
6. [Setup and Installation](#setup-and-installation)
7. [System Requirements](#system-requirements)
8. [Engineering Decisions & Bottlenecks](#engineering-decisions--bottlenecks)
9. [Submissions](#submissions)

## Overview
AuraFit addresses a critical gap in the at-home fitness market: the lack of accessible, private, and intelligent coaching. While many apps track workouts, they fail to provide the real-time, expert feedback necessary for safe and effective exercise. Existing AI solutions often compromise user privacy by streaming sensitive camera data to the cloud.

AuraFit is a revolutionary Android application, built entirely in Kotlin, that transforms a smartphone and a `Samsung Galaxy Watch` into a seamless, on-device AI coaching system. By fusing real-time computer vision with live biometric data, AuraFit delivers an unparalleled, expert-level training experience with a 100% privacy-first guarantee.

## Core Features

- **Multi-Modal Form Coaching**  
  Real-time form analysis for multiple exercises (e.g., push-ups, squats), enriched with live heart rate data streamed directly from the Galaxy Watch.  

- **Automated Rep Counting**  
  Hands-free, accurate repetition counting using a state-machine logic built on body angle detection.  

- **Data-Driven Dashboard**  
  A personalized home screen showing live daily steps, calories (via Samsung Health), and dynamic daily challenges with progress toward weekly goals.  

- **Hands-Free Voice Feedback**  
  AI-powered corrections and motivational cues delivered through Text-to-Speech so users can stay focused on training.  

- **Persistent Workout History**  
  Completed sessions are securely stored in an on-device Room database, enabling a detailed history view and profile with lifetime statistics.  

- **Login-Free Personalization**  
  Greets users by name using Android’s `ContactsContract`, delivering a personalized experience without requiring account creation.  

- **Multi-Model AI Model**
  Powered by `Gemma3b (1.3gb)` fully on-device LLM and a smart engine fallback ensuring 100 percent uptime and on-device privacy and security of data.

- **Daily Updates**
  Keeps Calendar based updates of every data daily. Serving as a personal workout diary.

## 3. System Architecture: The Hybrid AI Data Fusion Pipeline

AuraFit is built as a high-performance, real-time data fusion system, optimized for **on-device execution** to guarantee privacy and offline reliability. At its core is our **Hybrid AI Engine** — a dual-model architecture that ensures maximum intelligence while guaranteeing **100% operational uptime**, even under strict mobile memory constraints.

### Data Fusion Pipeline

- **Parallel Inputs**  
  - **Camera Feed:** Processed by Google’s MediaPipe Pose Landmarker, extracting 33 skeletal keypoints per frame in real time.  
  - **Biometric Feed:** Handled by a custom `HealthDataManager` using Samsung Health Data SDK for live heart rate monitoring.  

- **On-Device Processing**  
  Both data streams are fused within the **Hybrid AI Engine**, enabling synchronized physical + biometric analysis without external servers.  

### The Hybrid AI Engine

- **Reflex Engine (Rule-Based AI)**  
  A lightweight, always-on engine running on every frame. It performs **instantaneous, critical tasks** such as rep counting and injury-prevention form alerts. Its efficiency ensures continuous operation regardless of system load.  

- **Cognitive Engine ("Plug-and-Play" LLM Slot)**  
  A deeper reasoning layer designed for **extensibility**. Any on-device generative model can be integrated. For our prototype, we deployed a 1.3GB Gemma LLM with a crash-proof pause/resume protocol: the camera stream briefly freezes to release memory, then resumes seamlessly once inference is complete.  

- **Smart Fallback (100% Uptime Guarantee)**  
  If the Cognitive Engine cannot load due to memory limits, the app **gracefully defaults** to the Reflex Engine without interruption. This **dual-AI architecture** ensures the coaching experience remains intact under all conditions — no crashes, no loss of functionality.  

### Unified Output

Actionable feedback is delivered through two synchronized channels:  
- **On-Screen Visuals** for clarity.  
- **Hands-Free Voice Coaching** via Android’s native Text-to-Speech for uninterrupted workouts.  

---


## 4. Tech Stack

- **Platform:**  
  Android (Kotlin-first), ensuring modern language features, strong tooling support, and seamless integration with Jetpack libraries.  

- **Vision AI Engine:**  
  Google MediaPipe Pose Landmarker (`tasks-vision:0.10.9`) for real-time, on-device body keypoint detection (33 landmarks per frame).  

- **Wearable Integration:**  
  Samsung Health Data SDK (`samsung-health-data-api-1.0.0.aar`) to stream live biometric signals (e.g., heart rate) from Galaxy Watch devices.  

- **Cognitive AI Engine:**  
  **Gemma LLM (1.3GB, on-device)** integrated as the reasoning layer within the Hybrid AI Engine, using a custom pause/resume protocol to handle memory constraints without crashes.  

- **Data Persistence:**  
  Android Room Persistence Library for secure, structured, and offline-ready storage of workout history.  

- **UI & Architecture:**  
  Android Jetpack components (Fragments, Navigation) combined with **View Binding** and **Coroutines** for clean, responsive, and maintainable UI workflows.  

- **Voice Output:**  
  Android Native Text-to-Speech (TTS) Engine for natural, hands-free coaching feedback.  

## 5. Project Structure

The AuraFit codebase is organized into a **modular and scalable structure**, separating core AI logic, UI, data management, and resources for maintainability.
```
samsung_genai/
├── app/
│ ├── build.gradle # App-level Gradle configuration
│ ├── libs/
│ │ └── samsung-health-data-api-1.0.0.aar # Samsung Health SDK
│ ├── local.properties
│ ├── proguard-rules.pro
│ ├── download_tasks.gradle
│ └── src/
│ ├── androidTest/ # Instrumented tests
│ ├── test/ # Unit tests
│ └── main/
│ ├── AndroidManifest.xml
│ ├── assets/
│ │ └── pose_landmarker_full.task # MediaPipe model
│ ├── jniLibs/ # Native libraries (if any)
│ ├── java/
│ │ └── com/google/mediapipe/examples/poselandmarker/
│ │ ├── data/ # Room DB entities, DAO, Database
│ │ │ ├── AppDatabase.kt
│ │ │ ├── Challenge.kt
│ │ │ ├── WorkoutDao.kt
│ │ │ └── WorkoutSession.kt
│ │ ├── fragment/ # UI Fragments
│ │ │ ├── CameraFragment.kt
│ │ │ ├── DashboardFragment.kt
│ │ │ ├── PermissionsFragment.kt
│ │ │ ├── ProfileFragment.kt
│ │ │ └── WorkoutsFragment.kt
│ │ ├── HealthDataManager.kt # Samsung Health SDK manager
│ │ ├── HistoryActivity.kt
│ │ ├── HistoryAdapter.kt
│ │ ├── MainActivity.kt
│ │ ├── MainViewModel.kt
│ │ ├── OverlayView.kt
│ │ ├── PoseLandmarkerHelper.kt
│ │ ├── SplashActivity.kt
│ │ └── WorkoutActivity.kt
│ └── res/
│ ├── color/
│ │ ├── bg_nav_item.xml
│ │ └── bottom_nav_color.xml
│ ├── drawable/ # Images, icons, shapes
│ ├── layout/ # XML layout files
│ │ ├── activity_history.xml
│ │ ├── activity_main.xml
│ │ ├── activity_splash.xml
│ │ ├── activity_workout.xml
│ │ ├── calendar_day.xml
│ │ ├── fragment_camera.xml
│ │ ├── fragment_dashboard.xml
│ │ ├── fragment_profile.xml
│ │ ├── fragment_workouts.xml
│ │ └── item_history_entry.xml
│ ├── menu/
│ │ ├── bottom_nav_menu.xml
│ │ └── menu_bottom_nav.xml
│ ├── mipmap-hdpi/
│ ├── mipmap-mdpi/
│ ├── mipmap-xhdpi/
│ ├── mipmap-xxhdpi/
│ ├── mipmap-xxxhdpi/
│ ├── navigation/ # Navigation graphs
│ └── values/ # Colors, strings, dimens
├── build/ # Gradle build outputs
├── gradle/
├── gradle.properties
├── gradlew*
├── gradlew.bat
├── local.properties
├── pose_landmarker.png
├── settings.gradle
└── build.gradle # Project-level Gradle configuration
```

---

###  Highlights

- **Modular Java/Kotlin code** under `com.google.mediapipe.examples.poselandmarker/` separates:
  - `data/` → Room database
  - `fragment/` → UI screens
  - Core activities and helpers (`MainActivity.kt`, `PoseLandmarkerHelper.kt`, `HealthDataManager.kt`)
- **Assets & Models**:
  - MediaPipe model (`pose_landmarker_full.task`) in `assets/`
- **Resources**:
  - Layouts, menus, colors, drawables, navigation graphs under `res/`
- **External Libraries**:
  - Samsung Health SDK in `app/libs/`
- **Branching Note**:
  - Samsung Health integration is implemented in the [`watch` branch](https://github.com/dagaayush1205/Samsung-GENAI-Hackathon/tree/watch)  

---

This version is **hierarchical, readable, and clearly separates modules**, making it perfect for a README.

---
## 6. Setup and Installation

### Prerequisites
- **Android Studio Iguana (or newer)**  
- **Physical Android device** running API 29 (Android 10) or higher  
- **Samsung Galaxy Watch** + **Samsung Health app** (required for full biometric functionality)  

### Steps
1. **Clone the repository**
   ```bash
   git clone https://github.com/dagaayush1205/Samsung-GENAI-Hackathon.git
   cd Samsung-GENAI-Hackathon

2. **Open the Project**
Launch **Android Studio** and open the cloned project directory.  

3. **Add Samsung Health SDK**
Ensure `samsung-health-data-api-1.0.0.aar` is placed in the `app/libs/` directory.  

4. **Sync Gradle Dependencies**
Allow Android Studio to complete the Gradle sync.  

5. **Run the App**
Connect your physical Android device and click **Run ▶️** in Android Studio.  

### Grant Permissions
On first launch, allow access to:
- Camera  
- Contacts  
- Samsung Health  


## 7. System Requirements

- **IDE:**  
  [Android Studio Iguana (2023.2.1) or later](https://developer.android.com/studio)  

- **Gradle JDK:**  
  The project is configured to build with **JVM 17**.  
  Ensure your Android Studio’s Gradle JDK is set to JDK 17 under:  
  `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`  
  👉 [Download OpenJDK 17](https://adoptium.net/temurin/releases/?version=17)  

- **Target Device:**  
  A physical Android device running **API 29 (Android 10)** or higher.  
  *(Required by Samsung Health SDK for biometric integration.)*  

- **Samsung Health SDK:**  
  Download the Samsung Health Data SDK (`samsung-health-data-api-1.0.0.aar`) here:  
  🔗 [Samsung Health Data SDK](https://developer.samsung.com/health/android/data/overview.html)  

- **Cognitive AI Models (Optional for Cognitive Engine):**  
  AuraFit supports **plug-and-play on-device LLMs**. For our prototype, we integrated **Gemma 1.3B**.  
  - 🔗 [Gemma Models (Google AI)](https://ai.google.dev/gemma)  
  - 🔗 [Hugging Face Gemma Repository](https://huggingface.co/google/gemma)  
  - 🔗 [Other On-Device Models (Hugging Face Models Hub)](https://huggingface.co/models?library=transformers&sort=downloads)  

## 8. Engineering Decisions & Bottlenecks

During development, we faced several significant technical challenges. Our solutions to these problems became a core part of AuraFit’s innovation.  

---

### Decision: In-Place Refactoring vs. New Project
- **Challenge:** Starting from scratch would have meant weeks of boilerplate work configuring **CameraX + MediaPipe**.  
- **Decision:** We chose to build directly on the **official MediaPipe Pose Landmarker sample**, performing extensive **in-place refactoring**.  
- **Impact:** This gave us a **stable, production-ready camera + pose pipeline** from day one, allowing the team to focus on our **novel contributions** (UI, database, AI logic, wearable SDK integration). Development velocity increased dramatically.  

---

### Bottleneck: Samsung Health SDK Integration
- **Problem:** Samsung provides **multiple SDKs** (Data, Sensor, Accessory). Our initial attempt with the **Sensor SDK** led to build failures, unstable runtime behavior, and inconsistent API support.  
- **Solution:** We pivoted to the **Samsung Health Data SDK**, which is robust and officially documented.  
  - Built a modern **Kotlin-first `HealthDataManager.kt`** wrapper based on Samsung’s legacy Java examples.  
  - Implementation is currently maintained in the dedicated **[`watch` branch](https://github.com/dagaayush1205/Samsung-GENAI-Hackathon/tree/watch)** (not `main`).  
  - Achieved stable, reliable fetching of both **historical stats** and **live heart rate data** from the Galaxy Watch.  
- **Outcome:** A clean, extensible integration that future-proofs wearable data collection.  

---

### Bottleneck: On-Device LLM Memory Crashes
- **Ambition:** We aimed to integrate **Google’s Gemma 2B** as an on-device LLM for generative workout feedback.  
- **Problem:** Running the **1.3GB model** concurrently with a live camera feed caused **native memory crashes (SIGSEGV)** on typical Android devices.  
- **Solution:**  
  - Engineered a **Hybrid AI architecture** with a **pause/resume protocol**: the camera stream temporarily freezes to release memory, allowing the LLM to run safely.  
  - This worked. We tested it with the Gemma3b Model and also created a smart fallback Comprehensive Rule Based engine for 100% uptime.
  - **Outcome:** A **crash-proof Cognitive Engine design** that guarantees 100% uptime today, while paving the way for true on-device generative AI tomorrow.  

---

**Key Takeaway:**  
AuraFit’s biggest innovations came directly from solving these bottlenecks:  
- **Leveraging stable foundations** (MediaPipe sample refactoring)  
- **Building clean integrations** (Samsung Health Data SDK manager)  
- **Pioneering crash-proof AI architectures** (Hybrid Reflex + Cognitive engines with smart fallback)  


## 9. Submissions

    Video Demo URL: [Link to your YouTube or Google Drive video]

    Supplementary PDF Report: [Link to your TeamName.pdf if hosted online]
