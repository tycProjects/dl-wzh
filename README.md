# ProxyLite — Android starter (no login)

This is a small Android Studio starter project for a proxy app UI that does not require an account.

## Current features
- No login or signup screen
- Server address and port fields
- Basic input validation and connection-status UI

## Important limitation
This starter **does not establish a proxy/VPN tunnel**. A working client needs the actual server details and compatible protocol implementation (for example, HTTP proxy, SOCKS5, or a specific tunnel protocol). The supplied ZIP contained an APK and a tutorial video, not the original source project or protocol configuration, so this project does not copy the original app or pretend to connect.

## Build
1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Build > Build APK(s).

You will need Android Studio with Android SDK platform 35 and a compatible JDK.
