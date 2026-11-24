# Ryomd Tube V3 — OAuth diagnostics and home feed pagination

Basis: `RYOMD_NATIVE_YOUTUBE_PLAYER_V2_3_TUBE_WZH_SOURCE.zip`

## Fixes
1. OAuth result processing now parses Google's returned Intent before interpreting the Activity result code. Google API status codes are surfaced instead of converting every non-`RESULT_OK` result into a generic cancellation message.
2. Explicit status `10` explains the Android OAuth package/SHA-1 mismatch; status `7/8` explains network/service failures; `12501` is shown as a user cancellation.
3. Missing result data now provides targeted checks for Google Play Services, OAuth Android client, package `com.ryomd.nativeapp`, and WZH release signing SHA-1. No Google password or secret is stored.
4. NewPipe home search now follows subsequent `SearchInfo` pages instead of reading only the first HTML result page. The native catalog returns up to 40 videos per request and supports additional slices up to the bounded 200-item search window.
5. Ryomd Tube Beranda now has `Segarkan` and `Muat video lainnya` controls. Results append without duplicate video IDs.

## Build
- `applicationId`: `com.ryomd.nativeapp` (unchanged)
- `versionCode`: `8`
- `versionName`: `1.1.7-ryomd-tube-v3`

## Limitations
- Google OAuth still requires a correctly configured Google Cloud OAuth Android client, matching release signing SHA-1, enabled YouTube Data API v3, and the account added as a test user while consent is in Testing. These are external project settings and cannot be fabricated in source.
- Static source checks do not prove OAuth login on device or a successful APK build. The resulting APK must be tested using the same WZH signing key fingerprint registered in Google Cloud.
- NewPipe extraction depends on YouTube's current public web responses and can be interrupted by anti-bot or upstream changes.
