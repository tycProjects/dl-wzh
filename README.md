# Ruijie URL Cleaner — LORD OF DARKNESS

Kotlin + Jetpack Compose Android Studio project.

## Included
- Three bottom-navigation pages: Channel, Cleaner (initial page), History
- URL cleaning: removes `timestamp` and `sign` query parameters by default (case-insensitive)
- Selectable extra parameters to remove
- URL syntax analysis and validation
- Original vs cleaned URL, removed-parameter report, copy/share/clear actions
- Local history with copy, reuse, per-item delete and clear-all
- Telegram channel join button: https://t.me/+_BWDsHfxJFVmYTY9
- Uses the included LORD OF DARKNESS logo

## Open and build
1. Extract this ZIP.
2. Open the `RuijieUrlCleaner` folder in Android Studio.
3. Let Gradle sync complete.
4. Build > Build APK(s).

This ZIP contains the Android Studio project source, not a prebuilt APK. Android Studio/Gradle may need internet access to download build dependencies.

## Important note about URL checks
The app validates URL structure locally. It does not claim to verify that a portal session is live or that a server will accept a URL. Removing signed/session parameters can make a URL stop working. Use only URLs you are authorized to handle.


## Launcher icon
The Android launcher icon uses the supplied LORD OF DARKNESS hacker artwork and is included in all standard mipmap densities. The manifest sets both `android:icon` and `android:roundIcon`.
\n\n## Parameter selection UI update\nThe Cleaner home page now shows a two-column parameter checkbox grid below the Clear/CLEAN URL buttons, matching the supplied reference. `timestamp` and `sign` are selected by default; token, auth, sid, session, nonce, and t are optional. Select all and Clear selection controls are included. Only selected parameters are removed.\n