# On-phone Kotlin / Java builds (v334)

Builds a Kotlin or Java project on the phone with Termux. No server, no GitHub, no build used.

## How it works
1. The app starts a tiny web server on 127.0.0.1 (this phone only; every address has a random token).
2. It asks Termux to run `assets/termux/wzh_build.sh` (Termux "RUN_COMMAND" intent).
3. The script downloads the project and android.jar from the app, then runs
   aapt2 compile + link, kotlinc, javac, jar, d8, zip. It posts steps and the log back.
4. The unsigned APK is sent back to the app, which aligns and signs it with the same key Quick build uses.

## Native side
`tool/android_setup.py` now also adds (safe to run twice):
- `com.termux.permission.RUN_COMMAND` and a `<queries>` entry for `com.termux` in the manifest
- a `wzh/termux` MethodChannel in MainActivity.kt
Your existing server pipeline already runs this script, so the next Flutter build picks it up.

## One-time setup on the phone (Build > Kotlin/Java project > On this phone > Set up)
1. Install Termux from F-Droid (not the Play Store).
2. Tap Allow in the app.
3. In Termux paste the copied line, press Enter, close and reopen Termux.
4. Tap Install tools (Termux opens, installs openjdk, kotlin, aapt2, d8, zip, unzip, curl).
5. Tap Check. All tool chips should be green.

## Limits of this first version
- Only the Android platform plus .jar files in the project's libs/ folder. androidx / Material / Maven
  dependencies are NOT downloaded yet (next step: a dependency downloader). Use the server build for those.
- The app name, icon and permissions from the code editor app are not applied yet; the project's own
  manifest is used. The package id is the editor app's "Final ID".
- Keep the app open while it builds. Kotlin compiles take roughly 1 to 4 minutes on a phone.

## Test order
1. Set up steps 1 to 5 until Check is green.
2. Build a tiny Java-only project (one Activity, no androidx).
3. Then a Kotlin one. Failures show the compiler's error lines on the failed screen.
4. Nothing here could be compiled or run in the chat (no Flutter, Dart, Termux or Android SDK), so all of it is untested on a device.

## Libraries (androidx, Material, any Maven dependency)

The on-phone build now resolves the module's Gradle dependencies the way the server's Gradle does:
reads `build.gradle(.kts)`, `gradle/libs.versions.toml`, `$variables`, `platform(...)` BOMs and the
`maven { url }` repos; downloads POM/JAR/AAR files (cached in `~/wzh_build/m2`); picks the highest
version per library; links library resources (`--extra-packages` gives each library its own `R`);
merges library manifests (providers, services, permissions); generates `BuildConfig`.
Needs internet on the first build of a project and the `python` Termux package (installed by
"Install tools", or automatically on the first build).

Not supported on the phone yet (the log prints a WARN line): Jetpack Compose, kapt/ksp/annotationProcessor,
viewBinding/dataBinding, other Gradle modules. Use the server build for those.

## Build Doctor on the phone

`assets/termux/wzh_doctor.js` is the server's Build Doctor (the repair rules from server.js, extracted with
their helpers) as one node script. The build runs it before the build (rules that need no log) and, when
Kotlin, Java or aapt2 link fails, reads that step's log, repairs the project (missing imports, moved
framework classes, legacy support libraries, resource mistakes, Compose/Kotlin API fixes and so on) and builds
again, at most 3 times. Needs `nodejs` in Termux (installed by "Install tools", or on the first build); without
it the build goes on without the doctor. To refresh it after server.js changes, regenerate the file from
the new server.js (the same extraction: runBuildDoctor and everything it uses, minus the Express/bandwidth parts).

## Crash-on-open guard (v343)
- The Kotlin runtime is now taken from the jar that really contains `kotlin/jvm/internal/Intrinsics` (before, `find | head -1` could pick a jdk7/jdk8 shim, so the app crashed the moment it opened with NoClassDefFoundError). If no such jar exists the build stops with a clear message.
- Before the app is sent back, the build checks that the manifest's start screen (the MAIN/LAUNCHER activity) is really inside the built code, and fails with a plain message if it is not (wrong package / namespace).
- Untested on a device: no Termux or Android SDK was available here.

## v344
- Build Doctor: "'x' hides member of supertype 'Activity' and needs an 'override' modifier" (e.g. a private fun setProgress in an Activity) is fixed by renaming the app's own function to xUi inside that file.
- Build Doctor: a lone '$' is only escaped when kotlinc actually reports "Incorrect template entry" (it is valid Kotlin otherwise).
- kotlinc runs with jansi disabled (-Djansi.passthrough=true) so Termux no longer prints the libjansi / libc.so.6 UnsatisfiedLinkError.
- Untested on a device.

## v345: Terminal (Termux)
- New Terminal screen in the code editor (top bar button and the menu): lib/screens/code_terminal.dart, lib/termux/termux_session.dart, assets/termux/wzh_term.sh.
- Every command runs in Termux and its output is shown in the app (colors supported, progress bars rewrite their line). The folder and exported variables stay between commands.
- With a project open, Termux gets a copy in ~/wzh_project (one way: changes made in the terminal are not sent back to the editor).
- Not supported: typing into a running command (stdin), full-screen programs (vim, nano, htop). Ctrl+C stops the running command.
- The shell loop was tested against a fake local server; the Dart side and UI could not be compiled here (no Flutter), so expect to fix small compile errors on the first build.

## v346
- Terminal: a command that prints nothing (mkdir, cd...) now ends with "[done, 0.3s]", other commands show their time, and the Run button turns into a spinner while a command runs.

## v347
- Setup page step 4 (Install tools) now runs in the app's own Terminal screen instead of opening a Termux window, and checks the tools when you come back. CodeTerminalPage takes initialCommand / initialLabel.

## v348: Terminal redesign
- Each command is a card (command, folder, time, output, Copy). Failed commands explain themselves in plain words (command not found, no such file, permission denied, no internet...).
- "Ideas" sheet with beginner commands in plain language (it fills the box; you press the arrow). "How this works" in the menu. The arrow turns into a Stop button while a command runs.
- Keyboard: the input used keyboardType visiblePassword, which made Android show the keyboard in secret/incognito mode. It is now a normal text keyboard (no autocorrect, no smart quotes/dashes, so commands stay exact).
- Untested on a device (no Flutter compiler here).
