# Android Island

Kotlin + Jetpack Compose. Current production integration scope: **Media + WhatsApp / WhatsApp Business**. The architecture is prepared for a generic notification activity pipeline, but other notification apps are not yet implemented.

## Update 1.16.5 — Deep search agent, berita WIB rapi, panel AI neon

- Berita RSS kini tampil sebagai daftar bernomor rapi: `Judul — Media (20 Nov 2026, 15:13 WIB) | Baca Sumber` — GMT dikonversi WIB, HTML mentah dan judul duplikat dihapus, URL panjang disembunyikan di balik tautan "Baca Sumber" yang bisa ditekan (NewsFormatter deterministik, tanpa campur tangan model).
- Agen AI punya `search_deep`: pencarian multi-ronde dengan varian kata kunci + pembacaan langsung halaman sumber; budget agent loop diperluas khusus riset (26 langkah / 300 detik).
- Panel AI dirombak ke identitas neon robotik: substrat AMOLED bercahaya, header inti berdenyut, pembatas gelombang One UI-style, chip neon outline, titik sibuk bernapas — semua draw-phase, tanpa tumpang tindih, hormati reduced motion & tier low-end.
- Bar progres musik di expanded card membawa gelombang bercahaya yang bernapas saat lagu diputar dan mereda saat pause.

## Update 1.14.8 — Android AI shell, opaque AI panel, and WhatsApp banner recovery

- Paste supported Android shell commands directly into AI chat; Shizuku returns stdout, stderr, exit code, and timeout information for AI explanation.
- Android-focused command allowlist expanded while keeping shell chaining, scripts, redirects, and unlisted commands blocked.
- AI chat surface is now fully opaque AMOLED dark instead of translucent; the rest of the island keeps its existing Liquid Glass styling.
- Incoming WhatsApp banners temporarily take priority over the expanded AI panel; the AI chat is restored after the banner timeout or after the user closes the WhatsApp stack.
- AI system instructions now include contextual assistance, writing, translation, content analysis, and shell-output interpretation. Hardcoded prompt date removed.
- Build version: 1.14.8 (versionCode 138).

## Update 1.11.4 — AI screenshot composer, chat deletion, and file picker

- "Screenshot Layar" now captures the foreground application window (using Accessibility screenshot APIs) and stages a compressed JPEG in the composer; it does not send automatically. The user writes a prompt and sends it with the image.
- On Android 14+, prefer `takeScreenshotOfWindow` for the foreground app so the AI overlay itself is not included in the attached image.
- The pending screenshot carries a small accessibility text context alongside the image when available.
- Conversation clearing invalidates in-flight AI callbacks, clears pending attachments and streaming state, serializes history-file deletion against writes, and prevents stale responses or writes from restoring deleted messages.
- File picker proxy no longer uses `noHistory`; it stays alive while the system document picker is open and copies the selected URI before finishing, preventing the attachment flow from being discarded.
- Action chips use a single horizontally scrolling row, with pending attachments adjacent to the composer so the prompt field remains visible.

## Architecture (aligned with reference approach)

| Component | Role |
|-----------|------|
| **IslandAccessibilityService** | Host overlay via `TYPE_ACCESSIBILITY_OVERLAY` (required to draw over status bar / camera cutout) |
| **WhatsAppNotificationListener** | WhatsApp notifications only |
| **MediaSessionService** | Media session playback |
| **IslandBus** | Reactive state, priority, media preservation under WhatsApp |

## Asisten AI (1.11.4)

- Panel chat **AI Island** hanya di island expanded: provider presets untuk OpenAI, Gemini, Anthropic-compatible/custom endpoints, OpenRouter, Groq, Mistral, DeepSeek, xAI, Together AI, Cerebras, Fireworks, dan Ollama/LM Studio; daftar model dimuat dinamis saat didukung provider.
- **Kirim file**: teks dibaca utuh (maks 40 ribu karakter), gambar dikompres (<=1024 px JPEG) untuk API vision.
- **Screenshot layar on-demand**: chip "Screenshot Layar" mengambil screenshot jendela aplikasi di belakang overlay dan menambahkannya sebagai lampiran pada composer. Pengguna menulis instruksi sendiri lalu menekan Kirim; tidak ada screenshot atau pembacaan layar otomatis.
- **Panggil**: One UI *Side button > Tekan dua kali > Buka app > "AI Island"* (ikon launcher kedua), atau Long press kapsul -> "Asisten AI".
- **Hemat RAM**: tanpa SDK AI/model on-device; client `HttpURLConnection` murni aktif hanya saat panel terbuka. Riwayat JSON kecil (maks 30 pesan).
- VoiceInteractionSession/RecognitionService sengaja ditunda; input keyboard dulu.

## Permissions

1. **Accessibility** — hosts the island overlay above the status bar. Window text and screenshots are accessed only after the user explicitly taps the screen-capture action; secure windows may block capture.
2. **Notification Listener** — reads WhatsApp / WhatsApp Business notifications and active media sessions.
3. **Shizuku (optional)** — used for supported Android shell diagnostics, selected system operations, and status-bar integration. Shell commands remain restricted by the Android command policy; Shizuku does not bypass Android's Notification Access or Accessibility consent screens.

## UI principles

- Compact = pure AMOLED `#000000`
- Expanded / Banner = Liquid Glass
- WhatsApp banner = avatar as full-card background
- Settings UI: dark premium grouped cards

## Build

Open in Android Studio or upload ZIP to cloud builder (gradlew included).

## Update 1.2.0

- Waterdrop capsule profile: square/flush upper edge with a subtly tapered, rounded lower edge.
- Auto top anchoring when cutout auto-detection is enabled; manual Y calibration remains available when disabled.
- Favorite button sends a rating/custom action to the active media session and only updates its saved state when the player accepts the command. Unsupported players show feedback instead of a fake toggle.
- WhatsApp banner uses near-full display width with roomier content spacing.
- Expanded width can be configured up to 720 dp and is constrained to the available screen width; minimum height range is expanded to 420 dp.
- Application ID / namespace: `com.android.island`.


## Overlay touch and bounds behavior (1.2.2)

- The accessibility overlay uses `WRAP_CONTENT` dimensions and `FLAG_NOT_TOUCH_MODAL`; it does not use `FLAG_NOT_TOUCHABLE`.
- `ViewTreeObserver.OnComputeInternalInsetsListener` restricts input to the rounded capsule/banner silhouette. The transparent WhatsApp banner side gutters and capsule corners are excluded.
- Compact waterdrop touch geometry follows the same top-flat/tapered-bottom path used by Compose.
- `FLAG_LAYOUT_NO_LIMITS` is intentionally omitted so the window cannot be laid out beyond the display edges.
- Horizontal offsets are clamped against the measured window width whenever the view is laid out, so expanded/banner widths remain on-screen.

## iPhone Notch overlay (v1.3.0)
- Optional persistent black notch overlay for waterdrop / punch-hole displays.
- Enable **Mode iPhone Notch** in Settings → Posisi & cutout.
- Adjust notch width (140–300 dp) and height (24–64 dp); defaults are 214 × 38 dp.
- The overlay stays visible while no media/notification is active, then morphs into the active island.
- This is a software overlay: it visually masks the camera area but cannot change the physical display cutout.


## Unified iPhone Notch / Compact behavior (1.3.1)

- Compact Island uses the exact same iPhone-notch silhouette and the same manually configured width/height as the idle notch. There is no second compact-size control while iPhone Notch mode is enabled.
- The accessibility overlay touchable region follows the notch path for idle and compact states; touches outside that path continue to pass through. Expanded and banner states retain their own visible shapes.
- Added **Sembunyikan saat idle**. When enabled, the service removes the overlay `ComposeView` and its composition while the island state is hidden, then recreates it for active media/notification content. The AccessibilityService remains running to detect new activity.
- The hide-when-idle setting, notch enable state, and manual width/height are persisted in Preferences DataStore.
- Notch width is adjustable from 100–360 dp and height from 20–72 dp.


### 1.3.1.1 Kotlin build fix
- Fixed `layoutParams` shadowing inside the ComposeView layout listener by explicitly referencing the service's `WindowManager.LayoutParams`.
- Removed calls to `ViewTreeObserver` internal-insets APIs that are not exposed by the public Android SDK and caused Kotlin compilation errors.
- The overlay remains `WRAP_CONTENT`, with Compose clipping and the existing touch handling. Note: the public SDK does not provide an app-level arbitrary window touch-region API; transparent rounded corners may remain within the small overlay window's rectangular hit bounds.


## Version 1.3.2 — Gesture & bounds correction
- iPhone Notch mode no longer responds to horizontal drag or swipe-down dismissal while compact; it stays fixed in place.
- Expanded swipe-to-dismiss now uses an in-bounds fade/scale animation instead of translating the card outside its WRAP_CONTENT window.
- Expanded overlay is re-centered after state changes; compact offsets are converted from dp to pixels before passing to WindowManager.


## 1.3.3 — Favorites, Power Saver & Notification Filter
- Heart button now always toggles a persistent Android Island favorites entry, and forwards a real MediaSession rating/custom action when supported by the active player.
- Added optional Low Power Mode: reduced media-position refresh cadence and disabled expensive blur/chromatic glass effects while retaining core features.
- Filtered WhatsApp background maintenance notices such as “checking for new messages”; normal message notifications remain eligible.


## 1.3.4 — Rendering, lifecycle and background-work optimization
- Reuse cached cutout geometry until display metrics change; exposes explicit invalidation.
- Keep liquid-glass drawing on the hardware-rendered overlay without forcing an offscreen buffer.
- Avoid regex-based WhatsApp status parsing and bound duplicate-notification memory.
- Cache per-track lyrics/accent work and avoid restarting lyric fetches on playback-state callbacks.
- Explicitly unregister overlay layout/touch callbacks and dispose the ComposeView composition on teardown.
- Add a lightweight spring/fade transition for the WhatsApp quick-reply panel and tune shared island motion.
- No wake locks are acquired by the notification listener.


## Android Island 1.3.5 — Morph, WhatsApp Banner & Shizuku

- Compact-to-expanded top position now follows the same morph progress as width and height.
- WhatsApp banner is centered at the expanded island's configured vertical position and width.
- Banner tap opens its notification PendingIntent; reply is shown only when Android provides RemoteInput.
- Added compact WhatsApp banner mode with width/height settings, message expansion arrow and right-swipe dismissal.
- Expanded island and WhatsApp banner use right-swipe-to-dismiss feedback.
- Removed decorative liquid-glass controls and simplified the surface to a restrained frosted-white treatment.
- Added optional Shizuku-backed notification status-bar icon hiding.
- Samsung Now Bar is not guaranteed to be affected: One UI exposes no stable public API for independently hiding that proprietary surface.


## Performance controls (1.3.6)
- Ultra Mode persists in DataStore and disables album-art blur and animated lyric shine while retaining media/notification listeners.
- Low-power media position refresh is throttled to about 1.1 seconds; normal mode remains responsive.
- Animation speed remains configurable and is applied by the shared IslandMotion morph spec.
- Lyric shine enable, speed, and intensity are configurable.


## Update 1.4.1 — banner motion and WhatsApp chat stack

- WhatsApp banner entrance now uses an independent spring channel. It does not translate or resize the compact capsule.
- The overlay Compose host no longer clips child drawing at its local content bounds; the banner can animate through its reserved capsule-to-card gap.
- WhatsApp notifications are held in a FIFO queue. A new contact does not replace the card currently being shown; repeated updates to the same notification refresh that entry.
- Tapping the WhatsApp banner/capsule opens a vertically scrollable stack of individual chat cards. Each card keeps its own Open, Reply and Dismiss actions.
- The compact island shows the number of pending WhatsApp notifications; when media is active, a WhatsApp indicator remains visible on the capsule.
- Per-notification expiry jobs are replaced/cancelled safely to prevent an older timer from clearing a refreshed notification.
- Removed the unused duplicate generic animation-spec helper. The main island morph continues to use one shared configurable spring/tween policy; banner entrance is deliberately isolated.

### Animation research / attribution

The motion and queue behaviour were informed by public implementation patterns in
[Arnav-Dugad/dynamic-island-android](https://github.com/Arnav-Dugad/dynamic-island-android)
(interruptible spring motion and fixed screen-space rendering) and
[bughunter11/Islandify](https://github.com/bughunter11/Islandify)
(queued notifications and per-message cards). These repositories are references only;
their source files were not copied into this project.


## Android Island 1.4.8 — Kotlin compile fix

- Fixed malformed manifest whitespace and corrected the status-bar notification-icon disable flag.
- Shizuku listener registration is idempotent and permission requests are gated by actual binder/permission state.
- IslandBus mutations are serialized to protect the activity queue from concurrent media/notification callbacks.
- Android 15+ only (minSdk 35). Real `DisplayCutout` geometry is read from the top edge: centred notch / iPhone-style island stays centred, an off-centre punch hole moves the capsule onto the hole, no cutout falls back to a centred capsule.
- Accessibility service no longer requests permission to retrieve other apps' view-tree content.
- Compact and idle overlays now use a bounded window instead of a full-screen transparent touch window, reducing the risk of blocking taps in other apps.

## Validation status

XML resources and manifest parse successfully. A full Gradle build and device-level overlay/permission tests remain required; this environment cannot download the Gradle 8.9 distribution or access an Android SDK.

## Android Island 1.4.9 — animation & banner stability

- Lower-cost, non-oscillating transitions when Low Power Mode and Ultra Mode are enabled together.
- WhatsApp banner swipe tracking no longer launches a coroutine for each pointer update.
- Expensive banner artwork blur and content-size animation are skipped in reduced-motion mode.
- Stable WhatsApp overlay host width reduces WindowManager re-centering while switching banner/compact states.
- Banner width control adapts to the display width and can extend to an 8 dp total side margin.

Static package verification is included. Release compilation still needs to be run in an environment with Gradle 8.9 and Android SDK access.


## 1.4.10
- Stable WhatsApp overlay host width and top-anchored expand/collapse morph.
- Shizuku status-bar API prefers the per-user binder method.


## 1.4.13 — Internal diagnostics build

This build adds an in-app diagnostic recorder independent of Android Logcat. Open **Home → Diagnostik internal** to inspect the latest records, copy the full buffer, or clear it. It records overlay window dimensions/bounds, layout updates, Island state, morph progress samples, expanded/capsule geometry, and Shizuku status-bar calls. Notification text and message contents are not recorded. This is an instrumentation build only: the Akira-style fixed drawing window + independent touch window architecture is not yet implemented, and the animation/anchor issue is not claimed fixed.

## 1.4.14 update

- Animation selector: Option 1 retains the prior stable profile; Option 2 adds a liquid-style panel reveal with a top-anchored scale and softer spring/easing.
- Diagnostic recording can be toggled in the home screen. It is enabled by default, stored only in the app's bounded in-memory buffer, and can be switched off without deleting existing entries.
- Shizuku still uses Android's `DISABLE_NOTIFICATION_ICONS` status-bar service flag for notification icons. A `READY` Shizuku state only confirms permission/binder readiness; actual OEM SystemUI behavior must be confirmed on the target device.


## Update 1.4.47

- Slightly larger compact defaults and wider compact-size adjustment range.
- Restored visible compact media activity when playback resumes after the island has become hidden.
- Retuned shared spring motion for a more responsive, controlled indicator transition, inspired by Droidbubble's motion approach; no source code copied.
- Build/device verification pending because Gradle distribution download is unavailable in this environment.


## 1.4.48
- Optimized animation invalidation by moving appearance progress reads into graphicsLayer.
- Prevented preference changes from restarting an active expand/collapse animation.


## 1.4.49 — Tema Dynamic Island
- Menambahkan pilihan tema Adaptif, Terang, Gelap, Native Putih, dan Native Hitam di Pengaturan.
- Menyimpan pilihan tema melalui Preferences DataStore.
- Menerapkan pilihan warna ke capsule, expanded media, stack WhatsApp, dan banner WhatsApp.
- Mode Adaptif tetap menggunakan analisis kecerahan artwork/avatar untuk menentukan warna teks; mode lainnya memaksa permukaan dan warna teks sesuai pilihan.


## Update 1.4.50 — Foreground color styles
- Replaced the incorrect surface-theme choices with Adaptive, White, Black, and Outline foreground styles.
- Compact capsule remains AMOLED black; foreground mode no longer changes its surface.
- Expanded media and WhatsApp banner keep their existing Liquid Glass surfaces across foreground modes.
- Legacy LIGHT/DARK/NATIVE settings migrate to White/Black so existing preferences remain readable.
- Adaptive continues to use the available album/avatar artwork luminance; wallpaper sampling is not yet implemented.


## 1.4.51
- Fix Kotlin compilation errors caused by the duplicated WhatsAppBanner source and missing ExpandedIsland implementation.
- Restore LikedTracks implementation.
- Keep island surfaces unchanged by foreground color modes.


## 1.4.52

Mode warna hanya mengatur teks, progress musik, dan tombol; permukaan capsule, panel, banner, serta Liquid Glass dipertahankan. Adaptive menganalisis artwork/avatar yang tersedia.

## Update 1.14.15 — Agent screen observation without AI-panel obstruction
- Before accessibility-tree observation or screenshot capture, the AI conversation/island overlay is temporarily hidden on the main thread.
- Capture waits for the hidden frame to settle, then reads the underlying foreground application; the overlay is restored in `finally`, including capture failures.
- Accessibility observation continues to prefer a non-Android-Island application window, and Android 14+ screenshot capture targets that application window when available.
- This is on-demand only; no background screenshot polling was added.
