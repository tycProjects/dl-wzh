# GREATNICE GTS-1550 SMART CONTROL

Native Kotlin / Jetpack Compose app for a Moto E13 (Android 13) and a GREATNICE GTS-1550 Bluetooth speaker.
No root, no internet permission, no accounts, no analytics, no API keys.

**Status: written but NOT yet compiled or tested on real hardware by the author. Nothing is verified on a GTS-1550.**

## Hardware limitations (what research found)
- GTS-1550 listings describe a budget dual-4-inch party speaker with Bluetooth, USB, TF card, FM and TWS, operated by **physical buttons**.
- No manual, companion app, SDK or documented remote protocol was found. Pairing is by searching for "GTS-1550" in Bluetooth settings.
- Therefore the only things a phone can legitimately do are standard Bluetooth audio (A2DP/AVRCP) things.
- Bass, EQ, FM, USB/TF mode, LEDs, TWS and battery are almost certainly local to the speaker and not remotely controllable. They are marked unsupported.

## Works with ordinary Bluetooth audio
| Feature | How | Caveat |
|---|---|---|
| Discovery / pairing | `startDiscovery`, `createBond` (system dialog) | Speaker must be in pairing mode |
| Connection status | A2DP profile proxy + broadcast (read-only) | |
| Play/pause/next/prev | `AudioManager.dispatchMediaKeyEvent` | Controls the phone's player |
| Volume | `AudioManager.setStreamVolume(STREAM_MUSIC)` | Speaker amp follows only with AVRCP Absolute Volume |
| Safe disconnect | Pause, then open system Bluetooth settings | Android has no public API to drop A2DP |

## Needs a manufacturer protocol (not implemented)
Battery (unless standard GATT 0x180F is found), bass/EQ, FM radio, USB/TF source, LED effects, TWS.

## Diagnostic mode
DIAG tab: SDP UUID list (non-standard UUIDs flagged), device class/transport, a **read-only** BLE GATT probe
(never writes, never subscribes), a log you can share. Safe investigation steps are shown in the app:
HCI snoop log of the *official* app's traffic, analysed in Wireshark; never replay or guess bytes.

## Build
1. Install Android Studio (Hedgehog or newer) with JDK 17 and Android SDK 34.
2. File → Open → this folder. Let Gradle sync (needs internet once to download dependencies).
   The wrapper JAR is not included; Android Studio uses `gradle-wrapper.properties` (Gradle 8.2). If needed run `gradle wrapper --gradle-version 8.2`.
3. On the Moto E13: Settings → About phone → tap Build number 7× → Developer options → USB debugging.
4. Connect by USB, press Run. Or Build → Build APK(s) and sideload.
5. First launch: grant "Nearby devices". Put the speaker in pairing mode, DEVICES → SCAN → select → PAIR.

## Hardware verification checklist
- [ ] Pairs and shows A2DP CONNECTED
- [ ] Transport buttons control the phone's music app
- [ ] Volume slider changes speaker loudness (tells you if Absolute Volume works)
- [ ] DIAG shows UUIDs; note any NON-STANDARD ones
- [ ] GATT probe result (likely "no BLE")
