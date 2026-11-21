# Update 1.8.0 — Android 15+, cutout (iPhone / punch hole), cross-chipset hardening

1. minSdk 35 (Android 15+), target/compile 35. Baseline-profile module aligned.
2. CutoutDetector rewritten: reads the TOP-edge cutout from `currentWindowMetrics`; classifies notch / waterdrop /
   punch hole; re-detects on rotation, fold/unfold, density or resolution change (DisplayListener + onConfigurationChanged).
3. Cutout-aware placement (all profiles except Custom): capsule width is widened to always cover the real hole;
   an off-centre punch hole moves the capsule horizontally (`CutoutProfile.autoShiftDp`); a punch hole below the
   top edge centres the capsule vertically (`autoShiftYDp`). Touch window uses the same math. Your X/Y offsets are added on top.
4. Overlay windows: `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` and `fitInsetsTypes = 0` so no ROM (MIUI/HyperOS, ColorOS,
   One UI, Funtouch, Transsion) pushes the island below the status bar; sizes from real window metrics, not deprecated `getRealMetrics`.
5. Rendering: the AGSL lens bake is wrapped so a shader/driver/OOM failure (some Mali, PowerVR, old Adreno, Unisoc GPUs)
   falls back to plain artwork instead of crashing; hardware bitmaps are copied to software before baking.
6. Calls: ACTION_PHONE_STATE_CHANGED receiver replaced by `TelephonyCallback` (no duplicate events on dual-SIM MediaTek/Unisoc).
7. WhatsApp: avatars from any Drawable type (adaptive/vector), WhatsApp Business included in the accessibility filter.
8. Manifest/theme: telephony + touchscreen features not required, unused FOREGROUND_SERVICE removed, explicit
   receiver flag, cutout mode and contrast enforcement set for edge-to-edge on Android 15.

Not verified here: this environment has no Android SDK/Gradle, so the project was not compiled or run. Build once and test on a device.

## 1.8.0b — Exynos / mid-range stability, RAM, Samsung punch hole
- `DeviceProfile` (core): tier 0 (>=10 GB), tier 1 (<10 GB, e.g. Exynos 1380 phones with 6-8 GB), tier 2 (<=4 GB / low-RAM flag).
- Tier 1/2: artwork copy 256/160 px (was 512), LRU 2/1 entries (was 3), ambient loops 20/15 Hz (was 30),
  lens bake at 1/3 resolution (tier 2: skipped), drawing window 42 %/38 % of screen height (was 48 %), less buffer RAM.
- Cutout: sources tried in order (current metrics, maximum metrics, Display.getCutout), holes that do not touch the
  top edge (Samsung One UI) are found, and a status-bar-height fallback assumes a centred punch hole when a ROM reports none.

## 1.8.0c — Hidden on the lock screen
- `ScreenState.locked` (KeyguardManager.isKeyguardLocked), refreshed on screen on/off/user-present plus short delayed re-checks.
- While locked, `shouldShowOverlay` returns false: capsule, banners, calls and WhatsApp are all disposed; they return right after unlock.

## 1.8.0d — Calls stay visible on the lock screen; idle audit
- Lock screen: phone and WhatsApp calls (IslandActivity.Call) stay visible; idle capsule and every other banner stay hidden.
- Idle: the 2.5 s like/repeat refresh no longer runs while locked; the call-timer in the compact island now also waits while the screen is off.
- Audit: no polling outside expanded/playing states, no full-window scrim (single opaque capsule surface, overlay is NOT_TOUCHABLE when idle). Frame time was not measured here.

## 1.8.0e — New app logo
- Adaptive icon (background / foreground / monochrome for Android 13+ themed icons): dark indigo glass backdrop, island capsule with equalizer bars and camera lens, glass card below hinting the expanded island.
- Source art in docs/branding (logo.svg, logo-1024.png, logo-512-square.png for store listings).

## 1.8.0f — Exynos flat glass
- `DeviceProfile.isExynos` (Build.HARDWARE / BOARD / SOC_MODEL) and `flatGlass` (Exynos below 10 GB RAM, or the weakest tier on any chipset).
- Flat glass: FluidGlassBackdrop (expanded island, WhatsApp banner, call banner) skips frost blur, lens bake and artwork bitmap layers and draws a vertical two-tone gradient sampled from the artwork, plus the usual scrim, highlight and accent glow.
- Deliberately NOT done: LAYER_TYPE_SOFTWARE / removing FLAG_HARDWARE_ACCELERATED on the overlay. A software-rendered Compose window repaints on the CPU every frame, which is worse on Cortex-A55 cores than the GPU path.
- Ambient loops were already throttled (tier 1: 20 Hz; no full-refresh-rate infinite animations exist).

## 1.8.0g — Heap pressure while music / overlay is active
Audit result: media updates were already de-duplicated (no-op callbacks dropped, lyrics/artwork cached per track, position held in a MutableLongState read only by the progress bar, lyric line via derivedStateOf, wave loop stops when paused, overlay composition disposed when hidden). Remaining gaps closed:
- Diagnostics: `recordLazy {}` builds log strings only when diagnostics is ON (11 hot sites: layout, touch forward, touch bounds, state changes). Before, every touch-move and state emission allocated a string even when logging was off.
- IslandBus state observer no longer hashes the whole UI state (lyric list included) on every emission; the hash was only used for the log.
- Position/lyric clock: 250 ms (>=10 GB), 333 ms (mid), 500 ms (weak) via DeviceProfile.positionTickMs.
- Track artwork kept at 480 / 400 / 300 px by tier (was always 480).
Not done on purpose: LAYER_TYPE_HARDWARE. It is a View API; Compose graphicsLayer already renders through GPU RenderNodes, and an explicit hardware layer adds a GPU texture per layer (more EGL/GL mtrack, not less). distinctUntilChanged on IslandBus.state is a no-op because StateFlow already drops equal values.

## 1.8.0h — Expanded card no longer clipped in landscape (games)
- Cause: the drawing window was a fixed fraction (42 %) of the screen height. In landscape the screen is only ~320-400 dp tall, so the window was shorter than the media card and cut it off.
- Fix: `WindowTuning.windowHeightDp(screenHeightDp, requiredDp)`: the window is max(fraction, what the card needs), never above the screen height. The same value feeds the WhatsApp stack cap in IslandRoot, and applyLayout now resizes the window when settings change.

## 1.8.0i — Lower graphics RAM while the overlay animates
- Hidden capsule: while the panel was fully open, a CompactIsland (alpha 0) stayed composed under it and its waveform loop kept running while music played. It is now composed only while visible (first 40 % of the morph), via derivedStateOf so the animation does not recompose per frame.
- Artwork layers and the flat-glass gradient fade with CompositingStrategy.ModulateAlpha: alpha is applied directly to the single draw, so no full-surface offscreen GPU texture is allocated during cross-fades (works on every driver; Mali, Adreno, PowerVR).
- Blur: two ping-pong int buffers instead of one new array per box pass (6 -> 2 allocations per blur).
- Already in place (audited): temporary bitmaps in the lens bake are recycled; displayed bitmaps are not recycled on purpose (Compose may still draw them) and are bounded by the LRU + onTrimMemory.
- Not done on purpose: software layers (CPU repaint + a full copy in RAM, slower on Exynos A55 cores).
