# FF Proxy v3 — Auto Scan + Auto Replace
**by Trần phi thiên**

## Cơ chế mới
Bật tính năng trên menu → **ngay lập tức**:
1. Dò package Free Fire (`com.dts.freefireth` / max / …)
2. Tìm `libil2cpp.so` trong data
3. Backup file gốc → `/sdcard/FFProxy/backup/<feature>/`
4. Copy file mod từ `/sdcard/FFProxy/mods/<feature>/` đè lên
5. `am force-stop` game → mở lại là có hiệu lực

## File chính
`assets/index.html`

## Cấu trúc mods (bắt buộc để replace 100%)
```
/sdcard/FFProxy/mods/
├── common/libil2cpp.so
├── esp/libil2cpp.so
├── aimHead/libil2cpp.so
├── magicBullet/libil2cpp.so
└── ...
```

Không có file mod → fallback Frida script (cần offset).

## Build
Android Studio → Open → Sync → Build APK

## Cảnh báo
- File .so phải đúng version game
- Sai version = crash hoặc ACE ban
- Antiban chỉ mitigation
