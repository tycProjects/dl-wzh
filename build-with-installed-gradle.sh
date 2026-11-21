#!/usr/bin/env bash
# Build without downloading the Gradle distribution through the wrapper.
# Uses a `gradle` that is already installed on the machine (for example the
# Gradle 8.9 that the build pipeline ships with). Falls back to ./gradlew only
# when no installed Gradle is found.
#
# Usage:  ./build-with-installed-gradle.sh :app:assembleRelease
set -euo pipefail

TASK="${*:-:app:assembleRelease}"

if command -v gradle >/dev/null 2>&1; then
  echo "Using installed Gradle: $(gradle --version | sed -n 's/^Gradle //p')"
  exec gradle $TASK --no-daemon
fi

echo "No installed 'gradle' found; falling back to ./gradlew (needs network access)."
exec ./gradlew $TASK --no-daemon
