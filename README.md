# CopyMaster (Android)

CopyMaster là ứng dụng Android mẫu để nhận văn bản từ clipboard, giữ nội dung trong bộ nhớ RAM của tiến trình và sao chép toàn bộ hoặc từng đoạn trở lại clipboard.

## Tính năng
- Dán nội dung clipboard vào vùng soạn thảo.
- Đếm ký tự.
- Sao chép toàn bộ nội dung.
- Sao chép lần lượt từng đoạn với kích thước tùy chỉnh.
- Xóa nội dung trong ứng dụng và yêu cầu xóa clipboard.
- Không có quyền Internet, không có quyền đọc/ghi tệp và tắt Android Auto Backup.

## Giới hạn Android cần biết
Ứng dụng Android thông thường không thể tự ý chèn văn bản vào ô nhập của một ứng dụng khác. Vì vậy, sau khi CopyMaster đặt nội dung vào clipboard, người dùng chuyển sang ứng dụng đích và nhấn **Dán**. Nếu ứng dụng đích không nhận được toàn bộ văn bản trong một lần, dùng **Sao chép đoạn tiếp theo** và dán từng đoạn.

Không thể bảo đảm ứng dụng đích ghép các đoạn thành một nội dung duy nhất tự động. Giới hạn nhập liệu do ứng dụng đích quyết định, không phải CopyMaster.

## Quyền riêng tư
- Ứng dụng không ghi nội dung vào tệp hoặc cơ sở dữ liệu.
- Nội dung đang mở được giữ trong RAM; hệ điều hành có thể kết thúc tiến trình bất kỳ lúc nào.
- Clipboard là vùng hệ thống Android, nằm ngoài quyền kiểm soát hoàn toàn của ứng dụng. Bàn phím hoặc hệ điều hành có thể có lịch sử clipboard riêng.
- Nút xóa yêu cầu Android xóa clipboard, nhưng hành vi có thể khác nhau theo phiên bản Android/nhà sản xuất.

## Build APK
1. Mở thư mục dự án bằng Android Studio.
2. Chờ Gradle Sync hoàn tất.
3. Chọn **Build > Build APK(s)**.
4. APK debug thường nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

Yêu cầu: Android Studio với Android SDK 35 và JDK tương thích với Android Gradle Plugin 8.7.3.
