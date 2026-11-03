# SnapStreak 1.3

Personal tool. Press Start and it launches Snapchat, then loops until you stop it:
drop down arrow, multi-snap, 16 camera taps, edit & send, send to, face melt tab, select all,
send, camera icon (358,1444), and back to the top. Coordinates are screen pixels (720 x 1600).

## What is new in 1.3 (only 5 photos instead of 16)
With no pause between camera taps, Snapchat drops most of them. There is now a
"Gap between camera taps" setting on the app screen, starting at 800 ms.
- Fewer than 16 photos: press + 100 until you get all 16.
- All 16 every time: try - 100 to go faster.
It is saved, applies to the next run, and needs no rebuild.
(The old 2000 ms always worked; 0 gave about 5 photos.)

## Also in this version
- Loop: after Send it taps the camera icon and starts again. MAX_LOOPS in Steps.kt limits it (0 = forever).
- Floating red STOP button over every app while it runs. Tap = stop, drag = move it.
  Volume Down and the Stop button in the app also stop it.
- If another app comes to the front it stops by itself, so it never taps TikTok etc.
- Face melt tab check (Android 11+) runs every loop and stops before sending if the tab will not switch.

## Not possible
Running while you use another app. Taps always go to whatever is on screen, so Snapchat must stay in front.

## Build the APK
Android Studio: File > Open > pick this folder > wait for Gradle sync > Build > Build APK(s).
APK: app/build/outputs/apk/debug/app-debug.apk (installs over older versions)
No PC: put the contents of this folder in a GitHub repo, open the Actions tab, run "Build APK",
then download SnapStreak-debug-apk from the run Artifacts.

## First-time setup on the phone
1. Install the APK.
2. If the accessibility switch is greyed out: Settings > Apps > SnapStreak > three dots > Allow restricted settings.
3. Settings > Accessibility > Installed apps > SnapStreak tapper > On.
   (After updating, switch it off and on once so it picks up the new permissions.)
4. Developer options: switch OFF "Pointer location" and "Show taps".

## Run
Open SnapStreak > Start. Keep the screen on and unlocked. Progress shows as "Loop n - step/24".
A toast appears when it stops.

## Edit
Everything is in app/src/main/java/com/personal/snapstreak/Steps.kt

## If something breaks
- If taps stop registering while the STOP button is showing: set SHOW_STOP_BUTTON = false (Volume Down still works).
- Loops 2+ do not relaunch Snapchat. If the arrow or multi-snap taps toggle something OFF that was left on
  from the previous loop, tell me and I will add a reset.
- Arrow / multi-snap / camera taps work but Send to list ignores taps: Snapchat is rejecting accessibility taps
  on that screen. Use ADB-based taps instead (the Tasker SendSnapADB route).
