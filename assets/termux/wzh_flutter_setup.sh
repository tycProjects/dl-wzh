# WZH 2 APK: installs the Flutter tools on this phone (one time, a few GB). Runs in Termux.
# Flutter needs a normal Linux (glibc), so it lives in a Debian system inside Termux (proot-distro).
export DEBIAN_FRONTEND=noninteractive
wzh_flutter_main() {
  echo "== WZH 2 APK: installing the Flutter tools =="
  echo "Use Wi-Fi and keep this screen open. This takes a while."
  pkg update -y
  for p in proot-distro curl unzip; do pkg install -y "$p" || echo "could not install $p"; done
  if ! command -v proot-distro >/dev/null 2>&1; then echo "MISSING  proot-distro"; return 1; fi
  if ! proot-distro login debian -- true >/dev/null 2>&1; then
    proot-distro install debian || { echo "Could not install Debian."; return 1; }
  fi
  proot-distro login debian -- bash -s <<'WZH_INNER'
export DEBIAN_FRONTEND=noninteractive HOME=/root BOT=true CI=true
rm -f /root/.wzh_flutter_ok
echo "-- Debian packages"
apt-get update -y
# One by one, so a package this Debian does not have cannot stop the others (git is needed for Flutter).
for p in curl git unzip xz-utils zip ca-certificates file python3 cmake ninja-build clang lld llvm; do
  apt-get install -y "$p" >/dev/null 2>&1 || apt-get install -y "$p" || echo "WARN: could not install $p"
done
command -v javac >/dev/null 2>&1 || apt-get install -y openjdk-21-jdk-headless || apt-get install -y openjdk-17-jdk-headless || apt-get install -y default-jdk-headless || echo "WARN: could not install the Java compiler (JDK)"
for p in aapt aapt2 android-sdk-build-tools; do
  apt-get install -y "$p" >/dev/null 2>&1 && echo "   installed $p" || echo "   no Debian package: $p"
done
command -v git >/dev/null 2>&1 || { echo "MISSING  git (Flutter cannot be downloaded without it)"; }
A=""
for c in /usr/bin/aapt2 $(dpkg -L aapt 2>/dev/null | grep '/aapt2$'); do [ -x "$c" ] && A="$c" && break; done
echo "-- aapt2: ${A:-not found}"
git config --global --add safe.directory '*' >/dev/null 2>&1
echo "-- Flutter SDK"
if [ ! -x /root/flutter/bin/flutter ]; then
  git clone --depth 1 -b stable https://github.com/flutter/flutter.git /root/flutter || echo "WARN: could not download Flutter"
fi
export PATH=/root/flutter/bin:$PATH
flutter config --no-analytics >/dev/null 2>&1
echo "-- Flutter Android tools (this is the big download)"
flutter precache --android
echo "-- gen_snapshot"
is_arm() { [ "$(od -An -tx1 -j18 -N1 "$1" 2>/dev/null | tr -d ' \n')" = "b7" ]; }
find_gs() { find /root/flutter/bin/cache/artifacts/engine -path '*android-arm64-release*' -name 'gen_snapshot*' ! -name '*.real' -type f 2>/dev/null | head -1; }
gs_runs() {
  local o; o=$("$1" --version 2>&1 | head -n 5)
  echo "$o" | grep -qiE 'dart|snapshot|usage' && ! echo "$o" | grep -qiE 'exec format|no such file|cannot execute|invalid elf|unable to'
}
GS="/root/flutter/bin/cache/artifacts/engine/android-arm64-release/linux-x64/gen_snapshot"
if [ -f "$GS" ] && ! is_arm "$GS" && [ ! -f "$GS.real" ] && [ "$(od -An -tx1 -j18 -N1 "$GS" 2>/dev/null | tr -d ' \n')" = "3e" ]; then
  echo "   Flutter only ships gen_snapshot for x86-64 PCs. Setting up an emulator so this phone can run it (builds are slower)."
  dpkg --add-architecture amd64 >/dev/null 2>&1
  apt-get update -y >/dev/null 2>&1
  apt-get install -y libc6:amd64 >/dev/null 2>&1 || apt-get install -y libc6:amd64 || echo "WARN: could not install the x86-64 runtime"
  apt-get install -y qemu-user >/dev/null 2>&1 || apt-get install -y qemu-user-static || echo "WARN: could not install qemu-user"
  Q=$(command -v qemu-x86_64 || command -v qemu-x86_64-static)
  if [ -n "$Q" ]; then
    mv "$GS" "$GS.real"
    cat > "$GS" <<EOF_GS
#!/bin/sh
exec $Q -L / "$GS.real" "\$@"
EOF_GS
    chmod 755 "$GS" "$GS.real"
    echo "   gen_snapshot now runs through $Q"
  else
    echo "WARN: no qemu-x86_64 found"
  fi
fi
wzh_gs_link() {
  # Newer Flutter looks for gen_snapshot in linux-arm64 on this kind of phone, so point that folder at the emulated x86-64 one.
  local E=/root/flutter/bin/cache/artifacts/engine/android-arm64-release R Q
  [ -x "$E/linux-arm64/gen_snapshot" ] && return 0
  Q=$(command -v qemu-x86_64 || command -v qemu-x86_64-static); [ -n "$Q" ] || return 1
  R="$E/linux-x64/gen_snapshot.real"; [ -f "$R" ] || R="$E/linux-x64/gen_snapshot"
  [ -f "$R" ] || return 1
  mkdir -p "$E/linux-arm64"
  printf '#!/bin/sh\nexec %s -L / %s "$@"\n' "$Q" "$R" > "$E/linux-arm64/gen_snapshot"
  chmod 755 "$E/linux-arm64/gen_snapshot"
}
wzh_gs_link
echo "-- aapt2 (newer)"
# aapt2: Debian's is too old to read the android-36 platform ("RES_TABLE_TYPE_TYPE entry offsets overlap").
# Use Google's own newest aapt2 (x86-64) through the emulator instead. Falls back to Debian's if anything fails.
wzh_aapt2_x64() {
  [ -x /root/aapt2-wrap/aapt2 ] && return 0
  local Q V J
  Q=$(command -v qemu-x86_64 || command -v qemu-x86_64-static)
  [ -n "$Q" ] || { echo "   no emulator, keeping Debian's aapt2"; return 1; }
  V=$(curl -fsSL --max-time 60 https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/maven-metadata.xml 2>/dev/null | grep -o '<release>[^<]*' | cut -d'>' -f2 | head -n1)
  [ -n "$V" ] || V=8.9.1-12782657
  J=/tmp/aapt2-x64.jar
  curl -fsSL --retry 3 --max-time 300 -o "$J" "https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/$V/aapt2-$V-linux.jar" || { echo "   could not download aapt2 $V"; return 1; }
  rm -rf /root/aapt2-x64; mkdir -p /root/aapt2-x64 /root/aapt2-wrap
  unzip -q -o "$J" -d /root/aapt2-x64 || return 1
  chmod 755 /root/aapt2-x64/aapt2
  printf '#!/bin/sh\nexec %s -L / /root/aapt2-x64/aapt2 "$@"\n' "$Q" > /root/aapt2-wrap/aapt2
  chmod 755 /root/aapt2-wrap/aapt2
  if /root/aapt2-wrap/aapt2 version 2>&1 | grep -qi 'aapt'; then echo "   aapt2 $V runs through the emulator"; return 0; fi
  echo "   aapt2 $V did not start, keeping Debian's"; rm -rf /root/aapt2-wrap /root/aapt2-x64; return 1
}
wzh_use_aapt2() {
  [ -x /root/aapt2-wrap/aapt2 ] || return 0
  mkdir -p /root/.gradle; touch /root/.gradle/gradle.properties
  sed -i '/^android.aapt2FromMavenOverride=/d' /root/.gradle/gradle.properties
  echo "android.aapt2FromMavenOverride=/root/aapt2-wrap/aapt2" >> /root/.gradle/gradle.properties
}
wzh_aapt2_x64
echo "-- Android SDK"
S=/root/android-sdk
if [ ! -x $S/cmdline-tools/latest/bin/sdkmanager ]; then
  mkdir -p $S/cmdline-tools
  curl -fL -o /tmp/ct.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
    && unzip -q -o /tmp/ct.zip -d $S/cmdline-tools && mv $S/cmdline-tools/cmdline-tools $S/cmdline-tools/latest
fi
SM=$S/cmdline-tools/latest/bin/sdkmanager
yes | $SM --sdk_root=$S --licenses >/dev/null 2>&1
for pk in "platform-tools" "platforms;android-36" "build-tools;36.0.0" "platforms;android-35" "build-tools;35.0.0"; do
  $SM --sdk_root=$S "$pk" >/dev/null 2>&1 && echo "   installed $pk" || echo "   skipped $pk"
done
export ANDROID_HOME=$S ANDROID_SDK_ROOT=$S
flutter config --android-sdk $S >/dev/null 2>&1
yes | flutter doctor --android-licenses >/dev/null 2>&1
mkdir -p /root/.gradle
{
  echo "org.gradle.daemon=false"
  echo "org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m"
  echo "org.gradle.workers.max=2"
  [ -n "$A" ] && echo "android.aapt2FromMavenOverride=$A"
} > /root/.gradle/gradle.properties
wzh_use_aapt2
echo
echo "== Result =="
OK=1
chk() { if [ "$2" = 1 ]; then echo "  ok       $1"; else echo "  MISSING  $1"; OK=0; fi; }
flutter --version >/dev/null 2>&1 && chk "flutter" 1 || chk "flutter" 0
javac -version >/dev/null 2>&1 && chk "java (JDK with compiler)" 1 || chk "java (JDK with compiler)" 0
[ -n "$A" ] && chk "aapt2 (arm64)" 1 || chk "aapt2 (arm64)" 0
[ -x $SM ] && chk "android sdk" 1 || chk "android sdk" 0
GS=$(find_gs)
if [ -n "$GS" ] && is_arm "$GS"; then chk "gen_snapshot (arm64)" 1
elif [ -n "$GS" ] && gs_runs "$GS"; then chk "gen_snapshot (runs through an emulator, slower)" 1
else echo "  MISSING  gen_snapshot for this phone: ${GS:+$(file "$GS" | cut -c1-90)}"; OK=0; fi
if [ "$OK" = 1 ]; then touch /root/.wzh_flutter_ok; echo; echo "All set."; else echo; echo "Not everything installed. Use Full build for Flutter projects."; fi
WZH_INNER
  echo
  echo "Done. Go back to WZH 2 APK."
}
wzh_flutter_main
