# How to get the NOMAN GO app (APK)

You need: a free GitHub account, this project (`NomanGo-Android.zip`), about 10 minutes.
Nothing has to be installed on the phone or computer. GitHub builds the APK for you.

## A. From a phone (no folders to upload)

1. Open github.com, sign in, tap **New repository**, give it any name, tap **Create repository**.
2. Tap **Add file > Upload files**, choose **NomanGo-Android.zip** (the zip itself, do not unzip it),
   then **Commit changes**.
3. Tap **Add file > Create new file**. In the name box type exactly:
   `.github/workflows/build.yml`
   Paste the full text of `PHONE-BUILD/build-from-zip.yml` (it is inside the zip) and **Commit changes**.
4. Open the **Actions** tab, tap **Build APK from zip**, then **Run workflow > Run workflow**.
5. Wait 6 to 10 minutes until the run has a green tick. Open it, scroll to **Artifacts**,
   download **NomanGo-APK**, unzip it, and install `app-release.apk`
   (Android asks once to allow installing from your browser or file manager, allow it).

## B. From a computer

1. Unzip `NomanGo-Android.zip`.
2. Create a GitHub repository and upload the **contents** of the folder (so `app/`, `build.gradle.kts`
   and `.github/` sit at the top of the repository).
3. Actions > **Build APK** > Run workflow (it also runs on every upload). Download **NomanGo-APK**.

Or open the folder in Android Studio (Hedgehog or newer, JDK 17), choose the **release** build variant and Run.

## Which APK?

* `NomanGo-APK` (release): not debuggable, fastest, **use this one**.
* `NomanGo-debug-APK`: only as a fallback if the release APK ever misbehaves. It is slower.

## Optional: same font as the HTML

The HTML uses the Outfit font. Put a static `Outfit-Bold.ttf` into `app/src/main/res/font/` and rename it
`outfit_bold.ttf` (create the `font` folder). The toolbar text then matches. Without it the system font is used.

## If the build fails

Open the failed run, open the red step, copy the last 30 red lines and send them. Usually it is a
one-line fix.
