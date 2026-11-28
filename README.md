# CopyMaster Pro 2.0 — Android source

Bản nâng cấp gồm:
- Bộ soạn văn bản tạm trong RAM.
- IME (bàn phím Android) riêng: chèn toàn bộ nội dung bằng `InputConnection.commitText`, hoặc chèn từng đoạn.
- Accessibility Service tùy chọn: chỉ chạy khi người dùng chủ động kích hoạt và nhấn hành động Auto Paste trong bản tích hợp đầy đủ.
- Không khai báo quyền Internet, không có cơ sở dữ liệu, không ghi nội dung vào tệp, `allowBackup=false`.

## Cần lưu ý về Auto Paste
Mã nguồn này cung cấp dịch vụ trợ năng và phương thức `pasteNow()` để chèn văn bản vào node đang được focus. Để tránh hành vi tự ý thao tác trên ứng dụng khác, bản mẫu không tự chạy nền và không tự kích hoạt dịch vụ. Người dùng phải bật quyền Trợ năng trong Android Settings. Một UI/notification action do người dùng nhấn cần được nối vào `PasteAccessibilityService.activeService?.pasteNow()` trong bước tích hợp tiếp theo; Android không cho app thường tự bật quyền Trợ năng.

Một số app đích:
- Không cung cấp node nhập liệu qua Accessibility.
- Chặn `ACTION_SET_TEXT` hoặc `ACTION_PASTE`.
- Có giới hạn độ dài riêng, có thể làm mất phần văn bản.
- Dùng ô nhập bảo mật (mật khẩu) hoặc editor đặc biệt không hỗ trợ thao tác này.

Bàn phím riêng có thể chèn nội dung trực tiếp bằng `commitText`, nhưng ứng dụng đích vẫn có thể giới hạn số ký tự hoặc từ chối chuỗi rất lớn. Khi đó dùng nút "Chèn đoạn tiếp" trên bàn phím. Không có cách phổ quát để vượt giới hạn do app đích áp đặt.

## Quyền riêng tư và RAM
- Nội dung được giữ trong biến Kotlin trong tiến trình ứng dụng.
- Android có thể dừng tiến trình bất cứ lúc nào; dữ liệu RAM sẽ mất.
- Nội dung được đưa vào clipboard hệ thống khi người dùng chọn sao chép. Clipboard có thể do Android/bàn phím quản lý, nằm ngoài vùng lưu trữ riêng của app.
- Không thể đảm bảo mọi bàn phím bên thứ ba không ghi lịch sử clipboard. Dùng CopyMaster làm bàn phím hiện hành và tắt lịch sử clipboard của bàn phím khác nếu muốn giảm lưu ngoài ý muốn.

## Build
Mở dự án bằng Android Studio, dùng JDK 17, Android SDK 35, Gradle Sync, sau đó Build > Build APK(s). APK debug nằm ở `app/build/outputs/apk/debug/app-debug.apk`.

## Phạm vi bản mẫu
Đây là mã nguồn mẫu cần build và kiểm thử trên thiết bị thực. Không phải APK đã biên dịch hoặc chứng nhận tương thích với mọi ứng dụng Android.
