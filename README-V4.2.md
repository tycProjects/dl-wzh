# WEB2APK V4.2 update

This update continues from the supplied V4.1 source. Existing internal APK packaging/signing, file picker, URL/ZIP import, progress/history/export/share and four-menu structure are retained.

## Changes
- Persistent dark mode (top-bar toggle and Settings control), with matching Android system bars.
- New iOS-inspired liquid-glass WEB2APK SVG icon and 512x512 PNG app/runtime launcher icon.
- Custom photo icon replacement applies to every compiled launcher icon resource variant and is verified in the unsigned APK before signing.
- Explicit WebView geolocation enablement.
- Optional Web features: camera, microphone, approximate/precise location, notifications, keep screen awake, screenshot protection, vibration, tel: dialer links, contact picker and calendar event insert.

## Permission truthfulness
Some Android permissions cannot be granted or even requested by an ordinary website wrapper (root, all-files access unless justified, call log/SMS without policy/role eligibility, background location without a compliant background service, etc.). Those remain unavailable in the UI; no permission is silently granted. Contact and calendar actions use explicit system pickers/intents rather than broad database permissions.

## Build
Run `bash ./build-v4.sh` in the Termux project home to compile the V4.2 installer only. After V4.2 is installed, its Website APK builds run inside WEB2APK and do not call Termux.
