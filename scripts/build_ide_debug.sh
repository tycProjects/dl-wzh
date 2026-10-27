#!/usr/bin/env bash
set -euo pipefail
SOURCE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOST_DIR="${HOST_DIR:-$SOURCE_ROOT/build/flutter-host}"

command -v flutter >/dev/null || { echo "Flutter SDK is required on the development host to build the Local AIDE app." >&2; exit 2; }
command -v java >/dev/null || { echo "Java is required on the development host." >&2; exit 2; }

if [[ ! -f "$HOST_DIR/pubspec.yaml" || ! -d "$HOST_DIR/android" ]]; then
  mkdir -p "$(dirname "$HOST_DIR")"
  flutter create --platforms=android --org com.localaide --project-name local_aide "$HOST_DIR"
fi
cp "$SOURCE_ROOT/pubspec.yaml" "$HOST_DIR/pubspec.yaml"
mkdir -p "$HOST_DIR/lib"
cp "$SOURCE_ROOT/lib/main.dart" "$HOST_DIR/lib/main.dart"

"$SOURCE_ROOT/scripts/prepare_no_termux_engine.sh" "$HOST_DIR"
cd "$HOST_DIR"
flutter pub get
flutter build apk --debug --target-platform android-arm64

OUT="$SOURCE_ROOT/build/LocalAIDE-NoTermux-debug.apk"
mkdir -p "$(dirname "$OUT")"
cp "$HOST_DIR/build/app/outputs/flutter-apk/app-debug.apk" "$OUT"
echo "Built IDE APK: $OUT"
echo "Install this IDE APK. It will download the target Flutter toolchain inside its own app sandbox when you tap Install toolchain."
