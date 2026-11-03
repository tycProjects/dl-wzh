# Reference audit — 2026-11-03

## Dynamic-Island--1.6.1.zip
Inspected archive structure and key project files. The app module uses Kotlin/Compose and minSdk 26. The manifest includes permissions and components for phone calls, alarms/timers, wake behavior, Bluetooth, contacts, payment/NFC, accessibility overlay and general notification listening. These are not inherited into AetherIsland. Useful concepts for later clean-room implementation: media metadata/session monitoring, artwork palette extraction, lyrics repository and timestamped lyrics, state bus, compact/expanded UI split, overlay lifecycle. The new foundation retains the lyrics domain contract and timestamp synchronizer, but does not copy provider/network implementation yet.

## AndroidLiquidGlass-2.0.1.zip
Inspected README, module build configuration and LICENSE. The project describes Backdrop as a Compose Multiplatform Liquid Glass effect library and declares Apache License 2.0. It is not a ready-made Dynamic Island UI. AetherIsland currently does not vendor or copy its implementation. Later integration should isolate required Android-compatible effects and provide adaptive GPU fallback.

## Scope decisions
Only media playback and WhatsApp notifications are first-class activities. WhatsApp package allowlist is explicit. Media state is modeled independently from transient WhatsApp banner state. No call/alarm/navigation/charging/weather/calendar/other-app activity modules are created.
