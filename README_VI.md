# DƯƠNG BÁ KHÍ MIC ULTRA GAIN (Android source starter)

## Có gì trong bản này
- Android app Kotlin cơ bản, giao diện tiếng Việt.
- Xin quyền microphone và ghi âm ngắn để nghe thử.
- Điều khiển Gain kỹ thuật số trong app và preset EQ giọng nói.
- Màn hình đo mức tín hiệu thô (RMS) để canh giọng.

## Giới hạn quan trọng
Đây là **mã nguồn khởi đầu**, chưa được build/test trên thiết bị trong phiên này. Gain/EQ chỉ áp dụng cho đường âm thanh mà app tự xử lý; Android không root không cho app này tự thay microphone hệ thống của TikTok, Messenger, Discord hay cuộc gọi điện thoại. Quay màn hình/TikTok LIVE sẽ tiếp tục dùng đường mic mà ứng dụng đó chọn. Không thể cam kết tăng âm lượng ở mọi ứng dụng.

## Mở và build
1. Cài Android Studio trên máy tính.
2. Chọn **Open** và mở thư mục `Duong_Ba_Khi_Mic_Ultra_Gain`.
3. Chờ Gradle sync. Nếu Android Studio yêu cầu cập nhật Gradle/Android Gradle Plugin, chọn bản tương thích được đề xuất.
4. Chạy trên điện thoại Android thật, chấp nhận quyền Microphone.
5. Nhấn **Bắt đầu nghe thử**, nói vào mic, rồi nhấn **Dừng**.

## Muốn ra APK
Trong Android Studio: **Build > Build APK(s)**. APK debug thường nằm trong `app/build/outputs/apk/debug/`.

## Ghi chú âm thanh
Thanh Gain hiển thị 1–1000 nhưng được ánh xạ thành hệ số khuếch đại hợp lý và có giới hạn chống clipping; đặt 1000 không có nghĩa phần cứng tăng âm lượng 1000 lần. EQ ở bản starter là preset/điều chỉnh đơn giản, chưa phải bộ xử lý studio đầy đủ.
