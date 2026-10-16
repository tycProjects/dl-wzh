# SSCP Android (bản 1.3)

Chỉ đọc 4 vùng trên bàn Baccarat: Big Road (bảng to nhất) và 3 cầu phụ bên dưới (Big Eye, Small Road, Cockroach).
Bỏ qua hoàn toàn: video người chia bài, nút cược, bảng bên trái.

## Cách đọc
1. Tự tìm khung 4 bảng bằng các đường kẻ đen, tự căn lưới ô (chạy ở nhiều độ phân giải).
2. Đọc màu viền từng vòng tròn: đỏ = Nhà Cái (B), xanh = Tay con (P), gạch/số xanh lá = Hòa. Ô Rồng/Hổ đọc theo viền.
3. Ghép ô thành chuỗi ván đúng luật Big Road (kể cả "đuôi rồng"), đọc 3 cầu phụ theo cùng quy tắc.
4. Chỉ cập nhật khi 3 khung liên tiếp (~1,2 giây) giống hệt nhau và không có ô nào "không chắc".
   Vì đọc từ bảng cầu của bàn, kết quả chỉ đổi SAU KHI bàn đã ghi ván đó (xong lật bài), không nhảy lúc đang chia/lật.

## "Bộ não" (RoadBrain)
- Giữ lịch sử ván của ghế, tự ghép khi Big Road cuộn sang trái, tự nhận ghế mới.
- Gọi tên cầu đang chạy: bệt, 1-1 ping-pong, 2-2, 3-3, a-b lặp.
- Thống kê trong chính ghế này (ví dụ chuỗi 3 trước đây đã tiếp tục k/m lần).
- Đây là MÔ TẢ quá khứ. KHÔNG dự đoán ván sau: mỗi ván Baccarat độc lập, B 45,9% · P 44,6% · Hòa 9,5% bất kể cầu.

## Đã kiểm thử
Trên ảnh chụp bàn SSCP thật (3 độ phân giải): đọc đúng 32/32 ván (B16 P16, khớp số trên màn hình), 4 ô Hòa,
và cả 3 cầu phụ đúng từng ô. Ảnh có nét vẽ đè lên bảng sẽ bị báo "đọc chưa chắc" thay vì đoán.
Chưa chạy trên điện thoại thật; bàn có bố cục khác chưa kiểm thử.

## Biên dịch
Android Studio -> mở thư mục -> Sync Gradle -> Build > Build APK(s). Cần Android SDK 35.
APK: app/build/outputs/apk/debug/app-debug.apk

## Lưu ý
- Kéo cửa sổ nổi lên phía trên (trên video), đừng đè lên 4 bảng.
- Nếu app casino chặn chụp màn hình (khung hình đen), app báo "Chưa thấy Big Road".
- Số lần Hòa trên cùng một ô (số 2, 3...) chưa đọc được; chỉ biết ô đó có Hòa.
