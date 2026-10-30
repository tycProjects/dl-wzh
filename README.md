# Wolfly Gallery Remote — local-network prototype

This source project is not an APK. It shares photos only after the phone owner grants Android photo permission and taps **Start authorized gallery sharing**.

## Build
1. Install Android Studio on a computer.
2. Open this folder in Android Studio and let Gradle sync.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Install the generated APK on the host Android phone.

## Use
1. Put both Android phones on the same Wi-Fi network.
2. On the host, open the app, grant photo permission, then tap **Start authorized gallery sharing**.
3. On the viewer, open the IP address and port shown by the host (for example `http://192.168.1.20:8765`).
4. Enter the six-digit PIN shown by the host.
5. Tap **Stop sharing** on the host when finished.

## Limits and security
- Same Wi-Fi only; no internet relay/cloud access.
- Keep the host app open while sharing.
- Android 14+ may grant access only to selected photos; only media Android authorizes is visible.
- This prototype uses HTTP on the local network. Do not expose port 8765 to the public internet.
- Source code has not been compiled into an APK in this environment.
