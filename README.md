# SafePulse

SafePulse is an Android and Wear OS commute safety suite built for emergency response, journey monitoring, risk-aware maps, and phone-watch safety actions. The project contains a main Android phone app and a companion Wear OS app that communicate through the Google Wearable Data Layer.

The app is designed around practical personal safety workflows: quick SOS, silent alerts, live journey tracking, risk-map awareness, safest-route discovery, trusted contacts, emergency timelines, and wearable shortcuts.

## Table of Contents

- [Highlights](#highlights)
- [Core Features](#core-features)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Requirements](#requirements)
- [Setup](#setup)
- [Build and Run](#build-and-run)
- [Configuration](#configuration)
- [Data and Assets](#data-and-assets)
- [Permissions](#permissions)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Safety Notice](#safety-notice)

## Highlights

- Native Android app built with Kotlin and Jetpack Compose.
- Wear OS companion app with tiles, complications, quick safety actions, and phone sync.
- Continuous foreground safety monitoring with location, sensors, risk scoring, voice trigger, and shake detection.
- Safe Routes screen with Leaflet map, route risk analysis, safest-route overlay, and transport recommendation.
- Risk Map with crime zones, disaster zones, police, hospitals, safe zones, and emergency facilities.
- Room database preload from bundled safety datasets.
- DataStore and encrypted preferences for settings and safety PIN configuration.
- TensorFlow Lite keyword spotting asset for voice-trigger workflows.

## Core Features

### Emergency and SOS

- Manual SOS from the home screen.
- Silent SOS mode for discreet SMS-based help requests.
- Emergency countdown with cancel support.
- PIN-protected cancel flow.
- Duress PIN flow that appears to cancel SOS while continuing silent protection.
- Emergency SMS to trusted contacts with current/latest location.
- Emergency call support for primary contact when silent mode is not active.
- Emergency event logging for audit/history.
- Emergency photo support through FileProvider and CameraX-related dependencies.

### Continuous Safety Monitoring

- Foreground safety service for background monitoring.
- Location tracking through Google Play Services.
- Accelerometer and gyroscope processing.
- Triple-shake detection with confirmation.
- Voice trigger support for phrases such as "Help" or "Emergency".
- Wake lock support for active monitoring periods.
- Dynamic safety mode changes based on risk state.

### Maps, Risk, and Routes

- Leaflet-based map rendered inside Android WebView.
- OpenStreetMap tiles with online/offline status indicator.
- Risk Map for crime/disaster zones and nearby safety services.
- Safe Routes with route alternatives, risk scoring, distance, duration, and safest-route highlighting.
- Professional safest-route overlay showing tick mark, safety score, risk level, distance, time, and routes analyzed.
- Recommended transport card for cab, auto, bike taxi, public bus, walking, or avoid-travel states.
- Route navigation handoff to Google Maps.

### Journey Safety

- Unified safe journey flow from the phone app.
- Journey session tracking with phases such as walking, in vehicle, train transit, station exit, last-mile walk, arrived, and emergency.
- Journey timeline storage and review.
- Journey sharing with contacts.
- Companion/guardian dashboard for active journey status.
- Activity recognition support for journey transitions.

### Advanced Safety Tools

- Live SOS tracking session with timeline updates.
- Emergency timeline export/share support.
- Offline safety mode with bundled datasets and map cache summary.
- Fake call scheduler.
- Safety check-in timer.
- Trusted journey timer that can escalate if missed.
- Emergency drill mode that validates the local flow without sending real SMS or starting a real call.
- Fake Battery Dead Mode that keeps protection running while the phone appears inactive.

### Wear OS Companion

- Wear OS app module with Compose for Wear OS.
- Quick SOS and silent alert actions from the watch.
- Phone-watch communication through Wearable Data Layer paths under `/safepulse`.
- Synced safety state, emergency contacts, location, heart-rate data, live tracking state, journey state, and check-in state.
- Wear foreground safety service.
- Watch tile and complication support.
- Boot receiver for restoring watch-side service behavior.

### Onboarding and Support Screens

- Onboarding flow for first launch.
- User manual screen.
- Permission health screen.
- Settings screen.
- Event logs screen.
- Drawer navigation for key app areas.

## Architecture

SafePulse follows a layered Android architecture:

- UI layer: Jetpack Compose screens, navigation, reusable cards, map wrappers, and Wear UI.
- ViewModel layer: screen state, user actions, and coroutine orchestration.
- Domain layer: safety/risk engines, journey models, route risk models, transition logic, and recommenders.
- Data layer: Room database, DAOs, repositories, DataStore/preferences, bundled datasets, and local file-backed timeline data.
- Service layer: foreground safety service, emergency manager, location tracker, journey tracking, phone-watch sync, wearable listeners, battery-dead mode, and receivers.
- Worker layer: background WorkManager jobs for safety checks, data refresh, and check-ins.

## Project Structure

```text
.
|-- app/                         # Main Android phone app
|   |-- src/main/java/com/safepulse/
|   |   |-- data/                # Room DB, repositories, preferences, security
|   |   |-- domain/              # Safety, route, journey, transition, risk models/engines
|   |   |-- ml/                  # Voice trigger and audio processing
|   |   |-- service/             # Foreground service, emergency, location, sync, receivers
|   |   |-- ui/                  # Compose screens, map, journey, components, theme
|   |   |-- utils/               # Permission, notification, constants, diagnostics
|   |   |-- worker/              # WorkManager background jobs
|   |   |-- MainActivity.kt
|   |   `-- SafePulseApplication.kt
|   |-- src/main/assets/         # Maps, safety datasets, model, Leaflet assets
|   `-- build.gradle.kts
|-- wear/                        # Wear OS companion app
|   |-- src/main/java/com/safepulse/wear/
|   |   |-- complication/        # Watch complication provider
|   |   |-- data/                # Wear data paths and preferences
|   |   |-- presentation/        # Wear entry/activity/navigation/view model
|   |   |-- service/             # Wear safety and data listener services
|   |   |-- tile/                # Wear tile service
|   |   `-- ui/                  # Wear screens/theme
|   `-- build.gradle.kts
|-- gradle/                      # Gradle wrapper files
|-- master_dataset.csv           # Source/local dataset copy
|-- settings.gradle.kts          # Includes :app and :wear
`-- build.gradle.kts             # Root Gradle config
```

## Tech Stack

- Language: Kotlin
- UI: Jetpack Compose, Material 3, Compose for Wear OS
- Navigation: Navigation Compose
- Background work: WorkManager
- Database: Room with KSP
- Preferences: DataStore and encrypted preferences
- Maps: Leaflet WebView, OpenStreetMap assets, Google Maps/Location dependencies
- Routing: Google Directions API dependency and OSRM routing from Leaflet map HTML
- Location: Google Play Services Location
- Wear sync: Google Play Services Wearable Data Layer
- ML/audio: TensorFlow Lite, TensorFlow Lite Support, speech/keyword trigger modules
- Camera/photo: CameraX dependencies and FileProvider
- Networking: Retrofit, Gson, OkHttp logging interceptor

## Requirements

- Android Studio with Kotlin and Android Gradle Plugin support.
- JDK 17.
- Android SDK 34.
- Phone app minimum SDK: 26.
- Wear app minimum SDK: 30.
- A Google Maps API key for route and map-related integrations that use `BuildConfig.MAPS_API_KEY`.
- Physical Android device is recommended for SMS, phone call, sensors, location, background service, and Wear sync testing.
- Wear OS emulator or physical Wear OS watch for the `wear` module.

## Setup

1. Clone or open the project in Android Studio.

2. Create `local.properties` in the project root if it does not already exist.

3. Add your Android SDK path and Maps API key:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=your_google_maps_api_key_here
```

4. Sync Gradle.

5. Let Android Studio install any missing SDK/build tools.

6. Grant runtime permissions on the device when the app requests them.

## Build and Run

Build the phone app:

```bash
./gradlew :app:assembleDebug
```

Build the Wear OS app:

```bash
./gradlew :wear:assembleDebug
```

Run unit tests:

```bash
./gradlew test
```

Run a focused app test task:

```bash
./gradlew :app:testDebugUnitTest
```

On Windows PowerShell, use:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :wear:assembleDebug
```

## Configuration

### Maps API Key

Both modules read `MAPS_API_KEY` from root `local.properties` and expose it through `BuildConfig.MAPS_API_KEY`. The phone app also passes the key as a manifest placeholder.

Never commit a real API key to source control.

### Safety PINs

PIN and duress flows are handled through the safety/security preference layer. The UI references normal cancel behavior and duress behavior. Review Settings and Advanced Safety screens when changing this flow.

### Bundled Dataset Preload

`SafePulseApplication` initializes the Room database and preloads bundled data through `SafePulseDatabase.preloadSampleDataIfNeeded(...)`.

Preloaded entities include:

- Hotspots
- Unsafe zones
- Emergency services
- Journey sessions/events and transition events as users interact with the app

## Data and Assets

Important app assets live in `app/src/main/assets/`:

- `leaflet_map.html`, `leaflet.js`, `leaflet.css`, `images/` - map rendering and Leaflet resources.
- `hotspots.json` - sample hotspot data.
- `unsafe_zones.json` - unsafe zone data.
- `crime_risk_zones.json` - risk-zone map data.
- `disaster_risk_zones.json` - disaster map data.
- `emergency_services.json` - emergency service preload data.
- `emergency_facilities.geojson` - larger facility dataset.
- `police_stations_india.json` - police station dataset.
- `hospitals_india.json` - hospital dataset.
- `master_dataset.csv` - master safety dataset bundled in assets.
- `keyword_spotting_model.tflite` - TensorFlow Lite model asset for voice trigger workflows.

There is also a root-level `master_dataset.csv`, likely useful for dataset inspection, preprocessing, or external analysis.

## Permissions

The phone app declares permissions for:

- Internet and network state.
- Fine, coarse, and background location.
- Activity recognition.
- SMS and phone calls.
- Notifications and full-screen intent.
- Foreground services for location and microphone.
- Boot completed and wake lock.
- Vibration.
- Microphone/audio settings.
- Camera and optional camera features.

The Wear OS app declares permissions for:

- Watch hardware feature support.
- Location.
- Body sensors and activity recognition.
- Vibration and microphone.
- Notifications.
- Foreground services for data sync, health, and microphone.
- Wake lock and boot completed.
- Internet for phone communication.

Runtime behavior depends on the user granting the relevant permissions. The Permission Health screen exists to help users identify missing permissions.

## Important Runtime Components

- `SafePulseApplication` initializes shared repositories, database, journey session manager, journey sharing, transition risk engine, notification channels, and bundled data preload.
- `MainActivity` controls onboarding, theme, navigation, drawer destinations, and background worker scheduling.
- `SafetyForegroundService` performs ongoing monitoring, sensor processing, location updates, voice/shake emergency triggers, emergency countdowns, and SOS execution.
- `EmergencyManager` handles SOS message/call workflows and nearby emergency service alerts.
- `LocationTracker` provides current location updates for safety and journey flows.
- `JourneyTrackingService` connects safety state, location updates, route/journey phases, and emergency events.
- `PhoneWearListenerService` and Wear-side listener services handle phone-watch messages and synced state.

## Navigation Map

Main phone routes include:

- Home
- Safe Routes
- Risk Map
- Journey Timeline
- Guardian Dashboard / Companion Journey
- Advanced Safety
- Event Logs
- Settings
- Permission Health
- User Manual
- Battery Dead Mode overlay when enabled

## Testing

Existing test coverage includes voice keyword matcher tests under:

```text
app/src/test/java/com/safepulse/ml/
```

Suggested testing checklist before release:

- App launches and onboarding completes.
- Required permissions can be requested and granted.
- Foreground safety service starts and stops correctly.
- Manual SOS sends the expected test message/call behavior in a controlled environment.
- Silent SOS avoids call flow.
- Voice trigger and shake trigger work on a physical device.
- Safe Routes renders the map, destination search, route overlay, and transport recommendation.
- Risk Map loads bundled risk/emergency datasets.
- Journey start, share, timeline, complete, and cancel flows work.
- Wear app connects, syncs status, and can trigger supported actions.
- Battery Dead Mode can be entered and exited through the configured flow.

## Troubleshooting

### Gradle cannot access the wrapper cache

If Gradle fails with a lock/cache access error under `.gradle`, close other Gradle/Java processes or retry from Android Studio. On restricted environments, Gradle may need permission to access the user-level Gradle cache.

### Maps or routes are blank

- Check `MAPS_API_KEY` in `local.properties`.
- Confirm the device has network access.
- Confirm WebView is available/enabled on the device.
- Check that `leaflet_map.html`, `leaflet.js`, `leaflet.css`, and image assets are packaged.

### SOS does not send SMS or start calls

- Verify `SEND_SMS` and `CALL_PHONE` permissions.
- Test only on a physical phone with SIM/telephony support.
- Ensure emergency contacts are configured.
- Silent SOS intentionally avoids the call flow.

### Voice trigger does not work

- Verify microphone permission.
- Confirm foreground service microphone mode is allowed.
- Test on a real device in a quiet environment.
- Check that `keyword_spotting_model.tflite` is present in assets.

### Wear sync does not work

- Confirm phone and watch are paired.
- Install matching app versions on phone and watch.
- Ensure Google Play Services is available on both devices.
- Check Wearable Data Layer paths under `/safepulse`.

## Development Notes

- Keep real API keys, signing credentials, and private contact data out of source control.
- Large safety datasets are bundled in assets; avoid duplicating or unnecessarily transforming them in commits.
- Room currently uses `fallbackToDestructiveMigration()` for prototype-style schema changes.
- Test emergency flows in a safe, controlled environment and avoid sending real alerts during development.
- The app has both phone and watch application IDs set to `com.safepulse`, so install/deploy with care depending on target device type.

## Safety Notice

SafePulse is a safety-assistance prototype/application. It can support emergency workflows, risk awareness, and communication, but it must not be treated as a guaranteed emergency service. Real-world safety depends on device permissions, battery, connectivity, location accuracy, SMS/call availability, operating-system background limits, and user configuration.
