# Khóa màn hình Android — mã nguồn mẫu

Đây là **mã nguồn dự án Android Studio**, chưa phải APK đã biên dịch.

## Mã PIN mặc định
`2468`

Đổi `configuredPin` trong `MainActivity.kt` trước khi build. Đây là mẫu thử nghiệm; PIN được lưu trong mã nguồn nên không phù hợp để bảo vệ dữ liệu nhạy cảm.

## Biên dịch APK
1. Cài Android Studio và Android SDK.
2. Mở thư mục `KhoaManHinhAndroid` trong Android Studio.
3. Chờ Gradle đồng bộ.
4. Chọn **Build > Build APK(s)**.
5. APK debug thường nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

## Giới hạn Android
- Ứng dụng này khóa **bên trong chính ứng dụng**, không phải khóa hệ điều hành.
- Android có thể ngăn ứng dụng tự mở từ nền sau khi khởi động; có thể cần mở ứng dụng thủ công.
- APK thông thường không thể vô hiệu hóa nút nguồn, ngăn tắt máy, chặn Recovery hoặc thay thế màn hình khóa hệ thống.
- Để hạn chế Home/Recents và khóa thiết bị ở kiosk, cần cấu hình **Lock Task Mode** bằng một ứng dụng Device Owner trên thiết bị do bạn quản lý. Việc thiết lập Device Owner thường cần quy trình quản trị riêng và có thể yêu cầu thiết lập lại thiết bị.
- Luôn giữ mã PIN hệ thống Android và cách khôi phục thiết bị để tránh tự khóa mình khỏi điện thoại.
