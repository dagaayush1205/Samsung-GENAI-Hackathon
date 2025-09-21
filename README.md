# Pose Landmarker Android - Hackathon Version

A clean and hackathon-ready Android app using **MediaPipe Pose Landmarker** with custom UI and camera support.

## Project Structure
```
```
pose-landmarker-android/
│── app/
│ ├── src/
│ │ ├── main/
│ │ │ ├── code/ # Kotlin source files
│ │ │ │ └── poselandmarker/
│ │ │ │ ├── MainActivity.kt
│ │ │ │ ├── WorkoutActivity.kt
│ │ │ │ ├── SplashActivity.kt
│ │ │ │ ├── OverlayView.kt
│ │ │ │ ├── MainViewModel.kt
│ │ │ │ ├── PoseLandmarkerHelper.kt
│ │ │ │ └── fragment/ # Fragments
│ │ │ ├── res/ # Layouts, drawables, values
│ │ │ └── AndroidManifest.xml
│ │ ├── androidTest/
│ │ └── test/
│ ├── build.gradle
│ ├── proguard-rules.pro
│ └── download_tasks.gradle
│
├── gradle/
├── build.gradle # Project-level
├── gradle.properties
├── settings.gradle
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
```
```

## How to Build & Run

1. Clone the repository:
```bash
git clone https://github.com/<your-username>/<repo-name>.git
cd pose-landmarker-android

    Open in Android Studio → Sync Gradle

    Build & run on an Android device (min SDK 21, Camera required)
