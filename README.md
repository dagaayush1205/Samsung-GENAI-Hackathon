## AuraFit(watch): An On-Device, Multi-Modal AI Workout Coach
### Status: Samsung PRISM GenAI Hackathon 2025 Submission
Made by- Team Astro Bugs (SRM Institute of Science and Technology, KTR)

Platform: Android (Kotlin)

### Core Technologies: MediaPipe, Samsung Health SDK, Android Room, Gemma3b (LLM)
## Table of Contents

1. [Overview](#overview)
2. [Core Features](#core-features)
3. [System Architecture](#system-architecture)
4. [UI](#UI)
5. [Project Structure](#project-structure)
6. [System Requirements](#system-requirements)
7. [Engineering Decisions & Bottlenecks](#engineering-decisions--bottlenecks)

## Overview
Aurafit(watch) connects the Aurofit mobile device to the watch utilizing Samsung Health Data and Samsung Health Sensor. The idea behind this is to aid the Aurofit to give more accurate results and enabling smooth data integration.

This provides important information such as Heart-rate, Body Oxygen Saturation(SPO2), Stress, Steps and Inertial Measurment Units which are implemented in the current software.

This collected data is transmitted to the mobile device for deeper analysis. 

## Core Features

- **PPG - Photoplethysmography sensor**
  It is is non-invasive sensor that measures the changes in blood volume. It is comprised of PPG green, infrared, and red data. In this case it is being used to determine stress score of the person.

- **Heart rate**
  Measures the heart rate of the body

- **SpO2**
  Measures the blood oxygen level in the body.

- **IMU**
  This provides with Gyroscope and Accelerometer which are further used for various purpose these include counting steps, counting number of reps during a set.

- **Samsung Health SDK Integration**
  This provides with a lot of proccessed data, one example is stress score. A lot of sensor present are propriety and therefore can only be used with the help of samsung health SDK and the samsung sensor package.

  
## 3. System Architecture
<img width="1245" height="353" alt="Smart flowchart(1)" src="https://github.com/user-attachments/assets/e7e49f19-0eea-4bb6-b3f5-85686427df6c" />

This flow of software is very straight forward. Depending on the sensor it polls data from sensors or gets a continous stream of data.

## 4. UI
<img width="450" height="450" alt="Screenshot_20250928_102855_samsunghackathon" src="https://github.com/user-attachments/assets/d882e093-4cc9-4852-836a-c6f014dea5ef" />
<img width="450" height="450" alt="Screenshot_20250928_102907_samsunghackathon" src="https://github.com/user-attachments/assets/6afc6925-097e-4662-ac43-cf1652164dab" />

Displays basic data and derived sensor data which is available.

## 5. Project Structure
```
.
├── app
│   ├── build.gradle.kts
│   ├── libs
│   ├── lint.xml
│   ├── proguard-rules.pro
│   └── src
├── build.gradle.kts
├── gradle
│   ├── libs.versions.toml
│   └── wrapper
├── gradlew
├── gradlew.bat
├── README.md
└── settings.gradle.kts

2 directories, 12 files
```
## 6. System Requirements
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
  Download the Samsung Health Sensor SDK (`samsung-health-sensor-api-1.4.1.aar`) here:[Samsung Health Sensor SDK](https://developer.samsung.com/health/sensor/guide/introduction.html)
  

## 7. Engineering Decisions & Bottlenecks

### Bottleneck: Samsung Health SDK Integration

- **Problem:** Samsung provides **multiple SDKs** (Data, Sensor, Accessory). Our initial attempt with the **Sensor SDK** led to build failures, unstable runtime behavior, and inconsistent API support.  
- **Solution:** We pivoted to the **Samsung Health Data SDK**, which is robust and officially documented.  
  - Built a modern **Kotlin-first `HealthDataManager.kt`** wrapper based on Samsung’s legacy Java examples.  
  - Implementation is currently maintained in the dedicated **[`watch` branch](https://github.com/dagaayush1205/Samsung-GENAI-Hackathon/tree/watch)** (not `main`).  
  - Achieved stable, reliable fetching of both **historical stats** and **live heart rate data** from the Galaxy Watch.  
- **Outcome:** A clean, extensible integration that future-proofs wearable data collection.

Made with Love <3 
Team Astro Bugs (SRM IST, KTR)
