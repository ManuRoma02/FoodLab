# Mascot source map

Module directory is `app/` (from `settings.gradle.kts` `include(":app")` and `scripts/write_mascot_html.py`). There is no top-level `src/`. Later tasks that say `src/main/...` mean `app/src/main/...`.

`app/` is **not present on this Dropbox checkout** (not even as a placeholder). Paths below are reconstructed from readable Gradle/scripts plus `FoodLab_Latest.apk` (class names, Kotlin `SourceFile` attributes, and packaged resources). Source set is `app/src/main/java/` (standard for this `com.example` AI Studio layout; AGP also compiles `kotlin/` if it exists).

- HOME_FILE: app/src/main/java/com/example/ui/screens/DashboardScreen.kt
- WIDGET_LAYOUT: app/src/main/res/layout/widget_pet_status.xml
- WIDGET_PROVIDER: app/src/main/java/com/example/widget/PetStatusWidgetProvider.kt
- ASSETS_DIR: app/src/main/assets/mascot_3d
- VIEWER_HTML: app/src/main/assets/mascot_3d/index.html
- APPLICATION_ID: com.aistudio.foodtrack.mxpt
- PACKAGE: com.example.ui.screens
- WRITE_MASCOT_HTML: scripts/write_mascot_html.py
- BUILD_INLINED_HTML: scripts/build_inlined_3d_html.py

## Extra discovered files (not required keys)

- MASCOT_WEBVIEW_FILE: app/src/main/java/com/example/ui/screens/MascotComponents.kt
  - Composables: `AnimatedFoodLabMascot`, `ThreeJsMascot3DView` (Android `WebView` + `AndroidView`)
  - Loads `mascot_3d/index.html`; JS `setMascotState` / `onMascotTapped`
- APPLICATION_CLASS: app/src/main/java/com/example/FoodLabApplication.kt
- MAIN_ACTIVITY: app/src/main/java/com/example/MainActivity.kt
- NAMESPACE / R package: com.example
- THREE_JS: app/src/main/assets/mascot_3d/three.min.js
- WIDGET_INFO: app/src/main/res/xml/app_widget_pet_status_info.xml
- WIDGET_MASCOT_DRAWABLE: app/src/main/res/drawable-nodpi/foodlab_mascot_duo.png

## Other home-screen widgets (do not treat as the mascot widget)

| Layout | Provider |
| --- | --- |
| app/src/main/res/layout/widget_daily_calories.xml | app/src/main/java/com/example/widget/DailyCaloriesWidgetProvider.kt |
| app/src/main/res/layout/widget_pedometer.xml | app/src/main/java/com/example/widget/PedometerWidgetProvider.kt |
| app/src/main/res/layout/widget_quick_add.xml | app/src/main/java/com/example/widget/QuickAddWidgetProvider.kt |

No Glance `GlanceAppWidget` classes in the APK. Widget stack is classic `AppWidgetProvider` + XML layouts.

## Evidence

- `settings.gradle.kts`: `include(":app")`
- `scripts/write_mascot_html.py` and `scripts/build_inlined_3d_html.py` write `app/src/main/assets/mascot_3d/index.html`
- `aapt dump badging FoodLab_Latest.apk`: `package: name='com.aistudio.foodtrack.mxpt'`
- APK assets: `assets/mascot_3d/index.html`, `assets/mascot_3d/three.min.js`
- APK Kotlin SourceFile: `DashboardScreen.kt`, `MascotComponents.kt`, `PetStatusWidgetProvider.kt`
- APK class: `Lcom/example/ui/screens/DashboardScreenKt;` (home UI)
- APK class: `Lcom/example/widget/PetStatusWidgetProvider;` + layout `widget_pet_status`
