# Security checklist (from the "vibe-coded app" video) vs WZH 2 APK

| # | Check | Status |
|---|-------|--------|
| 1 | Hash passwords | N/A. No passwords of your own: sign-in is Google / Supabase Auth |
| 2 | API keys on the server | OK. GitHub token (SHARE_GITHUB_TOKEN), Cloudflare token, service_role, Telegram, Firebase are all server-side secrets. Only the Supabase anon key is in the app (it is meant to be public) |
| 3 | Authorization (the 403 check) | OK. Every table has RLS with no policies; all access is through functions that check `auth.uid()` / owner. Worker checks site ownership with an HMAC |
| 4 | HTTPS everywhere | OK in code (all remote URLs are https; http only to 127.0.0.1 for the preview). NEW: HSTS header on the download page |
| 5 | Validate on the server | OK. Lengths/limits in the SQL functions, size + type checks in the edge functions. NEW: storage now only accepts exact paths and file types |
| 6 | XSS | OK. App uses Flutter `Text`; download page uses `textContent`. NEW: CSP + nosniff + no-framing headers, and the download link must point at our own GitHub repo |
| 7 | SQL injection | OK. Only parameterized RPC calls, no string-built SQL |
| 8 | Rate limiting / CSRF | Worker: per-IP daily limit (needs HOST_KV). NEW: global flood cap on chat-media |
| 9 | Secure cookies | N/A. No cookies |
| 10 | Expiring reset links | N/A. No password reset |
| 11 | Limit file uploads | NEW: bucket accepts only APK/PNG at app.apk, logo.png, profiles/*.png, 25 MB max |
| 12 | Recheck price on server | N/A. No payments |
| 13 | Debug mode off | OK. `debugShowCheckedModeBanner: false`, only short debugPrint lines, no secrets in logs |
| 14 | Delete unused dependencies | NEW: removed `google_fonts` (unused, Inter is bundled) |
| 15 | Leaked keys in git | No private keys, keystores or .env in this zip. Run `git log -p \| grep -i -E "service_role|ghp_|sk_live"` in your real repo, and rotate anything found |
| 16 | Test every fix | See the notes in the reply |

## Still open (needs your decision or server.js, which is not in the zip)
- `kAppToken` + `kSiteOrigin` are in the APK, so anyone can copy those headers. Treat them as a speed bump, not a lock. Keep the server-side signature check (TYCEPT_SIGNING_SHA256) and the session token on /api/share-token.
- Store reviews and downloads count (`store_review_save`, `store_hit`) accept a made-up 32-char phone id, so ratings/downloads can be inflated. Fix: require sign-in for reviews and add a per-IP limit.
- `open_filex: any` and `media_store_plus: any` are unpinned. Pin them to the versions in your pubspec.lock.
- Generated apps (quick/manifest_gen.dart) set `usesCleartextTraffic="true"` and `allowBackup="true"`. That is deliberate for wrapping http sites; make it an option if you want safer defaults.
- Your real AndroidManifest.xml is not in the zip: check it has `android:allowBackup="false"` and no `usesCleartextTraffic`.
