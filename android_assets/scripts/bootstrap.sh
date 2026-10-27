#!/system/bin/sh
# Minimal first-run directory setup for the embedded Local AIDE PRoot runtime.
set -e
APP_PREFIX="${APP_PREFIX:?APP_PREFIX was not provided}"
APP_HOME="${APP_HOME:?APP_HOME was not provided}"
mkdir -p "$APP_PREFIX/etc/pr" "$APP_PREFIX/var/lib/pr/installed-rootfs" \
  "$APP_PREFIX/var/lib/pr/dlcache" "$APP_HOME" "$APP_PREFIX/tmp"
echo "Local AIDE embedded runtime initialized"
