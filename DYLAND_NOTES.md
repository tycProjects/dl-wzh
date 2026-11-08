# Dyland – project update

## Branding and UI
- Launcher label, accessibility label and quick-settings tile label changed to **Dyland**.
- Launcher artwork uses the user-provided image (`app/src/main/res/drawable-nodpi/dyland_icon.png`).
- Added a short animated Dyland intro overlay.
- Tightened page gutters, section gaps and setting-row padding for a more compact phone layout.
- Updated the dark palette to a navy / blue-violet glass direction.
- Refined the existing glass surface with a cool internal glow, multi-stop reflective veil and an extra lens highlight.

## Liquid-glass research
Reviewed these Android/Compose projects:
- https://github.com/styropyr0/PrismalAGSL — AGSL refraction, backdrop blur, specular edge, graceful API fallbacks.
- https://github.com/BuildItCode/LiquidGlass — live backdrop capture and AGSL glass rendering.
- https://github.com/Abdullajon1881/LiquidGlass — layered glass, refraction and interaction concepts.

The supplied project currently uses Kotlin 2.1.0 and an older Compose BOM. PrismalAGSL documents Kotlin 2.2+ and a newer Compose setup, so it was not added as a binary dependency in this pass: doing so without upgrading and validating the full Android toolchain risks making the project fail to build. Instead, the existing Compose glass renderer was refined without adding an unverified dependency. A true live-backdrop refraction renderer can be integrated after a coordinated Kotlin/Compose/AGP upgrade and device testing.

## Accessibility behavior
- The app opens the system Accessibility page (and attempts the per-service details page first).
- Added a dedicated App info shortcut and Vietnamese/English guidance for Android 13+ "Allow restricted settings".
- The app does not attempt to toggle Accessibility through hidden APIs or bypass Android's confirmation. The user must approve the service in system Settings.

## Build validation
A local Gradle build was attempted, but the environment could not resolve `services.gradle.org` to download the Gradle wrapper distribution. Compilation therefore remains unverified in this environment. Open the project in Android Studio and run `:app:assembleDebug` with network access to complete validation.
