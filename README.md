# 𝐒𝐡𝐚𝐫𝐢𝐧𝐠𝐮𝐧 𝐗𝐘𝐙 — Free Fire Headshot Panel

## Build Instructions

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Steps
1. Open Android Studio
2. File → Open → Select this `SharingunXYZ` folder
3. Wait for Gradle sync to finish
4. Connect phone or use emulator
5. Run → Run 'app'

### Or build APK directly:
```
./gradlew assembleDebug
```
APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## How to Use

1. Install APK on phone
2. Open **𝐒𝐡𝐚𝐫𝐢𝐧𝐠𝐮𝐧 𝐗𝐘𝐙** app
3. Toggle the **Enable Panel** switch → Allow overlay permission when asked
4. Open **Free Fire** (any version including 1.132.21)
5. A red **eye icon** will float on screen
6. **Tap** the eye icon → Panel opens
7. Choose **80% Headshot** or **100% Headshot**
8. **Drag** the icon anywhere on screen
9. To hide panel: tap ✕ inside panel
10. To stop: go back to app → toggle switch OFF

## Notes
- Works on Android 5.0+ (API 21+)
- Tested compatible with Redmi A1+ Android 12 Go Edition
- No display overlay injected INTO the game — uses Android system overlay (TYPE_APPLICATION_OVERLAY)
- Does NOT require root
- Does NOT require accessibility service
- Panel stays on top of Free Fire without triggering "not installed" error

## Package
`com.sharingun.xyz`
