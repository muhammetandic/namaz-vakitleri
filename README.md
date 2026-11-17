# Namazgâh

Native Android prayer-times app built with Kotlin and Jetpack Compose. Turkish-first, accessibility-focused, and offline-first.

## Features

- Daily prayer times, next-prayer countdown, and kerahah status
- City and district selection with FusedLocationProvider + reverse geocoding
- Exact alarms with vibration-aware notification channels
- Custom adhan/MP3 sounds via `ACTION_OPEN_DOCUMENT` with persisted URI permission
- Sahur wake-up alarm with full-screen controls, snooze, and stop actions
- Monthly imsakiye with sahur/iftar countdown and fasting tracking
- Qibla compass using rotation-vector sensor and spherical trigonometry
- Zikirmatik with 33/99/100 targets and vibration feedback
- Jetpack Glance home-screen widgets (2x2 and 4x2)
- Simple Mode, dynamic font scaling, dark theme, and Material 3 design

Kaza (missed-prayer) tracking is intentionally out of scope. The tracker is dhikr-only.

## Tech stack

| Area | Choice |
| :--- | :--- |
| Language | Kotlin 2.4.21 |
| Build | Gradle 9.9.1, Android Gradle Plugin 9.5.1, KSP 2.3.14 |
| JDK | Java 21 |
| SDK | `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37` |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | Single-module package-by-feature Clean Architecture, MVI/UDF |
| DI | Hilt |
| Local data | Room, DataStore Preferences |
| Network | Retrofit, OkHttp, Aladhan API (Diyanet method 13) |
| Audio | Media3 ExoPlayer foreground service |
| Background work | WorkManager smart monthly sync |
| Widgets | Jetpack Glance + RemoteViews |
| Tests | JUnit, MockK, Turbine, Truth, Robolectric |

AGP 9 uses built-in Kotlin; there is no `org.jetbrains.kotlin.android` plugin.

## Build

Requirements:

- JDK 21
- Android SDK
- Android Studio or command-line Android tools

`local.properties` is gitignored. Android Studio creates it, or create it manually:

```properties
sdk.dir=/path/to/Android/Sdk
```

Commands:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
./gradlew dependencyUpdates
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Data and privacy

Prayer times are fetched from the Aladhan API using Diyanet method 13 and stored locally in Room. The selected city is sent to the API for calculation. Location history is not stored. No ads, tracking, or analytics are included. Offline mode shows the last downloaded month.

Sound asset credits are listed in [`app/ATTRIBUTION.md`](app/ATTRIBUTION.md).

## Repository layout

```text
app/src/main/java/com/anttech/prayertimes/
├── core/       # database, network, location, notifications, sensor, UI theme
├── features/   # times, calendar, qibla, tracker, settings, widget
├── navigation/ # destination definitions
├── MainActivity.kt
└── MainAppScaffold.kt
```

Tests live under `app/src/test` and `app/src/androidTest`.

See [`AGENTS.md`](AGENTS.md) for architecture rules, TDD standards, and project decisions.
