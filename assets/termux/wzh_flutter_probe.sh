# WZH 2 APK: tells the app whether the Flutter tools are installed, and which part is missing. $1 = the app's local address
B="$1"
r="proot=0 flutter=0"
if command -v proot-distro >/dev/null 2>&1; then
  r="proot=1 flutter=0"
  # Checks the tools themselves (not only the "ok" file), so a finished install is always recognised.
  o=$(proot-distro login debian -- bash -s 2>/dev/null <<'WZH_PROBE'
export HOME=/root
F=0; J=0; S=0; A=0; G=0
[ -x /root/flutter/bin/flutter ] && F=1
command -v javac >/dev/null 2>&1 && J=1
[ -x /root/android-sdk/cmdline-tools/latest/bin/sdkmanager ] && S=1
for c in /usr/bin/aapt2 $(dpkg -L aapt 2>/dev/null | grep '/aapt2$'); do [ -x "$c" ] && A=1 && break; done
GS=$(find /root/flutter/bin/cache/artifacts/engine -path '*android-arm64-release*' -name 'gen_snapshot*' ! -name '*.real' -type f 2>/dev/null | head -1)
# ELF machine type 0xb7 = aarch64 (this phone). Otherwise the x86-64 tool must run through the qemu wrapper.
if [ -n "$GS" ] && [ "$(od -An -tx1 -j18 -N1 "$GS" 2>/dev/null | tr -d ' \n')" = "b7" ]; then G=1
elif [ -n "$GS" ]; then
  O=$("$GS" --version 2>&1 | head -n 5)
  echo "$O" | grep -qiE 'dart|snapshot|usage' && ! echo "$O" | grep -qiE 'exec format|no such file|cannot execute|invalid elf|unable to' && G=1
fi
ALL=0
[ $F$J$S$A$G = 11111 ] && { ALL=1; touch /root/.wzh_flutter_ok; }
[ -f /root/.wzh_flutter_ok ] && [ $ALL = 1 ] || rm -f /root/.wzh_flutter_ok
echo "flutter=$ALL has_flutter=$F has_java=$J has_sdk=$S has_aapt2=$A has_gensnapshot=$G"
WZH_PROBE
)
  [ -n "$o" ] && r="proot=1 $(printf '%s' "$o" | tail -n 1)"
fi
curl -s -m 20 -X POST --data-binary "$r" "$B/probe" >/dev/null 2>&1
