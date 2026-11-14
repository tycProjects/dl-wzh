# Push notifications for admin replies

## 1. Firebase (same project you already use)
- Project settings -> General -> Your apps: the Android app must use your app's package name.
  Download its `google-services.json` and put it in `android/app/`.
- Project settings -> Service accounts -> Generate new private key. This downloads a JSON file. Keep it private.
- Make sure it is the SAME Firebase project as the google-services.json.

## 2. Android build files (Flutter)
`android/settings.gradle(.kts)` plugins block: add
    id "com.google.gms.google-services" version "4.4.2" apply false
`android/app/build.gradle(.kts)` plugins block: add
    id "com.google.gms.google-services"
If the build complains about minSdk, set `minSdkVersion 23` in `android/app/build.gradle`.

## 3. Supabase
- SQL Editor: run `push.sql`.
- Edge Functions -> Secrets: add `FIREBASE_SERVICE_ACCOUNT` = the whole contents of the service-account JSON file.
- Edge Functions -> `telegram-hook`: paste the new `index.ts` and Deploy ("Verify JWT" stays OFF).

## 4. App
Replace `pubspec.yaml`, `lib/chat_cloud.dart`, `lib/notify.dart`, `lib/main.dart`, `lib/screens/message_page.dart`,
add `lib/push.dart`, then build and install over the current app.

## Test
Open the app once (it saves its push token), allow notifications, send a message, close the app completely,
then swipe-reply in Telegram. A notification should arrive. If not, look at telegram-hook logs for "fcm failed".
