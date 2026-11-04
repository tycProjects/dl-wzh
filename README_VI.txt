BLOCK BLAST HELPER — ANDROID STUDIO PROJECT

Đây là mã nguồn dự án Android, KHÔNG phải APK cài sẵn.

Có sẵn:
- Bàn cờ 10x10, chạm ô để đánh dấu/bỏ đánh dấu.
- Chọn tối đa 3 khối từ danh sách hình dạng.
- Tìm cách sắp xếp thứ tự và vị trí đặt, tính điểm theo số ô xóa và độ thoáng bàn.
- Hiển thị vị trí gợi ý; áp dụng nước đi vào bàn cờ trong ứng dụng.
- Hoàn tác tối đa 25 thao tác; xóa bàn cờ và khối đã chọn.

Cài/chạy:
1. Giải nén ZIP.
2. Mở Android Studio > Open > chọn thư mục BlockBlastHelper_Android.
3. Chờ Gradle Sync; lần đầu cần mạng để tải Android Gradle Plugin và SDK.
4. Chọn điện thoại Android hoặc emulator rồi bấm Run.

Yêu cầu: Android Studio tương thích AGP 8.6.1, JDK 17, Android SDK 35. minSdk 23.

Giới hạn trung thực:
- Chưa có nhận diện ảnh tự động; người dùng nhập bàn cờ và chọn khối thủ công.
- Không tự điều khiển game.
- Danh sách hình khối là các mẫu phổ biến, có thể cần bổ sung nếu game của bạn có khối khác.
- Thuật toán là heuristic tìm kiếm, không đảm bảo tối ưu tuyệt đối.
