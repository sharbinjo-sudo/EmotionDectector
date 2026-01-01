# 😊 Emotion Detector App (Android)

An Android application that uses **deep learning** and **Google’s Face Detection** to recognize human emotions from facial images.

The app detects a face using Google’s ML-based face detector and classifies the emotion into one of the following categories:

* **Happy**
* **Sad**
* **Angry**
* **Surprise**

---

## 📌 Project Overview

Accurate emotion recognition requires reliable face detection. This project combines **Google’s Face Detection** for precise face localization with a **deep learning–based emotion classifier**. The detected face is cropped, preprocessed, and passed to the model for real-time emotion prediction directly on the device.

---

## 🧠 Model & Detection Pipeline

* Face Detection: Google ML Kit Face Detection
* Emotion Classification: Convolutional Neural Network (CNN)
* Classes: Happy, Sad, Angry, Surprise
* Inference: On-device (TensorFlow Lite)

**Pipeline:**

1. Capture image from camera
2. Detect face using Google Face Detection
3. Crop and preprocess face region
4. Predict emotion using CNN model

---

## 🚀 Features

* Accurate face detection using Google ML Kit
* Real-time emotion recognition
* On-device deep learning inference
* Fast and lightweight Android application

---

## 🛠️ Tech Stack

* Android (Java / Kotlin)
* Google ML Kit Face Detection
* TensorFlow / TensorFlow Lite
* CameraX / OpenCV

---

## 📂 Dataset(fer-2013)

The emotion classifier is trained on a facial emotion dataset containing images labeled as:

* Happy
* Sad
* Angry
* Surprise

---

## 🔧 Future Improvements

* Add more emotion classes
* Improve performance in low-light conditions
* Support multiple face detection
* Enhance UI and user experience

---
