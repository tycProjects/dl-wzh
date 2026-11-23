#!/usr/bin/env bash
# Lightweight Gradle bootstrap for Android/Termux environments.
# Downloads the official Gradle distribution and verifies its published SHA-256.
set -euo pipefail

VERSION="8.13"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-${HOME:?HOME is not set}/.gradle}"
BASE="$GRADLE_USER_HOME/wrapper/dists/ryomd-bootstrap/$VERSION"
DIST="$BASE/gradle-$VERSION"

if [[ -x "$DIST/bin/gradle" ]]; then
  exec "$DIST/bin/gradle" "$@"
fi

if command -v gradle >/dev/null 2>&1; then
  INSTALLED="$(gradle --version 2>/dev/null | sed -n 's/^Gradle \([0-9.]*\)$/\1/p' | head -n 1)"
  if [[ "$INSTALLED" == "$VERSION" ]]; then
    exec gradle "$@"
  fi
fi

for cmd in curl unzip sha256sum; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing command: $cmd" >&2
    echo "Install it in Termux, for example: pkg install curl unzip coreutils" >&2
    exit 127
  fi
done

mkdir -p "$BASE"
TMP="$(mktemp -d "$BASE/.download.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT
URL="https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
echo "Downloading official Gradle $VERSION distribution..."
curl --fail --location --retry 2 "$URL" -o "$TMP/gradle.zip"
curl --fail --location --retry 2 "$URL.sha256" -o "$TMP/gradle.zip.sha256"
EXPECTED="$(tr -d '[:space:]' < "$TMP/gradle.zip.sha256" | tr '[:upper:]' '[:lower:]')"
ACTUAL="$(sha256sum "$TMP/gradle.zip" | awk '{print tolower($1)}')"
if [[ -z "$EXPECTED" || "$ACTUAL" != "$EXPECTED" ]]; then
  echo "Gradle SHA-256 verification failed. Archive was not installed." >&2
  exit 1
fi
unzip -q "$TMP/gradle.zip" -d "$TMP/unpacked"
if [[ ! -x "$TMP/unpacked/gradle-$VERSION/bin/gradle" ]]; then
  echo "Unexpected Gradle archive layout." >&2
  exit 1
fi
rm -rf "$DIST"
mv "$TMP/unpacked/gradle-$VERSION" "$DIST"
echo "Gradle $VERSION is ready."
exec "$DIST/bin/gradle" "$@"
