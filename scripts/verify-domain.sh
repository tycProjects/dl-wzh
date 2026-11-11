#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
if ! command -v kotlinc >/dev/null 2>&1; then
  echo "Kotlin compiler not found. Run ./gradlew test in Android Studio instead."
  exit 1
fi
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT
kotlinc "$ROOT/app/src/main/java/com/everline/lifesim/domain/SimulationRules.kt" "$ROOT/scripts/DomainSmoke.kt" -include-runtime -d "$TMP_DIR/everline-domain-tests.jar"
java -jar "$TMP_DIR/everline-domain-tests.jar"
