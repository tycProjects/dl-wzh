/// Environment inside Ubuntu (sourced from /root/.profile and /root/.bashrc).
const wzhEnv = r'''
export CI=true
export LANG=C.UTF-8
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PUB_CACHE=/root/.pub-cache
export FLUTTER_SUPPRESS_ANALYTICS=true
export PATH=/opt/flutter/bin:/opt/flutter/bin/cache/dart-sdk/bin:/root/.pub-cache/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:$JAVA_HOME/bin:$PATH
alias ll='ls -la'
''';

/// Runs once, visibly, in the app's terminal. Every finished stage leaves a marker file, so running it
/// again after a failure (lost connection, phone went to sleep) picks up where it stopped.
const setupScript = r'''
#!/bin/bash
# WZH Flutter IDE first-run setup.
export DEBIAN_FRONTEND=noninteractive
source /root/.wzhenv
S=/root/.wzh
mkdir -p "$S"
TOTAL=7

stage()  { echo; echo "##STAGE $1/$TOTAL $2"; }
skip()   { echo "   (already done)"; }
fail()   { echo; echo "##FAIL $1"; echo "Setup stopped: $1"; echo "Check your internet connection, then tap Try again. Finished steps are skipped."; exit 1; }
dl()     { curl -fL --retry 5 --retry-delay 3 --retry-all-errors --connect-timeout 20 -o "$2" "$1"; }

git config --global --add safe.directory '*' >/dev/null 2>&1

# 1 ---------------------------------------------------------------
stage 1 "Ubuntu packages"
if [ -f $S/1.done ]; then skip; else
  apt-get update || fail "apt update"
  apt-get install -y --no-install-recommends ca-certificates curl git unzip xz-utils zip file procps less nano \
      openjdk-17-jdk-headless || fail "installing Java and tools"
  apt-get install -y --no-install-recommends aapt >/dev/null 2>&1 || echo "(Ubuntu's aapt is not available, will use the phone's own)"
  touch $S/1.done
fi

# 2 ---------------------------------------------------------------
stage 2 "Checking Java"
java -version 2>&1 | head -1
[ -x "$JAVA_HOME/bin/java" ] || fail "Java is missing"

# 3 ---------------------------------------------------------------
stage 3 "Flutter SDK"
if [ -f $S/3.done ]; then skip; else
  if [ ! -x /opt/flutter/bin/flutter ]; then
    rm -rf /opt/flutter
    git clone --depth 1 -b stable https://github.com/flutter/flutter.git /opt/flutter || fail "downloading Flutter"
  fi
  flutter config --no-analytics >/dev/null 2>&1
  flutter --version || fail "starting Flutter (downloading Dart)"
  touch $S/3.done
fi

# 4 ---------------------------------------------------------------
stage 4 "Android SDK"
if [ -f $S/4.done ]; then skip; else
  if [ ! -x /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager ]; then
    mkdir -p /opt/android-sdk/cmdline-tools /tmp/cmd
    ok=0
    for v in 13114758 11076708 9477386; do
      if dl "https://dl.google.com/android/repository/commandlinetools-linux-${v}_latest.zip" /tmp/cmd/t.zip; then ok=1; break; fi
    done
    [ $ok = 1 ] || fail "downloading the Android command line tools"
    rm -rf /tmp/cmd/x && unzip -q /tmp/cmd/t.zip -d /tmp/cmd/x || fail "unpacking the Android command line tools"
    rm -rf /opt/android-sdk/cmdline-tools/latest
    mv /tmp/cmd/x/cmdline-tools /opt/android-sdk/cmdline-tools/latest
    rm -rf /tmp/cmd
  fi
  yes | sdkmanager --sdk_root=/opt/android-sdk --licenses >/dev/null 2>&1
  sdkmanager --sdk_root=/opt/android-sdk "platform-tools" "platforms;android-36" "platforms;android-35" "build-tools;35.0.0" \
      || fail "installing the Android platform"
  flutter config --android-sdk /opt/android-sdk >/dev/null 2>&1
  touch $S/4.done
fi

# 5 ---------------------------------------------------------------
stage 5 "Resource compiler (aapt2) and build settings"
if [ -f $S/5.done ]; then skip; else
  AAPT2=""
  for c in /usr/bin/aapt2 /usr/lib/android-sdk/build-tools/*/aapt2; do
    if [ -x "$c" ] && "$c" version >/dev/null 2>&1; then AAPT2="$c"; break; fi
  done
  if [ -z "$AAPT2" ] && [ -x /opt/wzh-native/libaapt2.so ]; then
    printf '#!/bin/sh\nexec /opt/wzh-native/libaapt2.so "$@"\n' > /usr/local/bin/aapt2
    chmod +x /usr/local/bin/aapt2
    if /usr/local/bin/aapt2 version >/dev/null 2>&1; then AAPT2=/usr/local/bin/aapt2; fi
  fi
  [ -n "$AAPT2" ] || fail "aapt2 does not run on this phone"
  echo "aapt2: $AAPT2"
  mkdir -p /root/.gradle
  cat > /root/.gradle/gradle.properties <<EOP
android.aapt2FromMavenOverride=$AAPT2
org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
org.gradle.daemon=false
org.gradle.parallel=false
org.gradle.workers.max=2
kotlin.compiler.execution.strategy=in-process
EOP
  touch $S/5.done
fi

# 6 ---------------------------------------------------------------
stage 6 "Flutter build files for Android"
if [ -f $S/6.done ]; then skip; else
  flutter precache --android --no-ios --no-web --no-linux --no-macos --no-windows --no-fuchsia || fail "downloading Flutter's Android files"
  touch $S/6.done
fi

# 7 ---------------------------------------------------------------
stage 7 "Test build (warms up Gradle, can take a while)"
if [ -f $S/7.done ]; then skip; else
  rm -rf /root/.wzh/warm
  if flutter create --project-name warm --org com.wzh /root/.wzh/warm >/dev/null 2>&1; then
    sed -i 's/-Xmx[0-9a-zA-Z]*/-Xmx1536M/' /root/.wzh/warm/android/gradle.properties 2>/dev/null
    if (cd /root/.wzh/warm && flutter build apk --debug --target-platform android-arm64); then
      echo "Test build worked."
    else
      echo "The test build did not finish. You can still use the IDE; check the messages above if builds fail."
    fi
  fi
  rm -rf /root/.wzh/warm
  touch $S/7.done
fi

touch $S/ready
echo
echo "##SETUP_OK"
echo "All set."
''';
