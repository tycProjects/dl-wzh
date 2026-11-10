<p align="center">
  <img src="art/icon-512.png" width="160" alt="Nova icon"/>
</p>

<h1 align="center">Nova</h1>
<p align="center"><b>Every touch becomes light.</b><br/>
Clean 4-point star novas, falling geometric shapes and a living white grid on pure black.</p>

---

## Features

| | |
|---|---|
| **Nova** | Crisp 4-point star core, iridescent shock ring, anamorphic flare and side sparks that leave long luminous paths when dragged |
| **Shapes** | Outlined squares & triangles with subtle 3D edge lighting that tumble and fall outward |
| **Water / Orbs / Lightning** | Still available if you want the classic set |
| **Living grid** | Pure black wallpaper + clean white square grid. Effects light the surface naturally |
| **6 colour palettes** | Aqua, Neon, Ember, Toxic, Sunset and live Rainbow |
| **Fine-tuning** | Size, duration, intensity, haptic, drag trails |
| **Multi-touch** | Every finger gets its own effect |
| **Three ways to use** | In-app playground · Live wallpaper · All apps (accessibility overlay) |
| **Private** | Only `VIBRATE`. No internet, no analytics, zero third-party libraries |

## Where the effects show up

1. **Playground** – top of the app. Taps and drags with live settings preview.
2. **Live wallpaper** – black + white grid home screen. Works on every Android version.
3. **All apps** – accessibility overlay. Touches are never blocked. On Android 13+ sideloaded builds you may need **App info → ⋮ → Allow restricted settings**.

## Build

Requirements: JDK 17 + Android SDK (platform 35).

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease
```

Or push to GitHub — the workflow builds both APKs automatically.

## Project layout

```
app/src/main/java/com/touchflux/app/
├── MainActivity.kt
├── data/Prefs.kt
├── fx/
│   ├── EffectEngine.kt
│   ├── EffectView.kt
│   ├── NovaEffect.kt  GeoEffect.kt  RippleEffect.kt  OrbEffect.kt  LightningEffect.kt
│   └── ...
└── service/
    ├── TouchFluxWallpaperService.kt   (black grid wallpaper)
    ├── TouchFluxAccessibilityService.kt
    └── FxState.kt
```

## Icon

Clean monochrome 4-point star on black rounded square (matches the living-grid aesthetic).

## License

MIT
