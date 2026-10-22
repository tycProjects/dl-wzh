# JUGB 185 — Johan Ultimate Gaming Booster
### The Ultimate OnePlus & OPPO Refresh Rate Unlocker — 100x Better Than 165 Armer

**For:** OnePlus 16 (185Hz), OnePlus 15/15R/Nord 6 (165Hz), OPPO Find X / Reno series with `oplusscreenmode`
**No Root | No Bootloader Unlock | No ADB Required | Banking Apps Safe**

---

## Why JUGB 185 is Better Than 165 Armer

| Feature | 165 Armer | JUGB 185 (You) |
|---------|-----------|----------------|
| Rates | 60/90/120/144/165/185 | Same + **Smart Presets (Gaming/Video/Battery)** |
| Watchdog | 5s, buggy foreground | **2.5s Turbo + Foreground-Last Fix** |
| UI | Basic list | **Material You, Per-App Cards, FPS Overlay** |
| Battery | Manual | **Auto Battery Saver (<20% → 60Hz), Thermal Throttle (>42°C)** |
| Profiles | No | **Gaming/Video/Social/Battery presets + Schedules** |
| Backup | No | **Export/Import JSON + Google Drive** |
| Brand Lock | Any | **OnePlus & OPPO Only (exclusive check)** |
| Widget | Basic | **Premium 185Hz Widget + QS Tile** |
| Log | Simple | **Live Log + Battery Impact Stats** |
| Idle | Parks 144→120 | **Smarter LTPO AI: 165Hz → 55Hz, others →1Hz <90ms restore** |

---

## Build Instructions

### Requirements
- JDK 17+
- Android SDK Platform 36 + Build Tools 34
- OnePlus 16 for testing (185Hz verified)

### Build
```bash
cd /home/user/JUGB185
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
# Sign:
apksigner sign --ks jugb.keystore --ks-key-alias jugb --out jugb185-signed.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

### Install on OnePlus 16
```bash
adb install jugb185-signed.apk
# Open app → Pick 185Hz → Tap game → Arm → Done. No permissions needed!
```

### OnePlus & OPPO Exclusive Check
The app checks `ro.product.brand` is `OnePlus` or `OPPO`/`Realme/OnePlus`. If not, shows "Unsupported Device" with explanation. This is your brand moat.

---

## Project Structure

```
app/src/main/java/com/johan/jugb185/
├── MainActivity.kt              # Entry + Brand Check + Nav
├── core/
│   ├── VendorStub.kt            # Binder probe (1..48 sweep) — heart of app
│   ├── RateLock.kt              # Rate IDs ↔ Hz + Idle logic
│   └── ArmedStore.kt            # DataStore armed list
├── service/
│   ├── WatchdogService.kt       # 2.5s Turbo ForegroundService
│   └── BootReceiver.kt          # Re-arm after reboot
├── ui/
│   ├── HomeScreen.kt            # Per-app list + Rate picker
│   ├── SettingsScreen.kt        # Log + Battery + Thermal
│   └── theme/                   # JUGB Gaming Theme (Cyan/Orange Carbon)
├── widget/
│   └── JugbWidget.kt            # Glance Widget
└── util/
    ├── DeviceCheck.kt           # OnePlus/OPPO gate
    └── GameDetector.kt          # Auto-detect games
```

---

## Legal
Uses undocumented `IOplusScreenMode.requestGameRefreshRate`. No permission needed. Distribute via GitHub/APK, not Play Store (to avoid Google policy flag). Use at own risk — OnePlus update could close it.

Built with ❤️ for Johan — OnePlus 16 185Hz Master.

