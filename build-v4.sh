#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

say(){ printf '\n\033[1;36m[WEB2APK V4.8.6]\033[0m %s\n' "$*"; }
fail(){ printf '\n\033[1;31m[ERROR]\033[0m %s\n' "$*" >&2; exit 1; }

# This script builds WEB2APK V4 itself. Website APKs are still built internally
# by the installed app; Termux is only needed for this one-time builder install.
if [ -z "${ANDROID_HOME:-}" ]; then
  if [ -d "$HOME/android-sdk" ]; then
    export ANDROID_HOME="$HOME/android-sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then
    export ANDROID_HOME="$HOME/Android/Sdk"
  else
    fail "Android SDK not found. Expected ~/android-sdk. Check ANDROID_HOME."
  fi
fi
export ANDROID_SDK_ROOT="$ANDROID_HOME"

command -v java >/dev/null 2>&1 || fail "Java is missing. Install OpenJDK 17 or later in Termux."
JAVA_VERSION="$(java -version 2>&1 | sed -n '1s/.*version \"\([0-9]*\).*/\1/p')"
[ -n "$JAVA_VERSION" ] || JAVA_VERSION=0
[ "$JAVA_VERSION" -ge 17 ] || fail "Java 17+ is required; detected Java $JAVA_VERSION."

ANDROID_JAR="$ANDROID_HOME/platforms/android-33/android.jar"
[ -s "$ANDROID_JAR" ] || fail "Android SDK Platform 33 is missing or incomplete: $ANDROID_JAR"

# Do not hard-require Build Tools 34.0.0: Termux SDK installations often have
# 34.0.4 or another patch version instead. Prefer explicit override, then 34.0.0,
# then the highest installed numeric version.
BT_ROOT="$ANDROID_HOME/build-tools"
[ -d "$BT_ROOT" ] || fail "Android Build Tools folder is missing: $BT_ROOT"
if [ -n "${WEB2APK_BUILD_TOOLS_VERSION:-}" ]; then
  BT_VERSION="$WEB2APK_BUILD_TOOLS_VERSION"
  [ -d "$BT_ROOT/$BT_VERSION" ] || fail "Requested Build Tools $BT_VERSION is not installed in $BT_ROOT."
elif [ -d "$BT_ROOT/34.0.0" ]; then
  BT_VERSION="34.0.0"
else
  BT_VERSION="$(find "$BT_ROOT" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' 2>/dev/null \
    | grep -E '^[0-9]+([.][0-9]+){1,3}$' | sort -V | tail -n 1 || true)"
  [ -n "$BT_VERSION" ] || fail "No installed Android Build Tools version was found in $BT_ROOT."
fi
[ -d "$BT_ROOT/$BT_VERSION" ] || fail "Selected Build Tools directory is missing: $BT_ROOT/$BT_VERSION"

# Keep Gradle's AAPT2 tool native to ARM64 Termux when available. The Maven
# AAPT2 binary selected by AGP can be x86_64-only on some Android/Termux setups.
AAPT2="${PREFIX:-/data/data/com.termux/files/usr}/bin/aapt2"
if [ -x "$AAPT2" ]; then
  if ! "$AAPT2" version >/dev/null 2>&1; then
    fail "Termux AAPT2 exists but cannot execute: $AAPT2. Run: pkg reinstall aapt2"
  fi
  if grep -q '^android\.aapt2FromMavenOverride=' "$ROOT/gradle.properties" 2>/dev/null; then
    sed -i "s#^android.aapt2FromMavenOverride=.*#android.aapt2FromMavenOverride=$AAPT2#" "$ROOT/gradle.properties"
  else
    printf '\nandroid.aapt2FromMavenOverride=%s\n' "$AAPT2" >> "$ROOT/gradle.properties"
  fi
  say "Using native Termux AAPT2: $AAPT2"
else
  # Do not leave a stale override pointing to a missing binary.
  sed -i '/^android\.aapt2FromMavenOverride=/d' "$ROOT/gradle.properties" 2>/dev/null || true
  say "Termux AAPT2 not found; Gradle will use its configured AAPT2 artifact."
fi

[ -s "$ROOT/gradle/wrapper/gradle-wrapper.jar" ] || fail "Gradle Wrapper JAR is missing from the source archive."
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$ROOT/local.properties"

say "Android SDK: $ANDROID_HOME"
say "Platform: android-33"
say "Build Tools: $BT_VERSION"
say "Java: $JAVA_VERSION"
say "Building WEB2APK V4.8.6 (first build requires internet for Gradle/Maven dependencies)..."

LOG="$ROOT/build-v4.log"
set +e
bash ./gradlew --no-daemon --stacktrace -Pweb2apkBuildToolsVersion="$BT_VERSION" :builder:assembleDebug 2>&1 | tee "$LOG"
RC=${PIPESTATUS[0]}
set -e
if [ "$RC" -ne 0 ]; then
  printf '\nLast 100 lines of build log (%s):\n' "$LOG" >&2
  tail -n 100 "$LOG" >&2 || true
  fail "Gradle build failed (exit $RC). Send build-v4.log or the last 100 lines for an exact fix."
fi

BUILT="$ROOT/builder/build/outputs/apk/debug/builder-debug.apk"
[ -s "$BUILT" ] || fail "Gradle reported success but builder-debug.apk was not found. Check $LOG"

if [ ! -d "$HOME/storage/downloads" ] && command -v termux-setup-storage >/dev/null 2>&1; then
  say "Setting up Termux shared Downloads link..."
  termux-setup-storage
fi
[ -d "$HOME/storage/downloads" ] || fail "Termux Downloads link is unavailable. Run termux-setup-storage once, allow storage access, then rerun this script."
OUT="$HOME/storage/downloads/Web2APK"
mkdir -p "$OUT"
cp -f "$BUILT" "$OUT/WEB2APK-V4-Internal.apk"

say "BUILD SUCCESSFUL"
say "Saved: $OUT/WEB2APK-V4-Internal.apk"
say "Build log: $LOG"
if command -v termux-open >/dev/null 2>&1; then
  termux-open "$OUT/WEB2APK-V4-Internal.apk" || true
fi
