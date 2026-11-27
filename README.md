# ROOM WANGI FAZX — Android starter

A native Android starter app with:
- dark crimson dashboard,
- OpenVPN for Android hand-off by profile name,
- local terminal animation (visual simulation only),
- Google account security checklist and official account-security links.

## Important scope
This is a **buildable MVP starter**, not a finished commercial VPN service. It does not include VPN servers, `.ovpn` files, VPN credentials, paid-key licensing, Firebase admin APIs, or server-side account provisioning. A VPN tunnel is provided by the OpenVPN client and your authorized VPN provider.

The app launches the OpenVPN for Android `ConnectVPN` activity using the documented profile-name extra. Import the `.ovpn` profile into OpenVPN for Android first, and use the exact profile name. The external client handles the Android VPN permission and tunnel.

## Build
1. Install Android Studio (current stable) and Android SDK Platform 35.
2. Open this folder in Android Studio.
3. Let Gradle sync.
4. Connect an Android device or start an emulator.
5. Run the `app` configuration.

No Firebase configuration is required for this MVP.

## OpenVPN setup
1. Install OpenVPN for Android.
2. Import a valid `.ovpn` configuration supplied by your VPN administrator.
3. Note the profile name shown in OpenVPN.
4. Enter that exact profile name in ROOM WANGI FAZX and tap **OPEN OPENVPN CLIENT**.
5. Approve the Android VPN connection prompt when it appears.

If the client is missing, the app shows a message and opens the upstream project page.

## Google Security Dashboard
The checklist is local UI only. It never asks for or stores Google passwords, one-time codes, recovery codes, or session cookies. The buttons open Google's own security and recovery pages.

## Production requirements not included
For a real multi-user product, add a trusted backend (for example Firebase Auth + Realtime Database + Cloud Functions) for:
- one-key/one-device claims,
- subscription expiry checks,
- admin-only server/profile management,
- revocation and audit logs.

Do **not** let Android clients write their own admin role, claim arbitrary keys, or store VPN passwords in public database paths. Enforce key claims and admin actions on a trusted server and use restrictive database rules. Never commit service-account credentials or private signing keys.

## OpenVPN licensing
This app uses an external OpenVPN for Android app through its documented intent interface; it does not bundle or modify ics-openvpn source. If you decide to embed, fork, or redistribute ics-openvpn code, read its GPL licensing terms and comply with the applicable source-distribution obligations. See:
- https://github.com/schwabe/ics-openvpn
- https://github.com/schwabe/ics-openvpn/blob/master/doc/README.txt

## Package
`com.roomwangifazx.app`
