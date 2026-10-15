# ldzedmods — đăng nhập native trước menu Android gốc

Dự án này được tạo từ mã nguồn Android gốc SensiScreen. Màn hình đăng nhập được viết bằng Kotlin native, không dùng WebView/HTML nên không phát sinh lỗi đóng gói `index.html`.

## Luồng sử dụng
- Khi mở ứng dụng, Android khởi chạy `LoginActivity`.
- Admin mở “Khu vực Admin · Tạo key”, nhập mật khẩu `LDZED-ADMIN-2026`, rồi tạo key có thời hạn 1–3650 ngày.
- Nhập key hợp lệ để mở `MainActivity`, là menu Android gốc với các chức năng hiện có.
- Key lưu cục bộ trên thiết bị. Đây không phải hệ thống xác thực an toàn cho phát hành công khai; mật khẩu Admin nằm trong mã nguồn và có thể bị trích xuất.

## Build
1. Giải nén ZIP và mở thư mục `SensiScreen` trong Android Studio.
2. Chờ Gradle Sync hoàn tất, sau đó chọn **Build > Build APK(s)**.
3. APK nằm trong `app/build/outputs/apk/`.

Yêu cầu JDK 17 và Android SDK theo cấu hình dự án (compileSdk 34). Các chức năng Android gốc vẫn dùng mã Kotlin hiện có và cần quyền Shizuku/Root/Trợ năng tương ứng. Không có cách nào cam kết “100%” nếu chưa build và kiểm thử trên thiết bị thật; hãy thử các chức năng và gửi log nếu Gradle báo lỗi.
