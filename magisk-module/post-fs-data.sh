#!/system/bin/sh
MODDIR=${0%/*}
[ -f "$MODDIR/config.json" ] || echo '{"schema":1}' > "$MODDIR/config.json"
