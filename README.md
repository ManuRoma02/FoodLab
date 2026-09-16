<div align="center">

<img src="./app/src/main/res/drawable/foodlab_launcher_symbol.png" alt="FoodLab Logo" width="120" height="120" />

# 🥗 FoodLab

### **Smart Nutrition, Fitness & Gamified Companion for Android**

[![Platform](https://img.shields.io/badge/Platform-Android%20(API%2024%2B)-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-orange.svg)](https://developer.android.com/topic/architecture)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

<br/>

<img src="./mascot_waving.webp" alt="FoodLab Mascot Companion" width="220" />

<p><em>An all-in-one, modern Android nutrition tracker featuring an interactive animated mascot, barcode scanning, custom recipes, pedometer sync, rich Android home screen widgets, and full Material 3 Adaptive UI.</em></p>

</div>

---

## ✨ Features

### 🍎 Nutrition & Calorie Tracking
- **Smart Macro Breakdown:** Real-time calculation and visual progress indicators for Calories, Protein, Carbohydrates, and Fats.
- **Meal Classification:** Categorized entries for Breakfast, Lunch, Dinner, and Snacks.
- **Streaks & Goals:** Gamified streak system with milestones to encourage continuous healthy habits.

### 🤖 Interactive Gamified Mascot Companion
- **Adaptive Reactions:** A dynamic companion reacting to your dietary and activity status (Idle, Studying, Cooking, Over Target, Wave, Celebration).
- **Multi-renderer:** Powered by Rive animations and custom Compose vector graphics with playful sound effects.

### 🔍 Barcode Scanning & Food Database
- **Google Play Services Code Scanner:** Instant barcode reading directly with the device camera without heavy MLKit bundles.
- **OpenFoodFacts & CREA Integration:** Comprehensive nutritional database search with offline caching.
- **Custom Foods & Recipe Creator:** Build and save custom meals and composite recipes with automatic per-serving macro computation.

### 👟 Pedometer & Activity Integration
- **Step Counter:** Hardware step-sensor tracking (`ACTIVITY_RECOGNITION`) measuring daily steps, distance (km), and estimated calories burned.
- **Background Sync:** Continuous updates with battery-efficient WorkManager routines.

### 📱 Android Home Screen App Widgets
- **Daily Calories Widget:** Quick overview of calories and remaining macros directly on your home screen.
- **Pedometer Widget:** Live step tracking meter.
- **Mascot / Pet Status Widget:** Companion status and mood on your home screen.
- **Quick Add Widget:** 1-tap shortcut to log meals.

### 📐 Android 15/16/17 Ready: Edge-to-Edge & Adaptive Layout
- **Immersive Edge-to-Edge:** Full transparent status and navigation bars with automatic light/dark icon contrast.
- **Keyboard (IME) Padding:** Zero layout shifting or obscured forms during text input.
- **Material 3 Adaptive Shell:**
  - **Compact Displays (Phones):** Ergonomic bottom navigation bar.
  - **Wide Displays (Tablets, Foldables, Landscape):** Navigation Rail layout with Dual-Pane List-Detail preview in the Food Database.

---

## 🛠️ Tech Stack & Architecture

FoodLab is built adhering to modern Android development best practices:

| Layer | Technology |
| :--- | :--- |
| **Language** | [Kotlin](https://kotlinlang.org/) (Coroutines, StateFlow, Flow) |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/) |
| **Architecture** | MVVM (Model-View-ViewModel) + Single Source of Truth (SSOT) |
| **Local Storage** | [Room Database](https://developer.android.com/training/data-storage/room) (SQLite with KSP schema tracking) |
| **Networking** | [Retrofit](https://square.github.io/retrofit/) + [Moshi](https://github.com/square/moshi) + [OkHttp](https://square.github.io/okhttp/) |
| **Background Tasks** | [AndroidX WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) |
| **Animations & FX** | [Rive Android](https://rive.app/) + Android SoundPool Manager |
| **Barcode Scanning** | Google Play Services Code Scanner |
| **Testing** | JUnit 4, Robolectric, Roborazzi screenshot verification |

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio:** Ladybug / Meerkat (or newer)
- **JDK:** Java 17 or Java 21/22
- **Android SDK:** `compileSdk 36`, `minSdk 24`

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/ManuRoma02/FoodLab.git
cd FoodLab

# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest
```

### Environment Configuration (`.env`)
Create a `.env` file in the project root based on `.env.example`:
```ini
# Optional: Required if using Gemini AI meal assistance
GEMINI_API_KEY=your_gemini_api_key

# Optional: Automated APK delivery via Telegram bot
TELEGRAM_BOT_TOKEN=your_telegram_bot_token
TELEGRAM_CHAT_ID=your_telegram_chat_id
```

---

## 📂 Project Structure

```
FoodLab/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/          # Room DB, Entities, DAOs & Repositories
│   │   │   │   ├── ui/            # ViewModels, Themes & Navigation
│   │   │   │   │   ├── screens/   # Compose Screens (Dashboard, Search, DB, etc.)
│   │   │   │   │   └── theme/     # Colors, Typography, Shapes
│   │   │   │   ├── util/          # Calculators, SoundManager, Workers
│   │   │   │   └── widget/        # AppWidgetProvider implementations
│   │   │   └── res/               # Vector drawables, layouts, sounds, strings
│   │   └── test/                  # Unit tests (Room, Calculators, ViewModels)
├── docs/                          # Architecture specs and development plans
├── scripts/                       # Automation and Telegram deployment tools
├── .gitignore
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
