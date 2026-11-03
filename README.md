# AetherIsland — Foundation 0.1.0

A clean Android/Kotlin foundation for a custom Dynamic Island. The uploaded projects were inspected as technical references; this project does not copy their application layouts or import their broad activity scope.

## Selected configuration
- Minimum Android: API 26 (Android 8.0)
- Compile/target SDK: 35
- Kotlin 2.0.21, AGP 8.9.2, JVM 17
- Jetpack Compose + Material 3
- Liquid Glass profile: Adaptive (foundation UI currently uses a lightweight preview; production backdrop/shader integration is a later milestone)
- DataStore Preferences, Coroutines, StateFlow-ready domain models

## Included foundation
- New package namespace: `com.aetherisland`
- Dashboard preview and adaptive glass intensity control
- Island activity/state models and dispatcher preserving media state under WhatsApp banners
- Strict WhatsApp package filter (`com.whatsapp`, `com.whatsapp.w4b`)
- Notification listener service shell
- Lyrics domain models, repository contract and timestamp synchronization helper
- Independent media snapshot store so transient banners cannot destroy playback state
- Overlay service lifecycle shell
- DisplayCutout model
- DataStore settings model for cutout calibration, glass intensity and reduced motion
- Unit tests for dispatcher behavior and WhatsApp-only filtering

## Explicitly not implemented yet
This is a foundation milestone, not a finished APK. WindowManager rendering, MediaSessionManager controller discovery, WhatsApp MessagingStyle parsing/RemoteInput actions, lyrics providers/synchronization, shader-based backdrop/refraction, full gesture handling, calibration UI and recovery logic are scheduled for subsequent milestones.

## Build
Open this folder in Android Studio (JDK 17), allow Gradle sync, then run the `app` configuration. Or use an installed Gradle 8.11+ distribution with `gradle :app:testDebugUnitTest :app:assembleDebug`.

No Gradle wrapper JAR is bundled in this initial source package. Android Studio can import the Gradle project using its configured Gradle installation.

## Permissions to enable manually during development
- Display over other apps (`SYSTEM_ALERT_WINDOW`) before enabling overlay rendering.
- Notification access for `WhatsAppNotificationListener` before receiving WhatsApp events.

The app intentionally does not request phone, alarm, navigation, Bluetooth, contacts, calendar, or unrelated notification permissions.

## Reference audit notes
- Dynamic Island 1.6.1: useful concepts include media session observation, artwork metadata, lyrics repository, state bus and overlay lifecycle. Its broader call/alarm/navigation/payment/progress activity scope is excluded.
- AndroidLiquidGlass 2.0.1: the included upstream LICENSE is Apache-2.0. Its Backdrop library is a separate Compose Multiplatform library; no source was copied into this foundation. Integration should be version- and GPU-tested before inclusion.
