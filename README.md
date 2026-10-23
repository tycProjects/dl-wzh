# KillCheat — Free Fire Game Utility

VPN-based packet manipulation tool for Free Fire & FF MAX.  
No root required. Built in **Kotlin**.

---

## Project Structure

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/killcheat/app/
│   │   ├── ui/
│   │   │   ├── SplashActivity.kt
│   │   │   ├── MainActivity.kt         ← Language picker button (top-right 🌐)
│   │   │   ├── FeaturesFragment.kt     ← Teleport / Freeze / Ghost toggles
│   │   │   ├── FloatSettingsFragment.kt ← Size & opacity seekbars
│   │   │   └── SettingsFragment.kt     ← About + Admin info
│   │   ├── adapter/
│   │   │   └── MainPagerAdapter.kt
│   │   ├── service/
│   │   │   ├── CheatService.kt         ← Foreground service launcher
│   │   │   ├── FloatingButtonService.kt ← Overlay panel (draggable)
│   │   │   └── VpnCoreService.kt       ← Packet engine (Teleport/Freeze/Ghost)
│   │   ├── state/
│   │   │   ├── CheatState.kt           ← Global feature toggles
│   │   │   └── FloatState.kt           ← Float panel size/opacity
│   │   └── util/
│   │       └── LanguageManager.kt      ← 11-language switcher
│   └── res/
│       ├── layout/         (6 layout files)
│       ├── drawable/       (6 drawable XML)
│       ├── values/         (colors, themes, strings EN)
│       ├── values-vi/      Tiếng Việt
│       ├── values-zh-rCN/  中文
│       ├── values-ar/      العربية
│       ├── values-ru/      Русский
│       ├── values-th/      ภาษาไทย
│       ├── values-id/      Bahasa Indonesia
│       ├── values-es/      Español
│       ├── values-pt/      Português
│       ├── values-tr/      Türkçe
│       └── values-hi/      हिन्दी
```

---

## Setup

1. Open in **Android Studio Hedgehog (2023.1.1+)** or newer
2. Place `orbitron.ttf` in `app/src/main/res/font/orbitron.ttf`
   - Download: https://fonts.google.com/specimen/Orbitron
3. Add a launcher icon at `app/src/main/res/mipmap-hdpi/ic_launcher.png` (etc.)
4. Sync Gradle → Build APK

---

## Features

| Feature   | How it works |
|-----------|-------------|
| Teleport  | Corrupts sequence bytes in outgoing position UDP packets → server snaps player to last valid position |
| Freeze    | Zeroes delta fields in incoming enemy state packets → enemies stop moving, shots still register |
| Ghost     | Drops outgoing visibility beacon packets (opcode 0x11/0x12) → near-range invisibility; gloo wall (0x2A) passes through |

---

## Language support (🌐 top-right button)

| Code | Language           |
|------|--------------------|
| en   | English            |
| vi   | Tiếng Việt         |
| zh   | 中文 (Simplified)  |
| ar   | العربية            |
| ru   | Русский            |
| th   | ภาษาไทย            |
| id   | Bahasa Indonesia   |
| es   | Español            |
| pt   | Português          |
| tr   | Türkçe             |
| hi   | हिन्दी             |

---

## Kotlin vs Java — Why Kotlin wins here

| Point              | Kotlin                              | Java                             |
|--------------------|-------------------------------------|----------------------------------|
| Null safety        | Built-in `?.` / `!!` / `?:`        | Manual null checks everywhere    |
| Coroutines         | Native — perfect for packet loops   | Threads only (verbose)           |
| Singleton objects  | `object CheatState` — 1 line        | Double-check locking pattern     |
| Lambda / callbacks | `{ _, checked -> }` inline          | Anonymous inner class boilerplate|
| Extension funcs    | `ByteArray.copyOfRange(...)` clean  | Utility class hell               |
| Interop            | 100% Java interop — zero cost       | —                                |
| APK size delta     | +50-100KB (stdlib)                  | Baseline                         |
| **Verdict**        | **Kotlin** for this project         | Use Java only if team forces it  |

Kotlin costs ~80KB extra in APK size for the stdlib.
Everything else — safety, expressiveness, service/coroutine ergonomics — Kotlin wins cleanly.
This project is 100% Kotlin.

---

## Anti-ban notes

- `addDisallowedApplication(packageName)` — bypasses VPN for own app, no loop
- Only intercepts FF port range `10012–10015` + `17500` — other traffic passes untouched
- No native library injection into FF process — avoids `/proc/maps` scan
- Packet modification at kernel network layer — no in-game process signature
- `allowBackup="false"` — state does not leak
- ProGuard enabled on release — service class names preserved via rules
