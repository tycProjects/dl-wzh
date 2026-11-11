#!/usr/bin/env sh
# Lightweight Gradle bootstrap for environments that require ./gradlew.
set -eu
GRADLE_VERSION="8.2.1"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_DIR="$GRADLE_USER_HOME/manual-dists/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_DIR/gradle-$GRADLE_VERSION/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_USER_HOME/manual-dists"
  ZIP="$GRADLE_USER_HOME/manual-dists/gradle-$GRADLE_VERSION-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  echo "Gradle $GRADLE_VERSION not found; downloading..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 "$URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "$URL"
  else
    echo "Error: curl or wget is required to download Gradle." >&2
    exit 1
  fi
  if ! command -v unzip >/dev/null 2>&1; then
    echo "Error: unzip is required to extract Gradle." >&2
    exit 1
  fi
  mkdir -p "$GRADLE_DIR"
  unzip -q -o "$ZIP" -d "$GRADLE_DIR"
fi
exec "$GRADLE_BIN" "$@"
