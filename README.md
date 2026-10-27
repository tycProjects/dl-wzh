# YEU EM MOI VU TRU

> Auto Typer cho Android — gõ tự động tin nhắn yêu thương vào Zalo, Messenger, hoặc bất kỳ app chat nào.

App tự động gõ và gửi tin nhắn từ file `.txt` của bạn. Dùng cho mục đích giải trí — gửi những lời yêu thương, spam nhẹ nhàng, hoặc tạo kịch bản chat vui vẻ với người yêu / bạn bè.

---

## ✨ Tính năng chính (v3.4)

### 🎨 Giao diện hiện đại
- **Aurora Glass** design — glassmorphism tối với 3 quầng sáng violet / pink / cyan
- **Light/Dark/System** theme — chuyển đổi bằng nút ở góc phải trên cùng
- **Top brand bar** với logo + theme toggle
- **Bottom nav Material 3** với active pill indicator + bounce animation

### 🏠 Tab Home
- **Card admin** với avatar xoay ring 5 màu (violet → pink → cyan → emerald → amber) + glow pulse
- **Trạng thái Trợ năng** + nút bật nhanh
- **Volume hint** — phím tắt khi đang chạy
- **START / STOP** — bấm Start rồi chuyển sang app chat, mở bàn phím là app tự gõ
- **Loop mode mặc định** — chạy xong tự loop từ đầu, không cần bấm gì thêm

### 📁 Tab Files
- **Recent files** — 10 file gần đây, bấm 1 phát mở lại, có nút ✏ chỉnh sửa riêng
- **Real-time editor** full-screen dialog — search + highlight + lưu trực tiếp
- **Animated stats** — counter đếm từ 0 lên giá trị thật khi load file
- **Empty state** — illustration + text khi chưa có file
- **Long-press context menu** — Đổi tên · Chia sẻ · Xoá
- **SAF persistence** — ghi lại quyền truy cập file, không cần cấp lại

### ⚙️ Tab Config
- **Số dòng mỗi lần gửi** (1–5) — gộp N dòng thành 1 tin
- **Cách gõ** — Gõ từng chữ (mượt) hoặc Dán từng dòng (nhanh)
- **Delay giữa các tin** — ms (mặc định 1000)
- **Réo tên cuối dòng** — tự thêm "yêu anh" / "baby" / ...
- **Auto-save** — mọi thay đổi lưu ngay vào prefs

### 🔒 Hoàn thiện trải nghiệm
- **Splash screen** 1.4s với avatar animation khi mở app
- **Tab transitions** slide + fade + scale khi đổi tab
- **Haptic feedback** khi bấm nút
- **Toast chỉ hiện trong app** — không che màn hình Zalo/Messenger khi đang chạy
- **Config persistence** — mở app lại giữ nguyên cài đặt

---

## 🚀 Cài đặt

### Yêu cầu
- Android 6.0 (API 23) trở lên
- Quyền Trợ năng (Accessibility) cho app

### Build từ source
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Hoặc mở trong Android Studio → Run.

### Cài đặt APK
1. Copy file `YeuEmMoiVuTru-v3.4.apk` sang điện thoại
2. Mở file → cho phép "Install from unknown sources" nếu được hỏi
3. Mở app → vào tab Home → **Bật quyền Trợ năng**
4. Tìm "YEU EM MOI VU TRU" trong danh sách → bật
5. Quay lại app → tab Files → chọn file `.txt` (hoặc tạo mới)
6. Tab Home → **START** → chuyển sang app chat → mở bàn phím
7. App tự gõ + tự loop → bấm **STOP** để tắt, hoặc Volume ▲/▼ để điều khiển

---

## 🎯 Cách dùng

### Phím tắt Volume khi đang chạy
- **Volume ▲** — Tiếp tục / chạy lại từ đầu nếu đã xong
- **Volume ▼** — Tạm dừng
- **STOP** (trong app) — Tắt hoàn toàn

### Loop mode
- **Mặc định bật** — khi gõ xong tất cả tin, app tự chạy lại từ đầu
- Muốn tắt hẳn: bấm **STOP** trong app
- Toast "🔄 Đã hết — tự động chạy lại từ đầu" báo mỗi vòng lặp

### Đổi giao diện
- Nút 🌙 ở góc phải trên cùng app
- Bấm để cycle: System → Dark → Light → System
- App tự reload với palette mới

### Chỉnh sửa file
- Tab Files → bấm ✏ trên file bất kỳ (current hoặc recent)
- Editor full-screen mở ra với search + save
- Mọi thay đổi đẩy vào AppState ngay → AutoTyperService dùng nội dung mới nhất khi bấm START

---

## 🛠️ Tech stack
- **Android** Java + Material Components 1.11.0
- **Min SDK 23** · Target SDK 34
- **Gradle 8.5** + AGP 8.2.0
- **Fragment + BottomNavigationView** cho 3 tabs
- **AppState** singleton chia sẻ state giữa fragments
- **SharedPreferences** lưu config + recent files
- **SAF** (Storage Access Framework) cho picker/editor
- **AccessibilityService** cho auto-typing

## 📦 Cấu trúc project
```
app/src/main/java/com/ryan/autotyper/
├── SplashActivity.java      ← splash 1.4s
├── MainActivity.java        ← tab host + top bar + theme toggle
├── AppState.java            ← shared state singleton
├── PrefsHelper.java         ← config + recent files persistence
├── AutoTyperService.java    ← accessibility service, typing logic
└── ui/
    ├── home/HomeFragment.java
    ├── files/
    │   ├── FilesFragment.java       ← list + recent + empty state
    │   ├── EditorDialogFragment.java ← full-screen editor
    │   └── FilesUi.java             ← animations helper
    └── config/ConfigFragment.java
```

---

## ⚠️ Miễn trừ trách nhiệm
App chỉ dùng cho mục đích **giải trí**. Đừng spam người khác quá nhiều, đừng dùng cho mục đích thương mại hoặc lừa đảo. Tác giả không chịu trách nhiệm cho bất kỳ hậu quả nào do sử dụng app.

## 📝 License
Personal project — không phân phối lại dưới bất kỳ hình thức nào mà không có sự cho phép của tác giả.

---

Made with ❤ by Ryan · v3.4
