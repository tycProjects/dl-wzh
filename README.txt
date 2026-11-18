LONG SUPRA // MOD LAB 4.0 SUPER

BUILD APK TRÊN ĐIỆN THOẠI (không cần máy tính):
1. Tạo tài khoản github.com, tạo repository mới.
2. Upload TOÀN BỘ file/thư mục trong zip này lên repo (nhớ cả thư mục .github).
3. Vào tab Actions -> "Build APK" -> chờ chạy xong (~3-5 phút).
4. Mở lần chạy đó -> mục Artifacts -> tải LSUPRA-FPS-Meter-APK -> giải nén -> cài app-release.apk.

VÀO APP: không còn tài khoản/mật khẩu. Màn hình mở đầu là cổng kích hoạt: GIỮ NÚT NGUỒN TRÒN khoảng 1 giây để vào app.

SHIZUKU (tab CÀI ĐẶT -> Trung tâm Shizuku):
- Cài Shizuku -> Tùy chọn nhà phát triển -> bật "Gỡ lỗi không dây" -> trong Shizuku bấm "Bắt đầu qua Gỡ lỗi không dây" -> mở app, bấm CẤP.
- Máy đã root: Shizuku chạy mức root. Cũng có thể bấm KIỂM TRA ở mục Root.
- Có Shizuku/root: bấm CẤP NGAY để app tự cấp quyền nổi, chạy nền, thông báo, bỏ tối ưu pin; đo FPS rendering của game và tự nhận game đang mở.

QUAY MÀN HÌNH (tab QUAY, hoặc từ bong bóng nổi / ô "Quay màn hình" ở trang chủ):
- Tới 1080p/1440p, 30-120 fps, HEVC hoặc H.264, bitrate chỉnh được, mã hóa bằng chip phần cứng.
- Khi hệ thống hỏi quyền, chọn "Toàn màn hình" (không chọn "một ứng dụng").
- 120 fps chỉ có khi màn hình đang ở 120Hz và chip mã hóa hỗ trợ; nếu không app tự hạ xuống mức cao nhất máy làm được và báo rõ.
- Video lưu tại Movies/LongSupra. Dừng bằng bong bóng hoặc nút DỪNG ở thông báo.


BẢN MỚI:
- Bong bóng nổi làm lại: logo tròn có vòng sáng quay, huy hiệu số ô đang bật / chấm đỏ khi quay; menu dạng thẻ 2x2 (FPS THẬT, FPS GIẢ, CHỮ NỔI, QUAY) + MỞ APP / ẨN BÓNG.
- Chữ nổi: bảng chỉnh ngay trên màn hình đầy đủ (chữ, mã {time} {date} {bat} {temp} {ram} {fps}, cỡ, màu + thanh sắc, hiệu ứng Neon / Cầu vồng / Nhịp / Nháy / Chuyển màu, phát sáng, font, đậm, nghiêng, nền, căn lề, khóa vị trí). Hiệu ứng cũng có cho FPS THẬT / GIẢ.
- Logo app, bong bóng, ảnh đại diện dùng ảnh mắt đỏ. Mục "Về tôi" đã bỏ chỉnh sửa.
- Tab HZ mới: chọn Hz máy hỗ trợ hoặc nhập Hz tùy ý, TỰ ĐỘNG để trả lại, KHÓA CỨNG Hz (ContentObserver + DisplayListener + đồng hồ canh 150ms-2s + ép lại khi mở màn hình, tự bật lại sau khi khởi động máy).
  Cần quyền "Ghi cài đặt hệ thống" hoặc Shizuku/root. Bấm CẤP NGAY trong CÀI ĐẶT để tự cấp.

4.0 SUPER:
- Bỏ kiểm tra toàn vẹn dựa trên chữ (không còn lỗi "đã bị chỉnh sửa" khi đổi nội dung giao diện).
- Chống ghi đè cấu hình giữa giao diện web và bảng chỉnh chữ nổi trên màn hình.
- Khóa Hz tự nghỉ canh khi tắt màn hình để tiết kiệm pin.
- Hiệu ứng chữ trong giao diện chỉ cập nhật phần cần thiết, mượt hơn.
- Sao lưu cấu hình: XUẤT / NHẬP mã trong tab CÀI ĐẶT.
- versionName 4.0, versionCode 4.


DISPLAY LAB (BUFF MÀN HÌNH):
- Tab BUFF thêm thư viện profile thiết bị, tra cứu kích thước, độ phân giải, Hz, tỷ lệ và PPI.
- Có ô tìm kiếm, chỉnh sửa/lưu profile cục bộ và khôi phục mặc định.
- Chỉ gửi lệnh Hz khi mức đó nằm trong danh sách tần số màn hình hiện tại hỗ trợ.
- Độ phân giải/DPI/tỷ lệ profile là dữ liệu tham chiếu; không thể biến phần cứng thành màn hình của mẫu khác.
- Các model chưa có thông số chính thức được đánh dấu CHƯA XÁC MINH, không tự điền thông số phỏng đoán.
