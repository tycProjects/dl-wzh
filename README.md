# TUANANH BROWSER v3.0 MASTER (Android)

## v3.1 - Turbo nhanh hon + sua loi + giao dien
- ANH NHANH HON: anh trong man hinh dau tai NGAY + uu tien cao, anh gan man hinh tai truoc, anh xa tai luoi; preconnect may chu anh.
- RAM cache tu chinh theo may (toi da 64MB, file <=3MB), ton trong Cache-Control (no-store/private khong giu, max-age toi da 1 gio).
- May chu chan tai thang (403/429/5xx) hoac timeout -> tu dung proxy cho may chu do 1-5 phut (khong cho 4s moi anh nua).
- Toi da 32 ket noi song song; xoa RAM cache khi may thieu bo nho hoac renderer crash.
- SUA LOI: cache anh dung chung giua cac ho so (lo anh giua ho so nay sang ho so khac) -> nay khoa theo ho so; cache giu 404/het han mai mai -> nay co TTL.
- BAO MAT: Turbo mac dinh TAT (100% qua proxy, IP that khong lo). Che do Can bang/Toi da phai bat tay va co canh bao ro.
  Neu bat: chi file https, khong bao gio tai JS thang, khong gui/luu cookie qua duong IP that.
- LOI NHO DA SUA: trang web doi dang nhap HTTP bi huy am tham -> nay hien hop thoai dang nhap rieng (khong gui mat khau proxy);
  thanh dia chi khong con chay javascript:/file:/content:; avatar ho so ten bat dau bang emoji bi vo ky tu; nhap proxy thieu cong
  bao loi de hieu; thanh trang thai hien "PROXY" khi Turbo tat; Xoa cookie cung don cache anh RAM.
- CHUC NANG MOI: nhan giu lien ket/anh (mo the moi, sao chep, tai anh qua proxy); Mo lai the vua dong; Dong cac the khac;
  Tiet kiem du lieu (chan anh bat/tat ngay); Don bo nho dem.
- GIAO DIEN: hieu ung bam (thu nho nhe) cho nut / menu / FAB, font vua hien dai, bottom sheet muot hon, bong do nut chinh.

## v3.0 MASTER - moi
- NUT MASTER (man hinh chinh, hoac menu ⋮ trong trinh duyet): doi sang giao dien robot/cyber toi
  (robot canvas co mat chop + LED, luoi neon, font mono, goc vuong) va bat TURBO ENGINE. Bam lai de ve giao dien thuong.
- TURBO - chia luong proxy: trang HTML / API / dang nhap van di qua proxy (web thay IP proxy);
  anh, CSS, font, video, (JS tinh o che do Toi da) tai THANG qua WiFi, co cache dia 64MB theo tung ho so.
  3 muc: Tat / Can bang (chi tai nguyen ten mien khac trang dang xem) / Toi da. Captcha luon di qua proxy.
  Luu y: file tinh di thang -> web co the thay IP that o cac file do. Muon an tuyet doi: chon Tat.
- Chan quang cao + tracker, anh tai luoi (lazy) + giai ma bat dong bo, tat Safe Browsing (giam tre, khong goi Google bang IP that),
  pre-raster, uu tien renderer.
- Thanh trang thai hien IP that cua proxy + quoc gia + ping, va so file Turbo (⚡ di thang / ⛔ da chan).
- Moi: Tim trong trang, Kiem tra IP (mo trang tra IP trong the moi), Nen toi cho web, TAI FILE QUA PROXY (truoc day bi chan).

## v3.0 - sua loi
- LO MAT KHAU PROXY: onReceivedHttpAuthRequest gui user:pass proxy cho ca trang web khi trang do doi dang nhap HTTP. Nay chi gui cho proxy.
- Kiem tra proxy tu dien o "Mui gio" -> ghim mui gio, doi proxy van giu mui gio cu. Nay khong tu dien.
- Luu ho so lam mat cac truong khac; chuoi proxy co khoang trang/xuong dong gay loi.
- Cache mui gio luu user:pass proxy ro rang trong khoa -> nay dung ma bam.
- WebView khong duoc destroy khi tao lai man hinh (ro ri RAM); trang crash lap vo han -> gioi han 3 lan.
- Xoa ho so: don them cache Turbo + cache mui gio.

## Ban cu v2.11

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