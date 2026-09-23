# ZnexMos 1.0 — UI + permissions update

## Đã thay đổi
- Thiết kế lại giao diện native theo phong cách dark glassmorphism, thẻ bo góc, accent xanh lục và các nhóm chức năng rõ ràng.
- Giữ package/applicationId: `com.znexmods.app`.
- Thêm tích hợp Shizuku API `13.1.5`.
- Thêm `ShizukuProvider` vào manifest để nhận binder Shizuku.
- Khi mở ZnexMos, app kiểm tra và tự gọi luồng xin quyền Shizuku nếu Shizuku đang chạy.
- Nếu Shizuku chưa chạy/cài, app hiển thị hướng dẫn và nút mở Shizuku/trang tải.
- Thêm quyền truy cập bộ nhớ:
  - Android 11+: dẫn tới trang hệ thống `Allow access to manage all files`.
  - Android 6–10: dùng runtime `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` khi phù hợp.
- Hiển thị trạng thái RAM, Shizuku và bộ nhớ ngay trên màn hình chính.

## Lưu ý
Source này chưa thực hiện thay đổi dữ liệu của Free Fire bằng Shizuku; quyền Shizuku chỉ được tích hợp và xin cấp theo yêu cầu. Các thao tác tối ưu hiện có vẫn giữ ở mức an toàn.
