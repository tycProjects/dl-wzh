# Proxy Browser (Android)

Trinh duyet WebView: moi profile co proxy, User-Agent, UUID rieng, chan ro ri WebRTC.

## Build APK
**Cach 1 - Android Studio (PC):** Open thu muc nay -> Build > Build APK(s). APK nam o `app/build/outputs/apk/debug/`.
**Cach 2 - dong lenh:** `gradle assembleDebug` (JDK 17, Gradle 8.2+).
**Cach 3 - tren dien thoai:** Termux (`pkg install openjdk-17 gradle`, can Android SDK) hoac dich vu build online
(GitHub Actions: workflow `./gradlew assembleDebug`). Dung ban *debug* de APK tu duoc ky.

Yeu cau may chay: Android 9+ (API 28), Android System WebView / Chrome cap nhat moi.

## Chuc nang
- Proxy http / socks5, ho tro user:pass. Trang chi duoc tai SAU khi proxy ap dung xong; proxy loi = khong load (khong lo IP that).
- User-Agent + navigator.* (platform, languages, cores, RAM, touch) dong bo.
- UUID: du lieu WebView (cookie, localStorage...) tach rieng theo UUID; seed cho canvas noise.
- WebRTC: `off` go han; `relay` chi dung ung vien relay (khong host/srflx).

## Gioi han (do WebView cua Android)
- Khong the ep WebRTC UDP di qua proxy nhu Chrome desktop -> dung `off` hoac `relay`.
- Khong gia lap timezone, GPU/WebGL, font; khong doi duoc header Accept-Language.
- Kiem tra bang browserleaks.com/webrtc (mo san khi chay).
