# Vortex Android starter project

This is a starter Android app with a black/purple interface, a floating overlay, and a local text editor. It does not inject scripts into Roblox or bypass Roblox protections.

## Build an APK without owning a PC

The repository includes a GitHub Actions workflow at `.github/workflows/build-apk.yml`.

1. On your phone, create/sign in to a GitHub account at https://github.com.
2. Create a new **public or private repository** named `VortexAndroid`.
3. Upload the contents of this ZIP into the repository (extract the ZIP first if GitHub's upload page does not accept the ZIP as source files). Make sure `.github/workflows/build-apk.yml` is included.
4. Open the repository's **Actions** tab and select **Build Vortex APK**.
5. Tap **Run workflow** if it is available, then wait for the run to finish successfully.
6. Open the completed workflow run and download the `Vortex-debug-apk` artifact. Extract the downloaded artifact ZIP to get `app-debug.apk`.

This creates a debug APK for testing, not a signed release for Google Play. GitHub's mobile interface may make uploading a whole project folder awkward; if it does, use GitHub's browser upload or a supported cloud IDE. Do not rename the project ZIP to `.apk`.

## Local build

Open the project in Android Studio with Android SDK installed, then run `gradle assembleDebug`. The output is `app/build/outputs/apk/debug/app-debug.apk`.
