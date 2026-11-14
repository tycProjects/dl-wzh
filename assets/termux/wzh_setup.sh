# WZH 2 APK: installs the build tools in Termux (one time). Shown in a visible Termux window.
export DEBIAN_FRONTEND=noninteractive
echo "== WZH 2 APK: installing build tools =="
pkg update -y
inst() { for p in "$@"; do if pkg install -y "$p"; then return 0; fi; done; return 1; }
inst openjdk-17 openjdk-21
inst kotlin
inst aapt2
inst d8 dalvik-exchange
inst zip
inst unzip
inst curl
inst python
inst nodejs
echo
echo "== Result =="
for t in java javac jar kotlinc aapt2 d8 zip unzip curl python3; do
  if command -v "$t" >/dev/null 2>&1; then echo "  ok       $t"; else echo "  MISSING  $t"; fi
done
echo
echo "Done. Go back to WZH 2 APK and tap Check."
