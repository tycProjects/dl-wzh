# Stage 32 — HomeScreen unresolved aiProvider fix

- Fixed `HomeScreen.kt` in the provider preset selection fallback.
- The nested `firstOrNull` lambda previously used an implicit `it` for both the preset candidate and the settings object. Kotlin resolved `it.aiProvider` against the preset candidate, which has no `aiProvider` property.
- Renamed the inner lambda parameter to `candidate`, preserving the outer settings `it.aiProvider` lookup. This is a scope/shadowing correction; provider selection and custom Base URL behavior are otherwise unchanged.
- Static source check confirms the nested lambda now uses `candidate` and the settings update still reads `it.aiProvider`.
- Build verification could not run in this environment: Gradle wrapper attempted to download Gradle 8.9 and network DNS resolution for `services.gradle.org` failed (`UnknownHostException`). This ZIP is not represented as a build-verified artifact.
