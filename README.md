# Lua Learn Android

Ứng dụng Android có 2 mục:
- **Học Lua**: bài học và ví dụ cơ bản.
- **Test code**: ô nhập code phía trên, kết quả phía dưới.

## Build APK bằng GitHub (không cần cài Android Studio)
1. Tạo repository GitHub mới.
2. Giải nén ZIP này và upload toàn bộ nội dung lên repository.
3. Mở tab **Actions**.
4. Chọn workflow **Build Lua Learn APK** → **Run workflow** (hoặc đợi build sau khi push).
5. Mở lần chạy mới nhất, tải artifact **LuaLearn-debug-apk**. Giải nén artifact để lấy `app-debug.apk`.

## Build trên máy có Gradle
Cài JDK 17 và Gradle 8.9, chạy:
`gradle assembleDebug`
APK nằm ở `app/build/outputs/apk/debug/app-debug.apk`.

## Lưu ý về trình chạy
Trình chạy tích hợp hiện là bộ mô phỏng cú pháp Lua cơ bản cho các ví dụ học tập (print, biến, phép tính, if/else và for đơn giản), **không phải Lua runtime đầy đủ**. Không nhập code không tin cậy nếu bạn thay đổi bộ chạy sau này.
