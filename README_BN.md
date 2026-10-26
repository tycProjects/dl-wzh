# DARK TK — Native Kotlin Android Project

**Package:** `com.tkofficial.assistant`  
**Language:** Kotlin only (no Java source files)  
**AGP:** 8.6.1 • **Gradle:** 8.7 • **Compile SDK:** 35 • **Min SDK:** 26  
**JDK:** JDK 17 is the conservative choice for this AGP; JDK 21 is also supported by Gradle 8.7/AGP 8.6 in typical setups. Kotlin bytecode target is JVM 17.

## Included working features
- Native Android UI with dark dashboard, responsive navigation and subtle transitions.
- Gemini and OpenAI text API calls (requires internet and a valid provider API key).
- Provider/model settings, saving and deleting keys.
- Android speech-recognition intent and Android Text-to-Speech test profiles (profile selection adjusts pitch/rate, not separate voice models).
- Local text editor that saves/loads files in app-private `files/Dark TK Lood`.
- App-private file/folder listing, create folder, delete file/folder, share text file.
- Clear session chat history; honest setup/error messages when keys/network are missing.

## Not implemented (do not assume these exist)
- No backend proxy, secure hardware-backed API key vault, account sync, encrypted memory database, full compiler/IDE, arbitrary code execution, HTML preview sandbox, web search, weather/news, image/document AI, cloud voice provider, reminders, or backup/restore. Those require additional work and/or credentials/services.
- Chat history is in-memory only. Keys are in ordinary app SharedPreferences. This is a prototype foundation, not a production security boundary.

## Build in AndroidIDE
1. Extract the ZIP fully into a writable folder, e.g. `/storage/emulated/0/AndroidIDEProjects/DARK_TK_Kotlin`.
2. Open the **root** folder containing `settings.gradle`, `build.gradle`, `gradle.properties`, and `app/`.
3. Let AndroidIDE resolve/download AGP/Kotlin plugin and Gradle 8.7. Internet is required on first sync.
4. Use JDK 17 as the first choice. JDK 21 may also work with this toolchain; changing JDK will not fix a missing Gradle launcher.
5. Build/Run. If AndroidIDE reports `./gradlew: No such file or directory`, it is launching a wrapper command outside the project or its Gradle runner is misconfigured. Use AndroidIDE's project Build/Run action after opening the correct root; do not press an unrelated terminal run command.

This archive intentionally does not include a fake `gradlew` or a fabricated wrapper JAR. AndroidIDE should manage/supply the Gradle runtime during sync/build. If your AndroidIDE build mode strictly requires `./gradlew`, generate a real wrapper from a desktop Gradle installation (`gradle wrapper --gradle-version 8.7`) or use AndroidIDE's supported wrapper setup.

## API notes
- Gemini endpoint: `v1beta/models/{model}:generateContent`
- OpenAI endpoint: `/v1/chat/completions`
- Provider API pricing, model access, regional availability, and API formats can change. A provider may reject an unavailable model or invalid key; the app displays the HTTP error.
- Never paste API keys into ChatGPT or publish them in source control. For public release, route requests through your own authenticated backend instead of shipping provider secrets in an APK.
