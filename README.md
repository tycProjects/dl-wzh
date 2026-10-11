# Leon-Source web 3.0 (Android, Java, không dùng thư viện ngoài)

Ứng dụng soi mã nguồn website, xem trước web và duyệt web trong tệp ZIP — giao diện hoàn toàn tiếng Việt.

## Chức năng
- Tải HTML từ URL (theo chuyển hướng http→https, đúng bảng mã), 6 tab: HTML, CSS, JS, MEDIA, LIÊN KẾT, CHỮ.
- **Mở ZIP** từ máy hoặc dán link `.zip`: giải nén an toàn (chống zip-slip/zip-bomb), duyệt danh sách tệp, lọc tên, tìm trong nội dung mọi tệp, xem mã từng tệp.
- **Xem trước web ngay trong app**: trang trực tiếp, bản HTML đã tải, hoặc web nằm trong ZIP (chạy offline qua WebView, tự tìm index.html). Có bật/tắt JavaScript, giao diện máy tính, chặn mạng ngoài.
- **Thông tin (i)**: thuần HTML hay SPA/CMS/framework, header bảo mật, cấu trúc, công nghệ, SEO, sao chép báo cáo.
- **Cài đặt (⚙)**: màu chủ đạo (8 màu + tuỳ chỉnh), 4 nền, phông giao diện, phông mã, cỡ chữ, tô màu cú pháp.
- Tài nguyên (mở CSS/JS, xem ảnh, đi theo liên kết), lịch sử URL, dán URL, mở trình duyệt ngoài, tìm kiếm kèm nhảy kết quả, định dạng/thu gọn HTML, xuất ZIP.

## Build nhanh
Không có dependency nào, tắt lint khi build release, bật cache + parallel + configuration cache.
Để hết cảnh báo "Gradle wrapper missing": `gradle wrapper --gradle-version 8.9` rồi commit.
