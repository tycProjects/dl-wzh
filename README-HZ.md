# HZ – Minecraft Mod Builder for Android

HZ is built on the Termux app source. It installs Java 8/11/17/21 and build tools into the
Termux environment, lets you import a mod project, edit it, and build it with one button.
An AI agent (Groq / OpenRouter) can read and edit project files and run the build.

## Build the APK
Needs Android SDK (compileSdk 36), NDK 29.0.14206865 and internet (Termux bootstrap zips are
downloaded by Gradle on first build):

    ./gradlew assembleDebug

APK: `app/build/outputs/apk/debug/`

## What was added (all in `app/src/main/java/com/termux/hz/`)
- `HzActivity` main UI: Project / Editor / Agent / Console tabs, **Build** button top-right
- `SetupActivity` first-run installer (bootstrap, git/curl/zip, JDKs, Gradle config)
- `JdkManager`, `ProjectInfo`, `BuildManager` JDK selection by Minecraft version, Gradle/Maven build
- `EditorPanel`, `CodeEditText`, `Highlighter` tabs, line numbers, undo/redo, find/replace, symbol bar
- `AgentPanel`, `LlmClient`, `AgentTools` streaming chat, tool calling, approvals, revert
- `HzSettingsActivity` API keys, agent permissions, build options, Java installs

Changes to Termux files: `TermuxInstaller` made public, app name set to HZ, launcher moved
from `TermuxActivity` to `HzActivity` (manifest), `Theme.HZ` + icons + menu resources.

## Notes
- Package stays `com.termux` because Termux bootstrap binaries have that path hard-coded.
- Java 17/21 install from the Termux repo. Java 8/11 are tried via tur-repo; if unavailable, set a
  custom Android-compatible JDK archive URL in Settings > Java.
- The HZ Java sources were compile-checked with javac against hand-written Android API stubs (0 errors)
  and the pure-logic classes were run in a JVM. A full Gradle/Android build still needs the Android SDK/NDK.
