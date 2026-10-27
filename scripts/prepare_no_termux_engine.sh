#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <flutter-host-project>" >&2
  exit 2
fi
HOST_DIR="$(cd "$1" && pwd)"
VENDOR="$HOST_DIR/vendor/pr"
ENGINE="$VENDOR/android/proot-engine/src/main/jniLibs/arm64-v8a"
TARGET="aarch64-linux-android"

command -v git >/dev/null || { echo "git is required on the development/build host." >&2; exit 3; }
command -v cargo >/dev/null || { echo "Rust cargo is required on the development/build host." >&2; exit 3; }
command -v python3 >/dev/null || { echo "python3 is required on the development/build host." >&2; exit 3; }

mkdir -p "$HOST_DIR"
if [[ ! -d "$VENDOR/.git" ]]; then
  rm -rf "$VENDOR"
  mkdir -p "$(dirname "$VENDOR")"
  git clone --depth 1 --recurse-submodules https://github.com/oonid/pr.git "$VENDOR"
else
  git -C "$VENDOR" submodule update --init --recursive
fi

# The upstream script builds the Android-native PRoot and its loader with the NDK.
"$VENDOR/scripts/build.sh" --arch=arm64

# pr-cli embeds its Android integration test executable, so build that first.
(
  cd "$VENDOR/src/proot-integration-test"
  cargo build --target "$TARGET" --release
)
(
  cd "$VENDOR/src/pr-cli"
  cargo build --target "$TARGET" --release
)

mkdir -p "$ENGINE"
copy_required() {
  local src="$1" dst="$2"
  if [[ ! -f "$src" ]]; then
    echo "Required upstream binary missing: $src" >&2
    echo "Review upstream build output and README before packaging this APK." >&2
    exit 4
  fi
  if [[ "$(realpath "$src")" != "$(realpath -m "$dst")" ]]; then
    cp "$src" "$dst"
  fi
  chmod 755 "$dst"
}
copy_required "$VENDOR/build/out/arm64/proot" "$ENGINE/libproot.so"
copy_required "$VENDOR/build/out/arm64/loader" "$ENGINE/libproot-loader.so"
copy_required "$VENDOR/src/pr-cli/target/$TARGET/release/pr-cli" "$ENGINE/libpr-cli.so"

# BusyBox is part of the upstream project; place its ARM64 shared library beside the engine.
BUSYBOX="$(find "$VENDOR/android" -type f -name libbusybox.so -print -quit || true)"
if [[ -z "$BUSYBOX" ]]; then
  echo "Upstream libbusybox.so was not found. The runtime bootstrap needs this binary." >&2
  echo "Do not package an APK until the upstream BusyBox binary/build step is available." >&2
  exit 5
fi
copy_required "$BUSYBOX" "$ENGINE/libbusybox.so"

# The app needs the same upstream bootstrap asset list used by the PRoot project.
ASSET_SOURCE="$VENDOR/android/session-core/src/main/assets"
if [[ ! -f "$ASSET_SOURCE/bin/busybox.applets" || ! -f "$ASSET_SOURCE/scripts/bootstrap.sh" ]]; then
  echo "Upstream PRoot bootstrap assets are missing at $ASSET_SOURCE" >&2
  exit 6
fi
mkdir -p "$HOST_DIR/android/app/src/main/assets/bin" "$HOST_DIR/android/app/src/main/assets/scripts"
cp "$ASSET_SOURCE/bin/busybox.applets" "$HOST_DIR/android/app/src/main/assets/bin/busybox.applets"
cp "$ASSET_SOURCE/scripts/bootstrap.sh" "$HOST_DIR/android/app/src/main/assets/scripts/bootstrap.sh"

python3 "$(dirname "$0")/patch_flutter_android.py" "$HOST_DIR"
# Patch helper seeds fallback assets; replace them with the project's full upstream assets.
cp "$ASSET_SOURCE/bin/busybox.applets" "$HOST_DIR/android/app/src/main/assets/bin/busybox.applets"
cp "$ASSET_SOURCE/scripts/bootstrap.sh" "$HOST_DIR/android/app/src/main/assets/scripts/bootstrap.sh"
echo "Embedded PRoot engine prepared. The final APK still needs to be built and tested on an ARM64 Android device."
