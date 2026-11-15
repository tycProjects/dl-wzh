# Android Island — Low-End performance validation

## What this revision changes
- WhatsApp notification banners are published before avatar decoding; avatar drawable loading and scaling now run on `Dispatchers.IO`.
- Switching renderer mode drops the service's retained album-art reference and re-requests the active track through the existing background decode path. Bitmaps are not forcibly recycled while Compose may still hold a frame reference.
- Optional event-driven resource snapshots report Java heap, Dalvik PSS, Native PSS, other PSS, total PSS, and cumulative process CPU time. They are not collected per animation frame.
- Jelly's spring/morph code is intentionally unchanged.

## On-device test (required; cannot be simulated reliably in this build container)
1. Install the same APK on the target low-end phone. Enable Android Island diagnostics in the app before testing.
2. Force-stop, launch, enable Low-End, then play music for 3 minutes. Expand/collapse 20 times, including Jelly.
3. Send 20 WhatsApp messages with contact photos, including rapid bursts. Verify the first banner appears before its avatar is upgraded and that no notification is lost.
4. Export in-app diagnostics. Compare `RESOURCE_SNAPSHOT` records before/after mode switch and notification bursts.
5. Capture process memory and frame timing from a host with Android platform-tools:
   - `adb shell dumpsys meminfo com.android.island` (Java, Native, graphics and total PSS)
   - `adb shell dumpsys gfxinfo com.android.island framestats` (frame timing / jank)
   - `adb shell top -b -n 1 | grep com.android.island` (CPU snapshot; repeat during animation)
   - For GPU/render-thread attribution and frame timeline, record a Perfetto trace with Graphics, View, sched, freq, and memory categories.
6. Repeat the identical sequence in Normal mode on the same device, with the same player/artwork and message workload. Compare median and peak PSS, CPU%, missed/deadline frames, and time-to-banner.

## Acceptance criteria
- Low-End has lower median and peak PSS than Normal during the same workload.
- No sustained frame deadline misses during Jelly morphs; no new jank cluster when notifications arrive.
- CPU usage returns near idle after animations and notification processing settle.
- No monotonic memory growth across repeated track/avatar changes (allow short-lived GC fluctuations).
- Artwork/avatar replacement does not crash or display recycled-bitmap errors.

## Measurement limits
App-side `Debug.MemoryInfo` does not provide a trustworthy isolated GPU-memory number, and cumulative process CPU time is not CPU percentage. GPU and frame-jank validation must use `gfxinfo`/Perfetto on the actual device. No on-device benchmark results are claimed by this source-only patch.
