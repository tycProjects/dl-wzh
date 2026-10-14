# SensiScreen

Ứng dụng Android đổi kích thước màn hình (`wm size` / `wm density`) riêng cho ứng dụng đã chọn, với đăng nhập tài khoản qua backend.

## Thành phần
- Android: Kotlin + XML, giao diện SensiScreen hiện có được giữ nguyên; màn hình đăng nhập là điểm vào ứng dụng.
- Backend: thư mục `backend/`, Node.js + Express + SQLite, mật khẩu băm bằng bcrypt, token JWT hết hạn sau 7 ngày, giới hạn tốc độ đăng nhập.
- API: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, `GET /health`.

## Chạy backend
1. Cài Node.js 20 trở lên trong máy chủ.
2. Vào `backend/`, chạy `npm install`.
3. Tạo `JWT_SECRET` ngẫu nhiên dài tối thiểu 32 ký tự (ví dụ dùng `openssl rand -base64 48`). Không dùng giá trị mẫu trong `.env.example`.
4. Đặt biến môi trường `JWT_SECRET` và `PORT` nếu cần, sau đó chạy `npm start`.
5. Đưa API sau HTTPS bằng reverse proxy hoặc nền tảng hosting có TLS. Không đưa backend công khai qua HTTP; ứng dụng chỉ chấp nhận URL `https://`.

Ví dụ Linux/macOS:
```sh
cd backend
npm install
export JWT_SECRET="$(openssl rand -base64 48)"
export PORT=8080
npm start
```

SQLite sẽ tạo file `sensiscreen.sqlite` trong thư mục làm việc. Hãy sao lưu file này và bảo vệ quyền truy cập máy chủ.

## Dùng trong ứng dụng
1. Mở dự án `SensiScreen` bằng Android Studio và chờ Gradle Sync.
2. Cài đặt bản APK lên điện thoại.
3. Ở màn hình đầu tiên, nhập URL HTTPS của API (ví dụ `https://auth.example.com`), tên tài khoản và mật khẩu.
4. Chọn **Tạo tài khoản** để đăng ký tài khoản đầu tiên; sau đó đăng nhập.
5. Các chức năng Shizuku/Root, chọn ứng dụng, tự động áp dụng, thử và khôi phục vẫn ở màn hình chính. Nút **Đăng xuất** nằm phía trên giao diện.

Tên tài khoản: 3–32 ký tự, chữ/số/dấu chấm/gạch dưới/gạch ngang. Mật khẩu: 8–128 ký tự.

## Bảo mật và lưu ý
- Backend mẫu cần được triển khai trên máy chủ của bạn trước khi đăng nhập thực tế; dự án không chứa máy chủ đã deploy hay tài khoản mặc định.
- Dùng HTTPS với chứng chỉ TLS hợp lệ. Không lưu mật khẩu trong ứng dụng; ứng dụng chỉ lưu JWT trong SharedPreferences.
- Đây là nền tảng xác thực tối thiểu. Trước khi phát hành rộng rãi, nên bổ sung quy trình đặt lại mật khẩu, xác minh email, thu hồi/đổi token, giám sát và chính sách quyền riêng tư.
- Nếu màn hình bị kẹt kích thước, bấm **Khôi phục** hoặc chạy `wm size reset` và `wm density reset` qua Brevent/ADB.
