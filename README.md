# Proxy Browser v2 (Android)

Trinh duyet da the (multi-tab) kieu Chrome, moi ho so co proxy / User-Agent / UUID / WebRTC rieng.

## Build APK
- Android Studio: Open thu muc -> Build > Build APK(s) (ban debug, tu ky). JDK 17, Gradle 8.2+.
- Dien thoai: day len GitHub, build bang GitHub Actions `gradle assembleDebug`.
Yeu cau: Android 9+, Android System WebView / Chrome moi.

## Tinh nang
- Nhieu the cung luc, popup mo the moi, keo xuong de tai lai, thanh tien trinh, tim Google ngay tren thanh dia chi.
- Menu: the moi, trang chu, trang web may tinh, chia se, sao chep, xoa cookie, thoat.
- Proxy http/socks5 (co user:pass). Trang chi tai sau khi proxy san sang. Tai file bi chan khi dung proxy (tranh lo IP).
- UA mac dinh "Tu dong" = UA that cua WebView (bo dau hieu wv) -> Google/dang nhap chay muot nhat.
- UUID: du lieu web rieng + seed canvas. WebRTC: tat han hoac relay.
- Mo lai ho so se khoi phuc cac the dang mo.

## Gioi han WebView
Khong ep WebRTC UDP qua proxy, khong gia lap timezone/WebGL/font.
