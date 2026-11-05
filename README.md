# 影译 AI — Android WebView project

## Build APK
1. Giải nén `ShadowYiAI-Android-Project.zip`.
2. Mở thư mục `ShadowYiAI` bằng Android Studio hoặc AndroidIDE có hỗ trợ Gradle.
3. Chờ Gradle Sync hoàn tất.
4. Chọn **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK debug nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

## Chức năng
- Chọn file SRT qua trình chọn tệp Android.
- Gọi Gemini 2.5 Flash bằng kết nối mạng native Android (không phụ thuộc CORS của WebView).
- Kiểm tra API trước khi bật nút dịch.
- Chia phụ đề thành từng batch, giữ timestamp và số thứ tự.
- Chỉnh sửa kết quả và tải SRT UTF-8.

## Lưu ý
- Cần Android Studio/AndroidIDE và Android SDK; đây là mã nguồn dự án để build APK, không phải APK đã biên dịch.
- API Key được nhập khi chạy và giữ trong bộ nhớ của ứng dụng; không được ghi vào mã nguồn. Không chia sẻ APK có nhúng API Key cá nhân.
- Dịch vụ Gemini có thể tính phí hoặc áp dụng hạn mức theo tài khoản/API Key.
