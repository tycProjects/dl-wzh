# WZH 2 APK (on-device builder)

An Android app: tap **Build your APK**, add your source (Upload an HTML file / ZIP / folder, or a
Website link), customize (package, version, icon, orientation...) and it builds a signed, installable
APK **on your phone**. No server, no account. Has a side menu with light/dark mode and a short guide, and a Recent builds list on the home screen
(tap an entry to install it again, save it to Downloads, or delete it).

## Get the app (phone only, free)
1. Create a free GitHub account and a **new repository**.
2. Upload everything from this zip (unzip first; **Add file → Upload files**).
   The hidden `.github` folder must go up too. If your phone skips it: **Add file → Create new file**,
   name it `.github/workflows/build.yml`, paste in the contents of that file from the zip.
3. Open the **Actions** tab and wait ~5 minutes for "Build WZH 2 APK app" to finish
   (if it asks you to enable workflows, tap enable).
4. Open **Releases** (right side of the repo page), download `WZH2APK.apk`, install it.
   (Allow "install unknown apps" for your browser.)

## Use it
Open **WZH 2 APK** → Build your APK → Step 1 (source) → Step 2 (customize) → Build APK → Install.
Website mode wraps a live link instead of bundled files, so that app needs internet.
Your site needs `index.html` at the top. Put CSS/JS/images/fonts inside it. Online links won't work offline.

## How it works
- `template/` is a tiny WebView app that serves `assets/www` as a fake https site.
- `builder/` embeds the compiled template, then for each project: patches the binary manifest
  (package, name, version, internet permission), swaps the icon, adds your site + config,
  aligns the ZIP and signs it (v1+v2) using Google's apksig.
- All generated apps are signed with the one key bundled in `builder/src/main/assets/`.
  That's fine for personal use; for the Play Store you'd need your own release key.

## Building on the phone (Android Code Studio / AndroidIDE)
1. Unzip, then open the **`html-to-apk` folder itself** (the one that contains `settings.gradle.kts`,
   `gradlew` and the `builder` and `template` folders), not its parent.
2. Let the project sync finish. If it says "Project initialization failed", swipe up and open
   **build output**; the first red error line is the reason.
3. Run `:builder:assembleDebug`. The template module is built first automatically.
   The APK is in `builder/build/outputs/apk/debug/`.
