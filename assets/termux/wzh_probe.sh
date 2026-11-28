# WZH 2 APK: tells the app which build tools Termux has. $1 = the app's local address
B="$1"

# --- WZH: repair curl if it is broken. A fresh Termux often has a curl that cannot start; pkg itself
# needs curl, so the repair goes through apt directly. ---
wzh_curl_ok() { curl --version >/dev/null 2>&1; }
if ! wzh_curl_ok; then
  echo "[wzh] Repairing Termux (curl does not start). This can take a few minutes..."
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -y >/dev/null 2>&1
  apt-get -y -o Dpkg::Options::=--force-confnew full-upgrade >/dev/null 2>&1
  wzh_curl_ok || apt-get -y install --reinstall curl libcurl openssl >/dev/null 2>&1
fi
# --- end repair ---
r=""
for t in java javac jar kotlinc aapt2 d8 dx zip unzip curl python3; do
  if command -v "$t" >/dev/null 2>&1; then r="$r $t=1"; else r="$r $t=0"; fi
done
# Send the answer. Uses curl; if curl is missing or broken (a fresh Termux often needs a package upgrade
# first), falls back to plain bash so the app still hears from Termux.
post() {
  curl -s -m 20 -X POST --data-binary "$2" "$B/$1" >/dev/null 2>&1 && return 0
  (
    hp="${B#http://}"; host="${hp%%/*}"; rest="${hp#*/}"; port="${host##*:}"; host="${host%%:*}"
    exec 3<>"/dev/tcp/$host/$port" || exit 1
    printf 'POST /%s/%s HTTP/1.0\r\nHost: %s\r\nContent-Length: %s\r\nConnection: close\r\n\r\n%s' "$rest" "$1" "$host" "${#2}" "$2" >&3
    cat <&3 >/dev/null 2>&1
  ) 2>/dev/null
}
post probe "$r"
