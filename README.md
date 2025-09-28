## AuraFit(watch): An On-Device, Multi-Modal AI Workout Coach
### Status: Samsung PRISM GenAI Hackathon 2025 Submission
Made by- Team Astro Bugs (SRM Institute of Science and Technology, KTR)

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

This flow of software is very straight forward. It either polls data from sensors or gets a continous set of data
## 4. Tech Stack

## 5. Project Structure

## 6. Setup and Installation

## 7. System Requirements

## 8. Engineering Decisions & Bottlenecks

### Prerequisites

### Steps
