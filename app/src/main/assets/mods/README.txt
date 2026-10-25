Đặt file mod vào đây để auto-replace hoạt động 100%.

Cấu trúc:
  /sdcard/FFProxy/mods/
  ├── common/
  │   └── libil2cpp.so          ← dùng chung nếu không có file riêng
  ├── esp/
  │   └── libil2cpp.so
  ├── aimHead/
  │   └── libil2cpp.so
  ├── aimNeck/
  │   └── libil2cpp.so
  ├── magicBullet/
  │   └── libil2cpp.so
  └── aimDrag/
      └── libil2cpp.so

Khi bật tính năng:
1. App dò package Free Fire
2. Tìm libil2cpp.so gốc
3. Backup vào /sdcard/FFProxy/backup/<feature>/
4. Copy file mod đè lên
5. Force-stop game → mở lại để load lib mới

Không có file mod → fallback Frida script (cần offset live).
File .so phải đúng version game hiện tại.
