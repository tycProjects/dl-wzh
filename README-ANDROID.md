# Msv mai co dac xe - Android port

This project is a native Android port scaffold of the supplied J2ME source.

## Build

Open the project in Android Studio and run `app` or build `assembleDebug`.
A GitHub Actions workflow is included at `.github/workflows/build-apk.yml` for cloud building.

## Multiplayer

The Android build keeps the original `Net` packet protocol and replaces JSR-82 Bluetooth with Android Bluetooth Classic RFCOMM using the same UUID:
`A1B2C3D4-E5F6-0718-293A-4B5C6D7E8F90`.

This allows the Android APK to communicate with the existing E72 J2ME Bluetooth implementation when the E72 exposes the same service.

Wi-Fi transport is not enabled by this first Android port because the supplied E72 JAR currently only exposes the Bluetooth transport (`BtLink`). To support E72 <-> Android over Wi-Fi, the J2ME side also needs a socket transport using the same `Net` protocol.
