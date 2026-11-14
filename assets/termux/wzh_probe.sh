# WZH 2 APK: tells the app which build tools Termux has. $1 = the app's local address
B="$1"
r=""
for t in java javac jar kotlinc aapt2 d8 dx zip unzip curl python3; do
  if command -v "$t" >/dev/null 2>&1; then r="$r $t=1"; else r="$r $t=0"; fi
done
curl -s -m 20 -X POST --data-binary "$r" "$B/probe" >/dev/null 2>&1
