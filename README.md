# Shop Manager Flutter project

This project wraps the supplied `index-44-9.html` inside an Android Flutter WebView so the original layout and JavaScript features are retained instead of being manually re-created with different UI.

## Included
- Original HTML app copied unchanged to `assets/index.html`
- Flutter entry point in `lib/main.dart`
- `flutter_inappwebview` for JavaScript, DOM storage, camera/media permission requests, and WebView rendering
- Android internet and camera/microphone permission declarations

## Build
1. Install Flutter SDK and Android Studio.
2. In this folder run `flutter create . --platforms=android` (if the Android Gradle wrapper / generated Flutter files are not present).
3. Run `flutter pub get`.
4. Run `flutter run` on an Android device, or `flutter build apk --release`.

## Important notes
- The supplied HTML uses Firebase Authentication and Firebase Realtime Database. An internet connection is needed for sign-in and cloud data. Firebase access rules and authorized domains must be configured correctly in the Firebase console.
- The original app also uses browser APIs and CDN-hosted QR libraries. Camera/QR scanning, file input, printing, and JSON/CSV downloads should be tested on the target Android device; browser-to-native behavior can vary by Android System WebView version. This starter wrapper does not claim every browser download/print path is fully native-integrated.
- The original HTML has been copied unchanged; no business logic or Firebase configuration was intentionally modified.
- Do not publish Firebase admin credentials in the app. The client Firebase config in the HTML is not a private server credential; protect data with Firebase Security Rules and App Check as appropriate.
