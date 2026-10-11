# JARVIS (Android) - Stage 3

Created by Tajim Uddin. Credit: Tajim Sir. Package `dev.tajim.jarvis`.

**Source only. This code has never been compiled by its author's tooling or run on a device by me.** Expect a few compile fixes on first build.

## Build (Termux or Android Studio)

1. Copy the wake-word model from your legacy zip into the project (68 MB, not bundled here):
   ```
   unzip -o ~/storage/downloads/app-release_extracted_files.zip 'assets/model-en-us/*' -d ~/JARVIS/app/src/main/
   ```
   It must end up at `app/src/main/assets/model-en-us/` (with `am`, `conf`, `graph`, `ivector`, `uuid`). Without it everything works except the always-on wake word.
2. `echo "sdk.dir=$HOME/android-sdk" > local.properties` and keep the aapt2 line in `gradle.properties` (Termux only).
3. `gradle assembleDebug`. APK: `app/build/outputs/apk/debug/app-debug.apk`. Vosk adds native libraries, so the first build downloads more and the APK is larger.

## What is in this build

- **UI** modelled on the reference: Home with hex fan panels (3D Studio, Text Lab, Draw Lab) around the Orb, neon connector lines, left/right launcher rails, status capsule, live waveform, weather card, Voice / Memories / Language chips, neon bottom dock with mic. Chat in the reference style. Settings as a profile card plus section rows.
- **Weather (real):** Open-Meteo, from your device location or a city you pick (Settings > Weather & Location).
- **AI (real):** provider-independent layer with a Gemini adapter. You enter your own API key (stored encrypted with the Android Keystore) and pick a model from the live list Google returns for your key. No model name is hard-coded.
- **Chat (real):** persistent history, code blocks with copy, stop and retry, Code mode, persona and memories applied. Not built: streaming, attachments, edit-and-resend, markdown beyond code blocks.
- **Voice:** Android speech recognition (device default, English or Bengali) and Android text-to-speech (Bengali voice used if installed).
- **Background assistant:** foreground service with a visible notification (Sleep/Wake, Stop). Modes: asleep (only the wake word "Jarvis" is heard, offline via Vosk), awake (listens for commands), "sleep" returns to asleep.
- **Phone control** through an Accessibility service you switch on yourself: open apps, Home/Back/Recents/notifications, lock screen (Android 9+), wake screen and swipe away a plain lock screen, scroll, volume, flashlight, tap a labelled button, type into the focused field, search the web/YouTube, call and WhatsApp (only after you say "yes"), time, battery, weather. Everything else goes to the AI.
- **Memory Bank**, **Permissions** screen, **Text Lab** (auto-saved draft, counts, lock, copy, clear), AI Lab list.
- 3D Studio, Draw Lab, Web/Code/File/Media Lab, Tools, System, More open their full-screen workspace but still say "not built yet". Updates shows only your real installed version (no update service exists).

## Honest limits

- **Unlock:** JARVIS cannot and will not enter your PIN, pattern or password. It wakes the screen and swipes away a plain swipe-to-unlock screen; with a secure lock you unlock yourself.
- **Background:** Android may stop or restrict apps; allow "Run without battery limits" in Permissions. Android 14+ does not allow starting a microphone service from boot, so open the app once after a restart. Some phones block speech recognition or opening apps from the background or with the screen off.
- **Wake word:** English "Jarvis" only; it needs the Vosk model above, uses battery, and false triggers or misses are possible. Command listening after wake uses Android's recognizer, which may send audio to Google's speech service unless an offline pack is installed.
- **AI Live audio** (the legacy Gemini Live WebSocket) is not included: replies come from text generation read aloud by TTS.
- Speaker verification ("voice enrolled") from the legacy app is not ported.
- Chat history, memories and the Text Lab draft are plain local files, not encrypted.
- Interface text is English only for now.

## Security choices

`allowBackup=false`; only `MainActivity` and the system-bound accessibility service are exported; cleartext traffic disabled; API key encrypted with Keystore and never logged; no QUERY_ALL_PACKAGES (only launchable apps are visible); permissions are requested only when you tap Allow; the accessibility service ignores all screen events and records nothing.

## Next

Draw Lab, 3D Studio, Code/Web/File/Media Labs, Tools, System, Gemini Live audio, speaker profile.
