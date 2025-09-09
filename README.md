# Nudity-Detection-Android

Android app for detecting nudity in images using skin pixel classification and face region analysis.  
Originally implemented by **Rahat Yeasin Emon**, based on the research paper:  
- [A Novel Nudity Detection Algorithm for Web and Mobile Application Development](https://arxiv.org/ftp/arxiv/papers/2006/2006.01780.pdf)  
- [ResearchGate Link](https://www.researchgate.net/publication/341851946_A_Novel_Nudity_Detection_Algorithm_for_Web_and_Mobile_Application_Development)

Although this repository focuses on the Android implementation, the original algorithm was designed to support both **web and mobile platforms**, making it adaptable across environments.

> **Disclaimer**: This project is adapted for educational and developmental purposes. We do not claim ownership of the original algorithm, research, or implementation. All intellectual property rights belong to their respective authors and contributors.

## Prerequisites
- Android Studio (Arctic Fox or newer)
- Android SDK 33+
- Gradle 7.x+
- Kotlin 1.6+
- Physical or virtual Android device (API 21+)

## Dependencies
- AndroidX libraries
- Google Play Services (Vision API)
- Kotlin Standard Library

## Installation

1. **Clone the repository**
```
git clone https://github.com/wkwk08/Nudity-Detection-Android.git
cd Nudity-Detection-Android
```

2. **Connect your Android device**
```
adb devices
```

3. **Build the APK**
```
./gradlew assembleDebug
```

4. **Install the APK to your device**
```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

5. **Run the app manually**
```
adb shell am start -n com.example.nuditydetectionapp/.MainActivity
```

## Usage
- Launch the app on your device
- Load an image into the interface
- The app will automatically detect skin pixels and highlight them
- Detection stats will be printed to the log (face region vs. full image)

## Attribution
This project is adapted from the original implementation by **Rahat Yeasin Emon**  
GitHub: [rahatyeasinemon/Nudity-Detection-Android](https://github.com/rahatyeasinemon/Nudity-Detection-Android)