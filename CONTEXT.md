# FoodLab Project Context

Last updated: 2026-07-23

This is the canonical operational context for FoodLab. Read it before changing the app and update it whenever behavior, architecture, visual identity, persistence, or verification changes. `README.md` is the short entry point.

## Product and runtime

FoodLab is a native Android nutrition and activity tracker. The main UI is Jetpack Compose; local data is stored with Room; background work uses WorkManager; Open Food Facts is accessed with Retrofit and Moshi. The Android package namespace is `com.example`, while the application ID is `com.aistudio.foodtrack.mxpt`.

The application class is `app/src/main/java/com/example/FoodLabApplication.kt`. It owns lazy database/repository/API instances. `MainActivity` is the launcher activity. The app targets Android SDK 36 with minimum SDK 24.

## Current visual identity

The canonical mascot artwork is `app/src/main/res/drawable-nodpi/foodlab_mascot_duo.png`: a green slime monster visibly annoyed while its cheerful ghost friend pulls its head. This exact duo is used on the Dashboard, launcher foreground, widgets, search, and food-detail empty states. Do not reintroduce the previous mascot or a visually unrelated variant.

The main palette is defined in `app/src/main/java/com/example/ui/theme/Color.kt`: near-black surfaces, white/slate text, and FoodLab green `#22C55E`. Macro identity is consistent across the app and widgets:

- protein: green `#22C55E`
- carbohydrates: amber `#F59E0B`
- fat: cyan `#06B6D4`

## Mascot rendering and motion

The single active mascot implementation is `AnimatedFoodLabMascot` in `app/src/main/java/com/example/ui/screens/MascotComponents.kt`. It applies Compose transforms to the shared PNG: breathing, floating, a periodic ghost-tug reaction, typing motion, an over-target irritated shake, state-colored glow, and a springy tap response with lift, squash, overshoot, and rotation. Keep the duo artwork shared; do not create another Kotlin Canvas mascot.

The former 60-byte `.riv` stub, its wrapper, and the Rive runtime were removed because the file had no artboard and could never render. Reintroduce Rive only with a genuine export of this exact slime-and-ghost design and a demonstrated animation advantage.

## Widgets

Both nutrition widgets are fixed 2×2 widgets. Their provider metadata must keep `110dp` minimum width and height, `targetCellWidth="2"`, `targetCellHeight="2"`, and `resizeMode="none"`.

`widget_pet_status.xml` is the primary compact status widget. It encapsulates the mascot, `TODAY`/`OVER` status, consumed calories, and calorie target together inside the green mascot halo container (`@drawable/widget_mascot_halo_bg`), above the three consumed macro values. The middle progress bar line has been removed and calorie status text uses slate (`#94A3B8`) rather than green to avoid visual confusion with protein. Macro labels are intentionally not visible because color identifies them; dynamic English content descriptions preserve accessibility. `PetStatusWidgetProvider.kt` supplies Room-backed values and opens the appropriate meal flow when tapped.

The widget layout must remain proportional rather than using fixed content heights. Launchers often allocate roughly 140–160dp to a nominal 2×2 widget even though the declared minimum is 110dp. Weighted header and action/macro rows let the mascot, calorie hierarchy, and bottom controls fill both the minimum size and the larger real launcher allocation without adding information.

`widget_daily_calories.xml` is the compact action widget. It uses the same surface, typography, mascot-wrapped calorie hierarchy, plus one `ADD FOOD` action. All visible widget copy is English.

Widget add-food intents always target today and pass canonical meal categories from `MealTypes`: `BREAKFAST`, `LUNCH`, `DINNER`, or `SNACK`. The Dashboard also canonicalizes stored meal categories while grouping, so legacy widget entries saved as title case or `Break` remain visible in the Home meal list. Do not introduce new category spellings directly.

Widget colors and shapes live in `app/src/main/res/drawable/widget_*`. The former gold-label backgrounds were removed because they did not match the active app theme.

## Data and recent engineering work

Room schema version 6 includes food search and meal/weight indexes plus meal templates and food portions. Local search ranking prioritizes favourites, use count, prefix matches, and contains matches. Open Food Facts search trims queries, debounces and cancels stale jobs, and shares nutrition mapping with barcode search. `NutritionCalculator` centralizes serving math. Pedometer startup is idempotent and uses locale-stable date keys. These behaviors should be preserved.

The Home streak is reactive Room data, never a UI default. `StreakCalculator` counts calendar-consecutive days within ±10% of the resolved calorie target, using specific-date target, weekday target, then `FIXED`. An incomplete current day preserves the run through yesterday; a completed current day extends it; an already over-target current day resets it. Missing past days break the run. `DashboardViewModel.streakCount` also watches the day boundary so an open app cannot retain yesterday's result.

## Project hygiene

The repository keeps only current source, Android configuration, current design/planning documents, native widget previews under `artifacts/`, and the latest installable APK as `FoodLab_Latest.apk`. Generated Gradle/Kotlin directories, obsolete APK copies, abandoned mascot assets, copied agent/skill packages, and superseded design/context documents are not part of the project. Workspace-local `.tokensave` data remains because active TokenSave processes use it; `.git_backup/config` remains as the only source-control recovery metadata. Neither is packaged in the app.

## Verification

Use JDK 22 on this workstation because the default Java selection may be incompatible:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-22'
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" testDebugUnitTest assembleDebug
```

`app/src/test/java/com/example/widget/WidgetLayoutTest.kt` inflates both widget layouts at the 110×110 mdpi minimum and a representative 144×144 launcher allocation using Robolectric native graphics. It checks bounds, text clipping, full-height use, and value-only macro previews, then writes render captures to `app/build/reports/widget-layout/`.

On this OneDrive checkout, Gradle intermediates can occasionally be locked. Stop Gradle before removing only verified generated paths under `app/build`; never delete the workspace or user files. Device-level animation and launcher QA require an attached emulator/device and `adb`.

Latest verification on 2026-07-22: a clean snapshot outside OneDrive passed all 11 unit tests across 5 suites with zero failures, errors, or skips; the exact-size widget tests passed and `assembleDebug` completed successfully. The first in-place run was blocked before compilation by an `AccessDeniedException` on a generated `app/build/intermediates` directory, so clean verification used `--no-daemon --max-workers=1 --no-configuration-cache` outside the synchronized folder. Native 110×110 previews are retained under `artifacts/`. Device-level QA was not available in this run.

Cleanup verification on 2026-07-22: after removing the unusable Rive stack, uncalled sound assets, stale build outputs, duplicate APKs, obsolete design/context files, and copied tooling, a fresh build again passed all 11 tests and `assembleDebug`. The verified debug APK dropped from 40,716,067 bytes to 21,150,131 bytes and replaced `FoodLab_Latest.apk`.

Widget fill verification on 2026-07-22: proportional widget rows passed native rendering at both 110×110 and 144×144 with no clipping and full usable-height coverage. The full suite passed all 12 tests and `assembleDebug` succeeded. ADB was found in the configured Android SDK, but no phone or emulator was connected for launcher-level capture.

Widget meal-flow verification on 2026-07-22: the launcher intent now resets the selected date to today and canonicalizes its meal category before navigation. All new single-food and template logs use the same canonical categories, while Dashboard grouping maps legacy title-case values and `Break` into visible sections. The full suite passed all 15 tests across 6 suites and `assembleDebug` succeeded. Device-level launcher verification remained unavailable because ADB reported no connected device.

Streak verification on 2026-07-23: the hardcoded `3` UI default and its fake test were removed. The Home now collects `DashboardViewModel.streakCount`, backed by reactive meals, all target tiers, and a day-boundary ticker. Six deterministic tests cover ±10% boundaries, incomplete and over-target today, missing days, target precedence, and future meals. The full suite passed all 20 tests across 7 suites and `assembleDebug` succeeded. ADB again reported no connected device.
