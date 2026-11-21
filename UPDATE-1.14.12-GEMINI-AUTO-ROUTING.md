# Android Island 1.14.12 — Gemini automatic model routing

This update builds on the supplied 1.14.11 project. It adds live Gemini capability discovery and task-aware model selection, preserves the existing Android-native speech recognizer and TTS paths, and includes the Kotlin nullable-node build fix in `IslandAccessibilityService`.

See `GEMINI-AUTO-MODEL-ROUTING.md` for the task-to-model routing table and current integration boundaries.

Build verification remains pending: the Gradle 8.9 wrapper could not download its distribution in the build environment because `services.gradle.org` DNS resolution failed.
