#!/bin/sh
# gradlew versi ringan (tanpa gradle-wrapper.jar).
# Menjalankan Gradle 8.7: pakai cache kalau ada, kalau belum ada diunduh otomatis.

GRADLE_VERSION=8.7
GUH="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_DIR="$GUH/gradlew-lite"
GRADLE_BIN="$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle"

# 1. Kalau wrapper resmi lengkap dengan jar-nya, pakai cara standar.
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$JAR" ]; then
  if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
  exec "$JAVACMD" -Dorg.gradle.appname=gradlew -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
fi

# 2. Cari Gradle 8.7 yang sudah ada di cache wrapper resmi.
if [ ! -x "$GRADLE_BIN" ]; then
  for c in "$GUH"/wrapper/dists/gradle-$GRADLE_VERSION-bin/*/gradle-$GRADLE_VERSION/bin/gradle; do
    if [ -x "$c" ]; then GRADLE_BIN="$c"; break; fi
  done
fi

# 3. Belum ada -> unduh.
if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$DIST_DIR"
  ZIP="$DIST_DIR/gradle-$GRADLE_VERSION-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  echo "Mengunduh Gradle $GRADLE_VERSION ..." >&2
  ok=0
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$ZIP" "$URL" && ok=1
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$ZIP" "$URL" && ok=1
  fi
  if [ "$ok" = 1 ]; then
    if command -v unzip >/dev/null 2>&1; then
      unzip -q -o "$ZIP" -d "$DIST_DIR"
    else
      python3 -m zipfile -e "$ZIP" "$DIST_DIR"
      chmod +x "$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle" 2>/dev/null
    fi
    rm -f "$ZIP"
  fi
fi

if [ -x "$GRADLE_BIN" ]; then
  exec "$GRADLE_BIN" "$@"
fi

# 4. Terakhir: Gradle bawaan sistem (harus versi 8.7 ke atas).
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "ERROR: Gradle tidak ditemukan dan gagal diunduh (cek koneksi internet)." >&2
exit 1
