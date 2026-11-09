#!/system/bin/sh
MODDIR=${0%/*}
mkdir -p "$MODDIR"
# Конфигурация приложения сохраняется в $MODDIR/config.json.
# Здесь намеренно нет универсальных хуков SystemUI: ресурсы MIUI/HyperOS
# различаются между сборками и требуют отдельного протестированного backend.
