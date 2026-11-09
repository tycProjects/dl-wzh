# Game Space — kiểm tra trước khi phát hành

Trạng thái: CHƯA kiểm thử trên máy thật. Làm theo thứ tự dưới đây; chỉ phát hành khi mọi mục đều đạt.

1. Build: Android Studio → Build > Make Project. Sửa lỗi biên dịch nếu có (gửi dòng lỗi để được sửa).
2. Cài lên máy, cấp đủ 5 quyền ở màn hình đầu (Overlay, Ghi cài đặt, Tắt tiết kiệm pin, Thông báo, Shizuku).
3. Game Center → "Kiểm tra độ sẵn sàng": ghi lại tính năng nào báo lỗi trên máy.
4. Thử từng công tắc: BẬT → kiểm tra hiệu ứng thật → TẮT → kiểm tra đã trả về mặc định.
   - Điểm chạm, Không làm phiền: chỉ bật khi người dùng bật; GAME ON không tự bật.
5. Vào Free Fire: vuốt mép trái mở menu; bật Monitoring FPS và so FPS với công cụ đo khác.
6. Thử trên ít nhất 3 máy (Samsung / Xiaomi / Pixel hoặc tương đương), Android 12–15.
7. Ký bản phát hành: Build > Generate Signed Bundle/APK (tạo keystore, cất kỹ), bật minify nếu muốn.
8. Ghi rõ trên trang phát hành: cần Shizuku; một số tính năng phụ thuộc ROM; không can thiệp vào game.
