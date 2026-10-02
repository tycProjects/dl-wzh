Map Phím v1.4 - dự án Android (Kotlin) + máy chủ key
Build: mở bằng Android Studio (JDK 17) -> Sync -> Build > Build APK(s). Android 8.0+. Giao diện ngang.

Nối máy chủ key: sửa  val apiBase = ""  trong app/build.gradle.kts, ví dụ "https://your-domain.com/api"
  (để trống = chế độ thử, không cần key; thử trên máy ảo: "http://10.0.2.2:3000/api")

Máy chủ: cd server && python3 server.py
  python3 server.py genkey 24 10      tạo 10 key dùng 24 giờ
  python3 server.py genkey 336 5      tạo 5 key dùng 14 ngày
  python3 server.py keys | revoke <key>

API (JSON, POST):
  /license/activate  {key, device_id} -> 200 {token, expires_at} | 4xx {error}
  /auth/verify       Authorization: Bearer <token> -> 200 | 401 (app tự đăng xuất)
1 key = 1 thiết bị: lần đầu kích hoạt gắn với device_id, hạn tính từ lúc kích hoạt.
