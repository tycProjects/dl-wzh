# Android Island AI Feature Audit — ZIP baseline 1.14.8

## Scope

Audit performed against the source tree supplied as `AndroidIsland-1_16_2-ponytail-dedupe.zip`.
The archive label says 1.16.2, but the checked-in Gradle and `VERSION.txt` metadata identify
this source snapshot as 1.14.8 (versionCode 138). This report preserves that distinction.

## Verified source-level foundations

- Chat/session history, staged image/document attachments, cancellation, provider fallback,
  streaming transport, and an observe-act loop exist in `ai/AiSession.kt` and `ai/AiClient.kt`.
- Structured action protocol and policy parsing exist in `ai/AiActionProtocol.kt`,
  `ai/IntentResolver.kt`, `ai/AgentLoopPolicy.kt`, and `ai/ShellPolicy.kt`.
- Foreground screen text capture uses the user-enabled Accessibility service in
  `ai/ScreenReader.kt`; screenshot capture uses Android's Accessibility screenshot API on API 30+.
- Image attachments are sent as image parts through the AI client; image generation has a
  separate client/parser path.
- Speech-to-text uses Android `SpeechRecognizer` in `ai/voice/SpeechInput.kt` and the composer.
- Device diagnostics and Shizuku shell bridge exist in `ai/DeviceDiagnostics.kt`,
  `system/ShizukuShell.kt`, and related policy classes.
- Web search, model discovery, provider selection, and a WhatsApp notification listener exist.
- Tests already cover action protocol, agent loop policy, shell policy, model fallback,
  model filtering, screen summary, key ring, and several device/UI policies.

These findings confirm source-level implementation, not successful runtime behavior on a physical
phone. Provider access, permissions, hardware, OEM overlay behavior, and remote model support
still require device/network testing.

## Important configuration defect corrected in this pass

- `app/build.gradle.kts` declared `minSdk = 35`, contradicting the stated Android 8.0/API 26
  requirement. It is now set to API 26. The project uses compileSdk 35, so API 30/34-only
  screenshot/window APIs must remain guarded at runtime; the existing source has guards around
  the relevant screenshot calls.

## Gaps found for the requested Galaxy-AI-style expansion

- No dedicated, user-managed long-term memory repository was found. Chat history is not a
  substitute for explicit, editable, consent-based memory.
- No TextToSpeech output implementation was found. Existing voice support is primarily speech
  recognition/input; two-way voice output is incomplete.
- No dedicated translation workflow, source/target language preference store, or offline
  translation model/resource was found. Translation can be requested from the general chat model,
  but that is not a standalone translation subsystem and is online/provider-dependent.
- Notification context is present through the WhatsApp listener, but a distinct AI notification
  digest/priority workflow needs further integration and careful state arbitration.
- Vision support is implemented through image request parts, but per-model capability metadata
  and provider-specific multimodal compatibility need stronger validation than simple model-ID
  filtering.
- AI settings store API keys in Preferences DataStore. They are not plaintext log output in the
  inspected paths, but the current storage is not encrypted at rest; migration to Android Keystore
  backed encryption is a separate security task.
- Voice recognition hardcodes `id-ID` in `SpeechInput`; automatic device-language selection and
  user language controls are incomplete.

## Build validation limitation

`./gradlew testDebugUnitTest --no-daemon` could not start because the Gradle 8.9 distribution was
not cached and this environment could not resolve `services.gradle.org` (UnknownHostException).
Therefore no compile, unit-test, APK, or physical-device success is claimed from this pass.
