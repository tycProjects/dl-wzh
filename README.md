# TUANANH BROWSER v2.11 (Android)

## v2.11 - Sua loi do trang / captcha / toc do
- DO KHI VAO WEB: truoc day neu ho so chua co mui gio, app cho dò mui gio qua proxy (toi ~48s) roi moi tai trang.
  Gio: dung mui gio da luu (cache theo ho so + proxy), neu chua co thi do o nen, cho toi da 4s roi tai luon.
- CAPTCHA KHONG HIEN: (1) mat khau proxy chi duoc gui 3 lan/trang -> captcha mo nhieu ket noi bi 407; nay cho 60 lan/20s.
  (2) "Chan anh" lam captcha dang anh trong -> tu bat lai anh khi trang goi reCAPTCHA / hCaptcha / Cloudflare / Arkose / GeeTest...
- CAPTCHA DO: bo LAYER_TYPE_HARDWARE (gay trang trang/do iframe); keo-xuong-tai-lai chi hoat dong khi vuot tu mep tren
  (truoc day vuot trong captcha co the tai lai trang); canvas fingerprint chi can thiep canvas nho (<=250k px) nen
  captcha goi toDataURL khong con bi do; Date/timezone co cache.
- TANG TOC: the nen khi mo lai ho so chi tai khi bam vao (khong tai het cung luc); cache inject.js.
- CHONG TREO: onRenderProcessGone -> tu tao lai the khi WebView bi he thong giet (truoc day dung hinh / crash).
- Ghi chu: UA iPhone tren WebView Android van gui Client Hints cua Chromium -> Cloudflare co the lap captcha; dung "Tu dong" hoac thiet bi Android neu gap.

## Ban cu v2.4

Trinh duyet da the (multi-tab) kieu Chrome, moi ho so co proxy / User-Agent / UUID / WebRTC rieng.

## Build APK
- Android Studio: Open thu muc -> Build > Build APK(s) (ban debug, tu ky). JDK 17, Gradle 8.2+.
- Dien thoai: day len GitHub, build bang GitHub Actions `gradle assembleDebug`.
Yeu cau: Android 9+, Android System WebView / Chrome moi.

## Giao dien v2.3
- Bo thiet bi mau (Devices.java): iPhone 7 -> 18 Pro Max, Samsung, Xiaomi/Redmi, OPPO, Vivo, Sony,
  Pixel, OnePlus, Realme (~41 thiet bi) + "May tinh Windows" + "Tu dong". Chon 1 thiet bi -> UA + UUID
  + fingerprint (platform, so nhan, RAM, maxTouchPoints, screen width/height, devicePixelRatio, canvas seed)
  tu dong dong bo. inject.js spoof them screen.* va devicePixelRatio. Sua UA tay -> tu go bo thiet bi.
- Doi ten thanh TUANANH BROWSER. Proxy tach thanh 2 loai HTTP / SOCKS5 (segmented trong form,
  badge HTTP/SOCKS5 tren card ho so va menu trinh duyet). Neu chuoi proxy co scheme (vd socks5://)
  thi tu dong nhan dien loai.
- He thong mau "Midnight Identity": navy + indigo/cyan/violet, icon vector rieng, adaptive launcher icon.
- Man hinh ho so: hero gradient + thong ke, card 3 vung ro rang (danh tinh / cau hinh / hanh dong),
  badge PROXY/TRUC TIEP, FAB gradient, hieu ung xuat hien.
- Trinh duyet: chrome navy 3 tang (tab strip / thanh dia chi / trang thai ho so), nut so the mo
  tab switcher, menu bottom sheet, thanh tien trinh gradient, trang loi thiet ke lai.
- Form ho so: 3 section card (Danh tinh / Ket noi / Dau van tay), o nhap focus highlight,
  chon WebRTC dang segmented, UA picker bottom sheet, nut Luu co dinh, kiem tra dinh dang proxy.

## Hieu nang & Cau hinh (v2.4)
Man hinh "Hieu nang & Cau hinh may" (PerfActivity + SysInfo.java), mo tu card duoi hero trang chu.
Cac tinh nang THAT, khong so lieu ao:
- DOC CAU HINH MAY: thiet bi (Build.MANUFACTURER/MODEL), Android, CPU (SOC_MODEL API31+ hoac
  /proc/cpuinfo) + so nhan, xung nhịp (chi doc scaling_cur_freq, nhieu may chan quyen), RAM
  tong/trong (ActivityManager.MemoryInfo), man hinh + dpi + tan so quet (Display.getSupportedModes),
  pin + nhiet do (BatteryManager/PowerManager), bo nho (StatFs), phien ban WebView. Nut "Lam moi".
- TANG FPS: yeu cau tan so quet cao nhat man hinh ho tro (preferredDisplayModeId) cho cua so trinh
  duyet. Chi co tac dung neu man hinh 90/120Hz; may 60Hz giu nguyen.
- FIX LAG: render priority HIGH + layer phan cung cho WebView, tuy chon chan anh (setBlockNetworkImage),
  nut Xoa cache (don Cache/Code Cache/GPUCache cua app_webview*, khong dung cookie), nut Don RAM (System.gc).
- EP XUNG: app thuong KHONG THE ep xung that (can root). Thay vao do la "Uu tien hieu nang CPU" dung
  PerformanceHintManager (API 31+) — gui goi y hieu nang + bao cao nhip khung that qua Choreographer.
  Doc CPU governor (chi doc). Co ghi chu ro rang day khong phai ep xung.
- GIAM PING: ping phu thuoc proxy/mang, khong "giam" ao. Them DO PING THAT (TCP handshake, ms, 3 lan
  lay trung binh) toi host:port tu chon (mac dinh 1.1.1.1:443). Do truc tiep tu thiet bi, khong qua
  proxy WebView.Muon ping thap: chon proxy gan hon.
- Tuy chon luu trong SharedPreferences "pb" (highFps/hwAccel/blockImg/perfHint), BrowserActivity doc
  khi mo ho so va ap dung; perf hint bat/tat theo vong doi onResume/onPause.

## Tinh nang
- Nhieu the cung luc, popup mo the moi, keo xuong de tai lai, thanh tien trinh, tim Google ngay tren thanh dia chi.
- Menu: the moi, trang chu, trang web may tinh, chia se, sao chep, xoa cookie, thoat.
- Proxy HTTP (ho tro user:pass) hoac SOCKS5 (chi host:port — WebView khong ho tro xac thuc SOCKS5).
  Trang chi tai sau khi proxy san sang. Tai file bi chan khi dung proxy (tranh lo IP).
- UA mac dinh "Tu dong" = UA that cua WebView (bo dau hieu wv) -> Google/dang nhap chay muot nhat.
- UUID: du lieu web rieng + seed canvas. WebRTC: tat han hoac relay.
- Mo lai ho so se khoi phuc cac the dang mo.

## Gioi han WebView
Khong ep WebRTC UDP qua proxy, khong gia lap timezone/WebGL/font.