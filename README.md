# SensiScreen

Đổi kích thước màn hình (`wm size` / `wm density`) **riêng cho từng ứng dụng bạn chọn**.
Khi thoát ra màn hình chính hoặc sang ứng dụng khác, màn hình tự trả về như cũ.

## Build
1. Android Studio (Koala trở lên) → **Open** thư mục `SensiScreen` → chờ Gradle sync (cần internet).
2. Cắm điện thoại hoặc chọn **Build > Build APK(s)** rồi cài file APK.

## Dùng
1. Mở **Shizuku** (hoặc có root) → trong SensiScreen bấm **Cấp quyền Shizuku** / **Kiểm tra Root**.
2. Bấm **Bật dịch vụ Trợ năng** → bật SensiScreen.
3. Nhập kích thước (ví dụ 1000 x 1000) → **Chọn ứng dụng** → tick ứng dụng cần chỉnh
   (nhấn giữ một ứng dụng để đặt kích thước riêng cho nó).
4. Có thể bấm **Thử ngay** / **Khôi phục** để kiểm tra bằng tay.

## Nếu bị kẹt kích thước
Bấm **Khôi phục** trong app, hoặc chạy trong Brevent/ADB:
```
wm size reset
wm density reset
```
