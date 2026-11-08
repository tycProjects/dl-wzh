# Dyland – project update

## Branding and layout
- Launcher label, accessibility label and Quick Settings tile label are **Dyland**.
- Launcher artwork uses the image supplied by the user at `app/src/main/res/drawable-nodpi/dyland_icon.png`.
- Removed the custom welcome/intro overlay: the app now opens directly on its main screen.
- Reduced page gutters, headings, setting-row spacing, slider padding and bottom navigation footprint for a denser phone layout.

## Liquid Glass implementation
- The app uses Jetpack Compose, so the Compose implementation **PrismalAGSL** is used instead of the XML/View-only Prismal renderer.
- Added JitPack and `com.github.styropyr0:PrismalAGSL:Prismal:1.0.7`.
- Added a full-screen animated blue/violet backdrop source. App cards, setting groups, compact chips, action buttons, toggles and bottom navigation sample that backdrop through Prismal's glass renderer.
- The bottom navigation uses Prismal's draggable liquid droplet tabs.
- The appearance is tiered by the library: API 33+ gets AGSL refraction; API 31–32 gets `RenderEffect` blur; API 26–30 gets the library's frosted fallback.
- The Dynamic Island overlay that appears above other apps retains its own rendering path; a Compose backdrop source from the in-app Activity cannot reliably sample pixels belonging to unrelated app windows.

Reference projects reviewed:
- https://github.com/styropyr0/PrismalAGSL
- https://github.com/styropyr0/Prismal
- https://github.com/SidZadaun02/AppleLiquidGlassForAndroid
- https://github.com/QmDeve/AndroidLiquidGlassView
- https://github.com/QWEA0/Liquid-Glass-Android
- https://github.com/Kyant0/AndroidLiquidGlass
- https://github.com/orbitalsonic/Sonic-Water-Wave-Animation
- https://github.com/zwmopen/Fluid-Navigation-Gesture
- https://github.com/Chrisvin/LiquidSwipe
- https://github.com/Chrisvin/BlobBackgroundLayout

PrismalAGSL was chosen because the app is Compose-based and the library offers backdrop sampling, refraction, specular/depth effects and ready-made glass controls. The XML-only projects were considered but not used in the Compose screen hierarchy. LiquidSwipe/BlobBackgroundLayout are transition/background concepts rather than replacements for every panel surface.

## Accessibility behavior
- The app opens Android's system Accessibility settings and provides the Android 13+ restricted-settings guidance.
- The app does not bypass Android's user confirmation or attempt hidden permission grants. The user must enable the service in system Settings.

## Build validation
- The source was updated to Kotlin 2.2.21, AGP 8.11.1 and compileSdk 36 for the PrismalAGSL dependency.
- `:app:compileDebugKotlin` could not be completed in this environment because the Gradle wrapper distribution could not be downloaded from `services.gradle.org` (network/DNS unavailable). Compilation and on-device appearance therefore still need validation in Android Studio with network access.
