# SSCP Android (bản 1.1)

Đọc bảng kết quả (bead plate: các hình tròn B / P / T) trên màn hình bàn Baccarat bằng phân tích màu điểm ảnh, rồi hiện trên cửa sổ nổi:
ván mới nhất, độ dài chuỗi, thống kê các ô đang hiện, 12 ván gần nhất.

Khác bản cũ: bỏ OCR chữ PLAYER/BANKER (đó là nhãn cố định của bàn nên luôn báo sai). Nay đọc đúng bảng kết quả, chỉ báo khi 2 khung liên tiếp giống nhau, không thấy bảng thì hiện "—", không đoán.

Kiểm thử: bộ đọc đọc đúng 20/20 ô trên ảnh chụp bàn SSCP thật (ở 3 độ phân giải).
Ô biểu tượng Rồng/Hổ được tính theo màu của ô (xanh = P, đỏ = B); đây là suy luận từ màu.

KHÔNG dự đoán ván kế tiếp: mỗi ván Baccarat độc lập và ngẫu nhiên; không có AI nào đoán đúng.

## Biên dịch
Android Studio -> mở thư mục -> Sync Gradle -> Build > Build APK(s) (hoặc `gradle assembleDebug`).
File APK ở app/build/outputs/apk/debug/app-debug.apk. Cần Android SDK 35.

## Lưu ý
Nếu app casino chặn chụp màn hình (FLAG_SECURE), khung hình sẽ đen và app sẽ báo không thấy bảng.
Bố cục bảng được định vị tự động ở góc dưới trái; nếu bàn khác bố cục, cần kiểm thử thêm.
