# Fartomizer (by Kian)

Shizuku-based optimizer for Samsung phones: refresh rate lock, phantom process limit toggle,
animation speed, and clear background apps. Original code; inspired by the feature set of the APK you uploaded.

## Import
1. Unzip, then in Android Studio: File > Open > select the Fartomizer folder.
2. Let Gradle sync (internet needed the first time, JDK 17).
3. Build > Build APK(s). The release build is signed with the debug key so it installs.

## Use on the phone
1. Install Shizuku from Play Store and start it with Wireless debugging.
2. Open Fartomizer > Grant permission > use the toggles.

Every change has a reset: "Back to default", the phantom switch off, animations 1x.
Samsung settings keys differ by model/One UI version, so confirm each tweak actually does something on your phone.

## Options
Refresh rate, phantom process limit, animation speed, clear background apps, resolution scale (auto-reverts),
per-app tools (game mode, optimize, force stop, keep alive), optimize all apps (dexopt), private DNS.

## Game tuner tab
Game Manager (add games, per-game graphics preset High/Medium/Low/Ultra Low via Android's Game Mode API),
Launch with boosts (clear background apps, Do Not Disturb, 120 Hz), End session to restore, live battery/RAM stats.
Game Mode presets only work on Android versions/ROMs that support `cmd game`; the Output box shows the result.

## Real booster additions
Launch always sets the game's standby bucket to active. Optional: force-stop other user apps (keeps game, launcher,
keyboard, Shizuku, this app). Samsung switch: disable/restore Game Optimizing Service (pm disable-user / pm enable).
Per-game Optimize button (speed-profile compile).
