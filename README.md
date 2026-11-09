# AgentCoder Android

An Android-native AI coding workspace starter built with Kotlin and Jetpack Compose.

## Current scaffold
- English dark UI with original pixel-inspired navigation glyphs.
- Workspace, Providers, Model Library, API Keys, Files, Terminal and Settings screens.
- Provider catalog placeholders and custom-provider dialog.
- Android project structure prepared for secure credential storage, model discovery and agent runtime integration.

## Build
Open this folder in Android Studio (JDK 17 recommended), allow Gradle sync, then run the `app` configuration on an Android 8.0+ device or emulator.

To build an APK:
```bash
./gradlew assembleDebug
```
The output will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Important implementation status
This is a UI and architecture starter, not a finished coding agent. Provider rows and model entries are sample UI data. The current custom-provider dialog is not yet wired to persistent storage or network calls. Do not enter production API keys into this scaffold.

Next engineering milestones:
1. Add encrypted API credential storage backed by Android Keystore.
2. Add provider adapters and live model discovery (including OpenAI-compatible custom endpoints).
3. Add chat streaming, tool-call parsing and agent task planning.
4. Add Storage Access Framework folder access and safe file diff/apply.
5. Add a remote execution worker for full build/debug; Android local execution is limited by the Android sandbox.
6. Add explicit permission gates, command allowlists, audit logs and rollback before enabling agent-driven execution.
