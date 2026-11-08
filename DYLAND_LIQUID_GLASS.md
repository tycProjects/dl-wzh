# Dyland Liquid Glass integration

## Renderer

The Compose UI uses `PrismalAGSL` 1.0.7. This is the Compose successor to the original OpenGL ES `Prismal` project. It records the app backdrop once and uses `drawPrismalGlass` to render transparent refractive surfaces over that backdrop.

Main integration points:

- `ui/DylandGlass.kt` — backdrop provider, shared glass modifier and moving ambient light background.
- `ui/MainActivity.kt` — root backdrop capture and the draggable droplet tab bar.
- `ui/Components.kt` — shared compact settings groups, liquid toggle, action chips and slider containers.
- `ui/screens/*` — screen-specific panels converted to the shared glass modifier.

## Platform behavior

| Android API | Render path |
|---|---|
| 33+ | AGSL refraction / dispersion |
| 31–32 | RenderEffect blur |
| 26–30 | Frosted glass fallback |

The app's `minSdk` remains 26. The library documentation recommends compileSdk 37+; this project is currently set to 36 with AGP 8.11.1 to stay aligned with the existing build toolchain. Raise compileSdk/AGP together when moving the project to the newer Android SDK toolchain.

## Build

Open this project in Android Studio, allow Gradle/JitPack dependency resolution, then run `:app:assembleDebug`. A local compile was not possible in the editing environment because the Gradle distribution download was blocked by network/DNS resolution.
