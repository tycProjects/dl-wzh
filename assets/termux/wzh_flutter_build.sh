# WZH 2 APK: on-phone Flutter build (Debian inside Termux). No server, no GitHub.
# $1 = the app's local address, $2 = app id (optional), $3 = app name (optional)
B="$1"; APPID="$2"; NAME="$3"
W="$HOME/wzh_flutter"
mkdir -p "$W"
post() { curl -s -m 60 -X POST --data-binary "$2" "$B/$1" >/dev/null 2>&1; }
step() { post step "$1"; }
sendlog() { [ -s "$1" ] && curl -s -m 120 -X POST --data-binary @"$1" "$B/log" >/dev/null 2>&1; }
finish_fail() { command -v termux-wake-unlock >/dev/null 2>&1 && termux-wake-unlock; post status "fail: $1"; exit 1; }
command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock

step "Checking the tools"
command -v proot-distro >/dev/null 2>&1 || finish_fail "The Flutter tools are not installed. Open Set up Flutter on this phone first."
proot-distro login debian -- test -f /root/.wzh_flutter_ok >/dev/null 2>&1 || finish_fail "The Flutter tools are not installed. Open Set up Flutter on this phone first."

step "Unpacking the project"
rm -rf "$W/p"; mkdir -p "$W/p"
curl -sf -m 300 -o "$W/project.zip" "$B/project.zip" || finish_fail "could not get the project from the app"
unzip -q -o "$W/project.zip" -d "$W/p" || finish_fail "could not unzip the project"
PY=$(find "$W/p" -maxdepth 3 -name pubspec.yaml -not -path '*/.*' | awk '{print length, $0}' | sort -n | head -1 | cut -d' ' -f2-)
[ -n "$PY" ] || finish_fail "No pubspec.yaml found in the project"
PD=$(dirname "$PY"); REL="${PD#$W/p}"; REL="p$REL"

step "Preparing your app"
curl -sf -m 60 -o "$W/wzh_flutter_inner.sh" "$B/wzh_flutter_inner.sh" || finish_fail "could not get the build steps from the app"
curl -sf -m 60 -o "$W/wzh_flutter_patch.py" "$B/wzh_flutter_patch.py" || finish_fail "could not get the project fixes from the app"
cp "$W/wzh_flutter_inner.sh" "$W/inner.sh"

proot-distro login debian --bind "$W:/work" -- env WZH_APPID="$APPID" WZH_NAME="$NAME" bash /work/inner.sh "$REL" >"$W/cmd.log" 2>&1 &
PID=$!
while kill -0 "$PID" 2>/dev/null; do step "Compiling your app"; sleep 20; done
wait "$PID"; RC=$?
tail -n 300 "$W/cmd.log" > "$W/cmd.tail"; sendlog "$W/cmd.tail"
if [ "$RC" -ne 0 ]; then
  case "$RC" in
    11) finish_fail "Could not open the project folder." ;;
    12) finish_fail "flutter pub get failed. Check your pubspec.yaml and the internet connection." ;;
    13) finish_fail "The Flutter build failed." ;;
    14) finish_fail "Could not add the Android part to this Flutter project." ;;
    *)  finish_fail "The build stopped (code $RC). Android may have closed Termux; open it once and try again." ;;
  esac
fi

APK="$PD/build/app/outputs/flutter-apk/app-release.apk"
[ -f "$APK" ] || APK=$(find "$PD/build/app/outputs" -name '*.apk' 2>/dev/null | head -1)
[ -f "$APK" ] || finish_fail "The Flutter build failed, no app was made. The error is below."
step "Packaging your app"
curl -sf -m 600 -X PUT -T "$APK" "$B/out.apk" >/dev/null 2>&1 || finish_fail "could not send the finished app back"
command -v termux-wake-unlock >/dev/null 2>&1 && termux-wake-unlock
post status ok
