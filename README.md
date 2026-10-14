# SensiScreen

Ứng dụng Android đổi kích thước màn hình (`wm size` / `wm density`) riêng cho ứng dụng đã chọn, có đăng nhập tài khoản và bảng quản trị Admin trong ứng dụng.

## Thành phần
- Android: Kotlin + XML; giữ giao diện và chức năng SensiScreen hiện có.
- Backend: Node.js 20+, Express, SQLite, bcrypt và JWT.
- Tài khoản mới đăng ký luôn có role `user`; người dùng không thể tự cấp quyền Admin.
- Admin trong Android có thể xem tài khoản và khóa/mở khóa tài khoản. Mọi quyền quản trị được xác minh ở backend, không dựa vào việc ẩn/hiện nút trong app.

## Chạy backend
1. Cài Node.js 20 trở lên trên máy chủ.
2. Vào `backend/`, chạy `npm install`.
3. Đặt `JWT_SECRET` ngẫu nhiên tối thiểu 32 ký tự, ví dụ `openssl rand -base64 48`.
4. Chỉ khi khởi tạo Admin đầu tiên, đặt cả `ADMIN_BOOTSTRAP_USERNAME` và `ADMIN_BOOTSTRAP_PASSWORD` trong môi trường máy chủ. Mật khẩu Admin phải dài 12–128 ký tự. Không đặt hai biến này trong APK hoặc kho mã nguồn.
5. Đưa API ra Internet qua HTTPS có chứng chỉ hợp lệ. Không gửi thông tin đăng nhập qua HTTP.

Ví dụ Linux/macOS:
```sh
cd backend
npm install
export JWT_SECRET="$(openssl rand -base64 48)"
export PORT=8080
# Chỉ cho lần khởi tạo Admin đầu tiên; dùng giá trị riêng của bạn.
export ADMIN_BOOTSTRAP_USERNAME="myadmin"
export ADMIN_BOOTSTRAP_PASSWORD="CHANGE_TO_A_UNIQUE_PASSWORD_12+"
npm start
# Sau khi Admin đã được tạo, xóa/unset hai biến ADMIN_BOOTSTRAP_* và khởi động lại backend.
```

SQLite tự tạo file `sensiscreen.sqlite` trong thư mục chạy backend. Sao lưu file này và bảo vệ quyền truy cập máy chủ.

## Tạo Admin đầu tiên (bắt buộc làm từ backend)
Không có tài khoản/mật khẩu Admin mặc định. Khi khởi động backend lần đầu, đặt hai biến môi trường `ADMIN_BOOTSTRAP_USERNAME` và `ADMIN_BOOTSTRAP_PASSWORD`. Backend chỉ tạo Admin nếu cơ sở dữ liệu chưa có Admin nào; nếu username đã thuộc tài khoản user thì backend dừng khởi động và không tự nâng quyền tài khoản đó. Mật khẩu bootstrap phải dài ít nhất 12 ký tự.

Ví dụ, trước khi chạy `npm start`:
```sh
export ADMIN_BOOTSTRAP_USERNAME="myadmin"
export ADMIN_BOOTSTRAP_PASSWORD="USE_A_UNIQUE_PASSWORD_OF_12_OR_MORE_CHARS"
npm start
```
Sau khi log báo đã tạo Admin thành công, **xóa/unset hai biến `ADMIN_BOOTSTRAP_*` khỏi cấu hình máy chủ và khởi động lại backend**. Không đặt mật khẩu Admin trong APK, Git hoặc chia sẻ công khai. Nếu đã có Admin, các biến này không thay đổi mật khẩu hoặc quyền của tài khoản hiện có.

## Dùng ứng dụng Android
1. Mở thư mục `SensiScreen` bằng Android Studio và chờ Gradle Sync.
2. Build/cài APK.
3. Ở màn hình đăng nhập, nhập URL API HTTPS, username và mật khẩu. Người dùng thường có thể tạo tài khoản từ nút đăng ký.
4. Đăng nhập tài khoản Admin đã tạo bằng bước bootstrap. Trên màn hình chính sẽ có nút **Quản trị Admin · Quản lý tài khoản**.
5. Trong bảng Admin, tải danh sách tài khoản và khóa/mở khóa. Tài khoản bị khóa sẽ bị từ chối đăng nhập và các token hiện hành cũng bị backend từ chối ở những API yêu cầu xác thực.
6. Các chức năng Shizuku/Root, chọn ứng dụng, tự động áp dụng, thử và khôi phục vẫn nằm trong màn hình chính. Nút đăng xuất vẫn có sẵn.

## API
- `POST /api/auth/register` — tạo tài khoản role `user`.
- `POST /api/auth/login` — xác thực và trả JWT cùng role.
- `GET /api/auth/me` — thông tin tài khoản đang đăng nhập.
- `GET /api/admin/users` — danh sách tài khoản (Admin).
- `PATCH /api/admin/users/:id/status` với `{"locked":true|false}` — khóa/mở khóa (Admin).
- `GET /health` — kiểm tra tình trạng backend.

## Quy tắc bảo mật
- Backend xác minh chữ ký JWT và đọc role/trạng thái tài khoản mới nhất từ SQLite trên mỗi API được bảo vệ.
- Người dùng không thể tự nâng role; không thể tự khóa mình; không thể khóa tài khoản Admin qua màn hình quản lý user.
- Mật khẩu băm bằng bcrypt; API giới hạn tốc độ; dùng HTTPS.
- Role được lưu trong ứng dụng chỉ để hiển thị nút quản trị. Backend mới là nơi quyết định quyền truy cập.
- Trước khi phát hành rộng rãi, nên bổ sung quản lý phiên/thu hồi token, khôi phục mật khẩu, giám sát, sao lưu và chính sách quyền riêng tư.

Nếu màn hình bị kẹt kích thước, bấm **Khôi phục** hoặc chạy `wm size reset` và `wm density reset` qua Brevent/ADB.
