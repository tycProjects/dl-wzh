# WZH 2 APK: terminal session. $1 = the app's local address, $2 = "1" when the app sends its project.
# Waits for commands from the app, runs each one here in Termux and sends the output back.
# The folder and exported variables are kept between commands; typing input (stdin) is not supported.
B="$1"; HASPROJ="$2"
W="$HOME/.wzh_term"; mkdir -p "$W"
command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock >/dev/null 2>&1
post() { printf '%s' "$2" | curl -s -m 20 -X POST --data-binary @- "$B/$1" >/dev/null 2>&1; }
CWD="$HOME"
if [ "$HASPROJ" = 1 ]; then
  P="$HOME/wzh_project"; rm -rf "$P.new"; mkdir -p "$P.new"
  if curl -sf -m 120 -o "$W/project.zip" "$B/project.zip" && unzip -q -o "$W/project.zip" -d "$P.new" >/dev/null 2>&1; then
    rm -rf "$P"; mv "$P.new" "$P"; CWD="$P"
  else
    rm -rf "$P.new"
  fi
  rm -f "$W/project.zip"
fi
rm -f "$W/env"
killtree() {
  # stop the parent first, so it cannot carry on with the next line, then everything under it
  local d pp kids=""
  for d in /proc/[0-9]*; do
    pp=$(sed 's/.*) //' "$d/stat" 2>/dev/null | cut -d' ' -f2)
    [ "$pp" = "$1" ] && kids="$kids ${d#/proc/}"
  done
  kill -TERM "$1" 2>/dev/null
  for d in $kids; do killtree "$d"; done
}
post hello "$CWD"
FAILS=0
while :; do
  CODE=$(curl -s -m 40 -o "$W/cmd" -w '%{http_code}' "$B/cmd" 2>/dev/null)
  case "$CODE" in
    204) FAILS=0; continue ;;
    410) break ;;
    200) FAILS=0 ;;
    *) FAILS=$((FAILS+1)); [ "$FAILS" -ge 8 ] && break; sleep 2; continue ;;
  esac
  ID=$(head -n1 "$W/cmd"); tail -n +2 "$W/cmd" > "$W/run.sh"
  O="$W/out.txt"; : > "$O"; rm -f "$W/rc" "$W/pwd"
  (
    cd "$CWD" 2>/dev/null || cd "$HOME"
    [ -f "$W/env" ] && . "$W/env" 2>/dev/null
    . "$W/run.sh"
    rc=$?
    pwd > "$W/pwd"; export -p > "$W/env" 2>/dev/null
    exit $rc
  ) </dev/null >"$O" 2>&1 &
  CP=$!
  OFF=0
  while kill -0 "$CP" 2>/dev/null; do
    sleep 0.5
    SZ=$(stat -c %s "$O" 2>/dev/null || echo 0)
    if [ "$SZ" -gt "$OFF" ]; then
      tail -c +$((OFF+1)) "$O" | head -c $((SZ-OFF)) | curl -s -m 20 -X POST --data-binary @- "$B/out" >/dev/null 2>&1
      OFF=$SZ
    fi
    [ "$(curl -s -m 4 "$B/cancel" 2>/dev/null)" = "1" ] && killtree "$CP"
  done
  wait "$CP" 2>/dev/null; RC=$?
  SZ=$(stat -c %s "$O" 2>/dev/null || echo 0)
  [ "$SZ" -gt "$OFF" ] && tail -c +$((OFF+1)) "$O" | curl -s -m 20 -X POST --data-binary @- "$B/out" >/dev/null 2>&1
  [ -s "$W/pwd" ] && CWD=$(cat "$W/pwd")
  post done "$ID
$RC
$CWD"
done
rm -f "$W/cmd" "$W/run.sh" "$W/out.txt" "$W/env"
