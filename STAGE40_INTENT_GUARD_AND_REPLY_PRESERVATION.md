# Stage 40 — Explicit Intent Guard & Original Reply Preservation

## Changes

- Device actions are now authorized only when the user's own message begins with a clear imperative/request form (including common polite prefixes and supported Indonesian/English verbs).
- Mentioning an app name, describing an app, or discussing how an action works no longer qualifies as a device command by itself.
- The deterministic command router is gated by the same explicit-intent check, preventing substring matches such as explanatory text containing `buka` from launching an app.
- Model-proposed actions remain rejected for ordinary chat/questions, even if the model hallucinates an action envelope.
- When providers wrap a tool call with readable prose, the parser preserves surrounding user-facing text while stripping internal tool markup and payload JSON.
- When an invalid envelope triggers a format-repair retry, the original readable answer is retained. A failed format retry no longer replaces that answer with a generic error for ordinary conversation.
- Added regression tests for app-name mentions, descriptive sentences, polite imperative requests, and prose surrounding tool markup.

## Verification

- Source-level inspection completed.
- Gradle unit tests attempted with `./gradlew testDebugUnitTest --offline`.
- Test execution could not start because Gradle 8.9 is not cached and the environment cannot resolve `services.gradle.org` (`UnknownHostException`).
- Device behavior and live provider responses remain unverified until tested on an Android device.
