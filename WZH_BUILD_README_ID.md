# Ryomd Tube — Android Gradle Source (V2.3 baseline)

## App identity

- Launcher / APK name: `Ryomd panel`
- YouTube feature name: `Ryomd Tube`
- `applicationId`: `com.ryomd.nativeapp` (unchanged so the installed app can be updated)
- `versionCode`: `9`
- `versionName`: `1.1.8-ryomd-tube-quality-fix`
- UI assets remain bundled in the APK (`USE_BUNDLED_UI = true`).

## Ryomd Tube capabilities

- Public YouTube discovery/search with native NewPipe pagination up to 100 results per query.
- Pull-down refresh at the top of the home feed and a persistent refresh icon button.
- Local-only channel subscriptions, local viewing history, and public link playback.
- Native Media3 playback with an in-player quality menu based on progressive resolutions actually returned for that video (Auto plus available heights up to 2160p).
- When the source exposes only an HLS/DASH manifest without individual progressive variants, Auto remains available; manual resolution choices may not be exposed for that video.
- No Google account sign-in, Firebase, Google OAuth bridge, or Play Services Auth dependency.

## UI/UX reference

Applied the Video Streaming/OTT design profile from UI/UX Pro Max (Dark Mode OLED + Motion-Driven; cinema-dark surfaces with a red video accent), plus its mobile/accessibility and interaction guidance. Official repository:
https://github.com/nextlevelbuilder/ui-ux-pro-max-skill

Relevant conventions applied: mobile-first layout, consistent dark/light semantic surfaces, 16:9 lazy-loaded thumbnails, 48dp minimum Android touch targets for player and navigation controls, accessible labels, visible refresh feedback, and reduced-motion support.

## WZH build instructions

1. Import this ZIP as an **Android Gradle project**, not as an HTML app or App Update Center package.
2. WZH's source scanner looks for a literal root `settings.gradle`; this package includes that file at the ZIP root, alongside conventional Groovy `build.gradle` files for the root project and `app/` module.
3. Use JDK 17 and Android SDK platform/build tools for `compileSdk 36`.
4. Keep the same package ID and persistent release signing key as the installed application. Do not uninstall the old app before updating.
5. Use WZH's Android Gradle build flow for `assembleRelease`.

The ZIP does not include `gradle-wrapper.jar`; the provided `gradlew` is the project's verified-download bootstrap script. WZH may use its own Gradle runner instead. Do not choose the generated default web/HTML-to-APK recipe if WZH fails to detect the Gradle source.

Backend Termux remains at `127.0.0.1:3002`. Existing WhatsApp, Hermes, RVC, databases, and Termux scripts are outside this Android source and are not modified.

## Validation status

Static JavaScript syntax, XML parsing, archive layout, and source checks are performed before packaging. A successful Gradle compile and WZH E-PUBLISH run must be confirmed in WZH itself; no claim of a successful WZH APK build is made until its build log reports success.


## Jika WZH menampilkan "No Gradle project (settings.gradle) could be located"
1. Unggah ZIP ini **apa adanya** (jangan dibungkus folder lain, jangan buka lalu zip ulang dari dalam folder induk). `settings.gradle`, `build.gradle`, `gradlew`, dan folder `app/` harus berada di **akar** ZIP.
2. Pilih mode **Android Gradle / Native project**. Log yang berisi "No build recipe ... default APK one" berarti WZH memakai resep web (HTML) bawaan — itu bukan mode yang benar untuk proyek ini.
3. Proyek ini memakai `settings.gradle` (Groovy), bukan `settings.gradle.kts`, karena pemindai WZH mencari nama file `settings.gradle` secara harfiah.
4. Paket ini memuat `gradle-wrapper.jar` + `gradlew` + `gradlew.bat` resmi (Gradle 8.13) dan *core library desugaring* (wajib untuk NewPipe Extractor pada minSdk 26).
