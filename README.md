# Leon-Source web — Bản Việt hóa

Dự án Android Studio / Gradle cho ứng dụng xem và phân tích mã nguồn website. Giao diện đã được Việt hóa; các thuật ngữ kỹ thuật như HTML, CSS, JavaScript, SEO và ZIP được giữ nguyên khi cần thiết.

## Tính năng
- Tải và hiển thị HTML thô của trang HTTP/HTTPS công khai.
- Liệt kê URL tài nguyên CSS, JavaScript, hình ảnh, video và âm thanh được tìm thấy trong HTML tĩnh.
- Tìm kiếm trong mã nguồn, định dạng hoặc thu gọn khoảng trắng.
- Phân tích loại website, tiêu đề phản hồi, cấu trúc, công nghệ và thông tin SEO cơ bản.
- Tùy chỉnh màu nhấn, nền, phông chữ, cỡ chữ và tô sáng cú pháp.
- Xuất HTML cùng tối đa 30 tài nguyên liên kết thành tệp ZIP để chia sẻ.

## Biên dịch
1. Mở thư mục dự án bằng Android Studio.
2. Dùng JDK 17 và đồng bộ Gradle (Android Gradle Plugin 8.7.3, compile SDK 35).
3. Chọn Build > Build APK(s).

Lưu ý: đây là ZIP chứa mã nguồn dự án, không phải APK đã biên dịch. Gói gốc không kèm Gradle Wrapper JAR; Android Studio có thể dùng Gradle đã cài hoặc tạo wrapper bằng lệnh `gradle wrapper`.

## Giới hạn
- Chỉ tải HTML tĩnh, không chạy JavaScript của trang.
- Nội dung ZIP là bản chụp tĩnh; một số đường dẫn tương đối có thể cần chỉnh sửa thủ công.
- Chỉ kiểm tra hoặc tải nội dung website mà bạn có quyền truy cập.
