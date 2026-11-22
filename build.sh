#!/system/bin/sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$ROOT"
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle не найден."
  echo "ZIP содержит исходники; для APK нужны Android SDK, NDK, CMake и Gradle."
  exit 1
fi
if [ -z "${ANDROID_SDK_ROOT:-}" ] && [ -z "${ANDROID_HOME:-}" ]; then
  echo "Укажи ANDROID_SDK_ROOT на каталог Android SDK."
  exit 1
fi
gradle --no-daemon :app:assembleDebug
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
