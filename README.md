# SADIK GAMING — Flutter Project

A premium dark/neon gaming-hub starter app built with Flutter and no third-party packages.

## Included
- Premium dark purple gaming UI
- Home, Games, Rewards and Profile tabs
- Working navigation and buttons
- Demo daily reward and random demo spin
- Game detail dialogs and profile/settings dialogs
- Responsive scrolling layouts

## Important
Reward balances and spin prizes are local demo data only. They have no real monetary value and cannot be withdrawn or redeemed. Game tiles show a detail dialog; they do not launch or install third-party games. Connect official destinations/services if you want those features.

## Build on Android / Termux or a computer
1. Install Flutter and Android SDK using the official Flutter setup guide.
2. Extract this ZIP.
3. In the project folder run:
   flutter pub get
   flutter create . --platforms=android
   flutter run
4. To build an APK:
   flutter build apk --release
5. APK output is normally at:
   build/app/outputs/flutter-apk/app-release.apk

If `flutter create .` asks to overwrite existing files, keep the supplied `lib/main.dart` and `pubspec.yaml`.
