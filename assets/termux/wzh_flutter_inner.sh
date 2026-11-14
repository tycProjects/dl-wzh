# WZH 2 APK: runs inside the Debian system. $1 = the project folder under /work.
# Env: WZH_APPID, WZH_NAME (optional). Steps follow the server's Flutter workflow (flutterBuilder.js).
export HOME=/root BOT=true CI=true ANDROID_HOME=/root/android-sdk ANDROID_SDK_ROOT=/root/android-sdk
export PATH=/root/flutter/bin:$PATH
git config --global --add safe.directory '*' >/dev/null 2>&1
cd "/work/$1" || exit 11
echo "[wzh] Flutter project: /work/$1"
if ! command -v python3 >/dev/null 2>&1; then
  echo "[wzh] installing python3 (one time)"
  apt-get update -y >/dev/null 2>&1; apt-get install -y python3 >/dev/null 2>&1
fi

# The Java compiler (javac) is required; a runtime alone is not enough. Installs it once if it is missing.
if ! command -v javac >/dev/null 2>&1; then
  echo "[wzh] installing the Java compiler (one time)"
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -y >/dev/null 2>&1
  apt-get install -y openjdk-21-jdk-headless || apt-get install -y openjdk-17-jdk-headless || apt-get install -y default-jdk-headless >/dev/null 2>&1
  command -v javac >/dev/null 2>&1 || echo "[wzh] WARN: could not install the Java compiler"
fi

# An android/ folder without its manifest is a partial export: replace it (keeping any jniLibs).
JNI=""
if [ -d android/app/src/main/jniLibs ]; then JNI=$(mktemp -d)/jniLibs; cp -r android/app/src/main/jniLibs "$JNI"; fi
if [ -d android ] && [ ! -f android/app/src/main/AndroidManifest.xml ]; then
  echo "[wzh] android/ is incomplete (no AndroidManifest.xml), generating a new one"
  rm -rf android
fi
if [ ! -d android ]; then
  echo "[wzh] generating the Android part with flutter create"
  flutter create --platforms=android --org com.wzh.generated . || exit 14
  if [ -n "$JNI" ]; then mkdir -p android/app/src/main/jniLibs && cp -rn "$JNI"/* android/app/src/main/jniLibs/; fi
fi
[ -d android/app/src/main/jniLibs ] && find android/app/src/main/jniLibs -type f -exec chmod 755 {} +

# The app id and name chosen in the editor.
if [ -n "$WZH_APPID" ]; then
  for G in android/app/build.gradle android/app/build.gradle.kts; do
    [ -f "$G" ] && sed -i -E "s/(applicationId[[:space:]]*=?[[:space:]]*)\"[^\"]*\"/\1\"$WZH_APPID\"/" "$G"
  done
fi
SN=$(printf '%s' "$WZH_NAME" | tr -d '"&\\/|<>')
MF=android/app/src/main/AndroidManifest.xml
[ -n "$SN" ] && [ -f "$MF" ] && sed -i -E "s/android:label=\"[^\"]*\"/android:label=\"$SN\"/" "$MF"

# Gradle settings for every project: no lint crash, Google's Maven Central mirror, longer download timeouts.
mkdir -p /root/.gradle/init.d
cat > /root/.gradle/init.d/skip-lint-vital.gradle <<'EOF_A'
allprojects { p ->
    p.tasks.configureEach { t ->
        if (t.name.startsWith('lintVital')) { t.enabled = false }
    }
}
EOF_A
cat > /root/.gradle/init.d/central-mirror.gradle <<'EOF_B'
def MIRROR = 'https://maven-central.storage-download.googleapis.com/maven2/'
gradle.ext.mirrorFailOnProject = false
try {
  gradle.beforeSettings { s ->
    try { s.pluginManagement.repositories { maven { url MIRROR }; gradlePluginPortal() } } catch (Throwable t) { }
  }
} catch (Throwable t) { }
try {
  gradle.settingsEvaluated { s ->
    try { gradle.ext.mirrorFailOnProject = s.dependencyResolutionManagement.repositoriesMode.get().toString().contains('FAIL') } catch (Throwable t) { }
  }
} catch (Throwable t) { }
try {
  gradle.allprojects { p ->
    if (!gradle.ext.mirrorFailOnProject) {
      try { p.buildscript.repositories { maven { url MIRROR } } } catch (Throwable t) { }
      try { p.repositories { maven { url MIRROR } } } catch (Throwable t) { }
    }
  }
} catch (Throwable t) { }
EOF_B
grep -q "org.gradle.internal.http.connectionTimeout" /root/.gradle/gradle.properties 2>/dev/null || cat >> /root/.gradle/gradle.properties <<'EOF_C'
org.gradle.internal.http.connectionTimeout=120000
org.gradle.internal.http.socketTimeout=120000
org.gradle.internal.repository.max.retries=8
org.gradle.internal.repository.initial.backoff=2000
EOF_C

# aapt2: Debian's is too old to read the android-36 platform ("RES_TABLE_TYPE_TYPE entry offsets overlap").
# Use Google's own newest aapt2 (x86-64) through the emulator instead. Falls back to Debian's if anything fails.
wzh_aapt2_x64() {
  [ -x /root/aapt2-wrap/aapt2 ] && return 0
  local Q V J
  Q=$(command -v qemu-x86_64 || command -v qemu-x86_64-static)
  [ -n "$Q" ] || { echo "[wzh] no emulator, keeping Debian's aapt2"; return 1; }
  V=$(curl -fsSL --max-time 60 https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/maven-metadata.xml 2>/dev/null | grep -o '<release>[^<]*' | cut -d'>' -f2 | head -n1)
  [ -n "$V" ] || V=8.9.1-12782657
  J=/tmp/aapt2-x64.jar
  curl -fsSL --retry 3 --max-time 300 -o "$J" "https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/$V/aapt2-$V-linux.jar" || { echo "[wzh] could not download aapt2 $V"; return 1; }
  rm -rf /root/aapt2-x64; mkdir -p /root/aapt2-x64 /root/aapt2-wrap
  unzip -q -o "$J" -d /root/aapt2-x64 || return 1
  chmod 755 /root/aapt2-x64/aapt2
  printf '#!/bin/sh\nexec %s -L / /root/aapt2-x64/aapt2 "$@"\n' "$Q" > /root/aapt2-wrap/aapt2
  chmod 755 /root/aapt2-wrap/aapt2
  if /root/aapt2-wrap/aapt2 version 2>&1 | grep -qi 'aapt'; then echo "[wzh] aapt2 $V runs through the emulator"; return 0; fi
  echo "[wzh] aapt2 $V did not start, keeping Debian's"; rm -rf /root/aapt2-wrap /root/aapt2-x64; return 1
}
wzh_use_aapt2() {
  [ -x /root/aapt2-wrap/aapt2 ] || return 0
  mkdir -p /root/.gradle; touch /root/.gradle/gradle.properties
  sed -i '/^android.aapt2FromMavenOverride=/d' /root/.gradle/gradle.properties
  echo "android.aapt2FromMavenOverride=/root/aapt2-wrap/aapt2" >> /root/.gradle/gradle.properties
}
wzh_aapt2_x64 >/dev/null 2>&1
wzh_use_aapt2

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

echo "[wzh] applying the project fixes"
python3 /work/wzh_flutter_patch.py

# Gradle is served from GitHub, which sometimes answers 504 for minutes: fetch it ourselves first (official
# address with retries, then public mirrors) and point the wrapper at the copy. Never fails the build.
P=android/gradle/wrapper/gradle-wrapper.properties
if [ -f "$P" ]; then
  URL=$(grep '^distributionUrl=' "$P" | head -n1 | cut -d= -f2- | sed 's#\\:#:#g' | tr -d '[:space:]')
  case "$URL" in
    https://*)
      FILE=$(basename "$URL")
      SHA=$(grep '^distributionSha256Sum=' "$P" | head -n1 | cut -d= -f2- | tr -d '[:space:]')
      DEST=/root/gradle-dist; mkdir -p "$DEST"
      if [ -s "$DEST/$FILE" ] && unzip -tq "$DEST/$FILE" >/dev/null 2>&1; then
        sed -i "s#^distributionUrl=.*#distributionUrl=file:///root/gradle-dist/$FILE#" "$P"
        echo "[wzh] Gradle wrapper uses the saved copy of $FILE"
      else
        for SRC in "$URL" "https://repo.huaweicloud.com/gradle/$FILE" "https://mirrors.cloud.tencent.com/gradle/$FILE"; do
          echo "[wzh] Gradle: trying $SRC"
          rm -f "$DEST/$FILE"
          if curl -fsSL --retry 4 --retry-all-errors --retry-delay 8 --connect-timeout 20 --max-time 600 -o "$DEST/$FILE" "$SRC" && unzip -tq "$DEST/$FILE" >/dev/null 2>&1; then
            if [ -n "$SHA" ] && [ "$(sha256sum "$DEST/$FILE" | cut -d' ' -f1)" != "$SHA" ]; then echo "[wzh] checksum differs, skipping"; continue; fi
            sed -i "s#^distributionUrl=.*#distributionUrl=file:///root/gradle-dist/$FILE#" "$P"
            echo "[wzh] Gradle wrapper uses the downloaded copy"
            break
          fi
        done
      fi ;;
  esac
fi


# ---- native code (cmake / NDK) on an arm64 phone ---------------------------------------------------------
# Google only ships x86-64 cmake and NDK compilers. They cannot run in this Debian, so plugins with native code
# (for example jni) fail with CXX1429. Fix: use Debian's own arm64 cmake, ninja, clang and lld, and keep only the
# NDK's headers and libraries (those are plain files and work on any CPU).
wzh_native_tools() {
  local p
  export DEBIAN_FRONTEND=noninteractive
  for p in cmake ninja-build clang lld llvm; do
    dpkg -s "$p" >/dev/null 2>&1 && continue
    echo "[wzh] installing $p (one time)"
    apt-get update -y >/dev/null 2>&1
    apt-get install -y "$p" >/dev/null 2>&1 || echo "[wzh] WARN: could not install $p"
  done
}

wzh_ndk_wrap() {
  # $1 = tool name inside the NDK bin folder, $2 = real tool on this phone (arm64), $3... = extra arguments
  local B="$WZH_NDK_BIN" name="$1" real="$2"; shift 2
  [ -n "$real" ] || return 0
  rm -f "$B/$name"
  printf '#!/bin/sh\nexec %s %s "$@"\n' "$real" "$*" > "$B/$name"
  chmod 755 "$B/$name"
}

# Returns 0 when it changed at least one NDK, 1 when there was nothing to do.
wzh_ndk_fix() {
  local N B SR RD CL CXX t R changed=1
  CL=$(command -v clang); CXX=$(command -v clang++)
  [ -n "$CL" ] || { echo "[wzh] no arm64 clang, cannot fix the NDK"; return 1; }
  for N in /root/android-sdk/ndk/*; do
    B="$N/toolchains/llvm/prebuilt/linux-x86_64/bin"
    [ -d "$B" ] || continue
    [ -f "$B/.wzh_native" ] && continue
    SR="$N/toolchains/llvm/prebuilt/linux-x86_64/sysroot"
    RD=$(ls -d "$N"/toolchains/llvm/prebuilt/linux-x86_64/lib/clang/*/ 2>/dev/null | head -n1); RD="${RD%/}"
    echo "[wzh] making NDK $(basename "$N") run on this phone"
    export WZH_NDK_BIN="$B"
    local CF="-resource-dir $RD --sysroot=$SR -fuse-ld=lld -Wno-unused-command-line-argument"
    wzh_ndk_wrap clang "$CL" $CF
    wzh_ndk_wrap clang++ "${CXX:-$CL++}" $CF
    for t in "$B"/clang-[0-9]*; do [ -e "$t" ] || [ -L "$t" ] || continue; wzh_ndk_wrap "$(basename "$t")" "$CL" $CF; done
    R=$(command -v ld.lld); wzh_ndk_wrap ld.lld "$R"; wzh_ndk_wrap ld "$R"; wzh_ndk_wrap lld "$R" -flavor gnu
    for t in llvm-ar llvm-ranlib llvm-strip llvm-nm llvm-objcopy llvm-objdump llvm-readelf llvm-readobj llvm-size llvm-as llvm-addr2line llvm-symbolizer; do
      R=$(command -v "$t" || ls /usr/bin/"$t"-[0-9]* 2>/dev/null | head -n1)
      [ -n "$R" ] && [ -e "$B/$t" -o -L "$B/$t" ] && wzh_ndk_wrap "$t" "$R"
    done
    touch "$B/.wzh_native"
    changed=0
  done
  return $changed
}

wzh_ndk_prepare() {
  # Installs the NDK version the project asks for (so Gradle does not download it first), then fixes it.
  local V S=/root/android-sdk
  V=$(grep -hoE 'ndkVersion[^"]*"[0-9]+\.[0-9]+\.[0-9]+[^"]*"' android/app/build.gradle android/app/build.gradle.kts 2>/dev/null | head -n1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+[0-9.]*')
  [ -n "$V" ] || V=$(grep -rhoE 'ndkVersion[^"]*"[0-9]+\.[0-9]+\.[0-9]+[^"]*"' /root/flutter/packages/flutter_tools/gradle/src/main 2>/dev/null | head -n1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+[0-9.]*')
  if [ -n "$V" ] && [ ! -d "$S/ndk/$V" ] && [ -x "$S/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "[wzh] downloading NDK $V (one time, large)"
    yes | "$S/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$S" "ndk;$V" >/dev/null 2>&1 || echo "[wzh] could not download NDK $V now, Gradle will try"
  fi
  wzh_ndk_fix || true
}
wzh_native_tools
wzh_ndk_prepare

flutter pub get || exit 12

# Use Debian's cmake (Google's cmake is x86-64 only). Written after pub get, which rewrites local.properties.
if [ -f android/local.properties ]; then sed -i '/^cmake\.dir=/d' android/local.properties; fi
printf '\ncmake.dir=/usr\n' >> android/local.properties

# Re-run the build (up to 5 times) only when the log shows a network failure, like the server does.
attempt=1
while true; do
  flutter build apk --release --target-platform android-arm64 2>&1 | tee /work/build.log
  code=${PIPESTATUS[0]}
  [ "$code" -eq 0 ] && exit 0
  # Native code failed because an x86-64 NDK/cmake tool cannot run here: fix the NDK Gradle just downloaded, retry.
  if [ "${fixes:-0}" -lt 3 ] && grep -qiE 'CXX1[0-9]{3}|exec format error|ndk.*(not|unable|failed)|cmake.*(failed|error)' /work/build.log; then
    wzh_native_tools
    if wzh_ndk_fix; then fixes=$((${fixes:-0} + 1)); echo "[wzh] fixed the NDK for this phone, trying the build again"; continue; fi
  fi
  if [ "$attempt" -lt 5 ] && grep -qiE 'Could not (GET|HEAD|download|resolve|read)|Gateway Time-out|timed out|Connection reset|Connection refused|Received status code 5[0-9][0-9]|Server returned HTTP response code: 5[0-9][0-9]|Premature end of Content-Length|Remote host terminated|SocketTimeoutException|UnknownHostException|SSLHandshakeException|Temporary failure' /work/build.log; then
    echo "[wzh] network error while downloading, trying again in $((attempt * 30))s (attempt $attempt of 5)"
    sleep $((attempt * 30)); attempt=$((attempt + 1)); continue
  fi
  exit 13
done
