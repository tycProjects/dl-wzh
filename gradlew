#!/usr/bin/env sh
# Lightweight Gradle launcher for environments where the wrapper JAR is unavailable.
set -eu
GRADLE_VERSION=8.9
GRADLE_HOME_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/nomango-gradle/gradle-$GRADLE_VERSION"
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
if [ ! -x "$GRADLE_HOME_DIR/bin/gradle" ]; then
  BASE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/nomango-gradle"
  mkdir -p "$BASE_DIR"
  ARCHIVE="$BASE_DIR/gradle-$GRADLE_VERSION-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$URL"
  else
    echo "Gradle 8.9 is not installed and curl/wget is unavailable." >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ARCHIVE" -d "$BASE_DIR"
  else
    echo "unzip is required to install Gradle 8.9." >&2
    exit 1
  fi
fi
exec "$GRADLE_HOME_DIR/bin/gradle" "$@"
