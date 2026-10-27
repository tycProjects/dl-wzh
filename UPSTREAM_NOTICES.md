# Upstream notices

This project is intended to bundle and use these third-party components. Preserve the complete upstream source, notices, and licenses in any distribution.

- Android PRoot engine: https://github.com/oonid/pr — includes PRoot, pr-cli, native Android execution integration, and other vendored code. The repository README identifies upstream license families; inspect its current `LICENSE`, `vendor/` licenses, and module-specific notices before redistribution.
- ARM64 Flutter on-device toolchain setup: https://github.com/0xR0/arm64-flutter-setup — installer fetched at runtime after the user taps Install toolchain.
- Flutter SDK: https://github.com/flutter/flutter — BSD license.
- Android SDK components and NDK: distributed under their respective Android SDK license terms.

Do not remove upstream license files from the `vendor/pr` directory during packaging. The build helper clones that repository into the generated Android host project.
