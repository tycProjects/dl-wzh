# Kineflux (Flutter)

CapCut x Alight Motion style editor: layers, video, keyframes, timeline, effects, MP4 export.

## Setup
1. `flutter create --org com.example --project-name motion_editor .` (inside this folder)
2. Keep this repo's `lib/` and `pubspec.yaml` (overwrite the generated ones)
3. `python3 patch_android.py` (adds media permissions + `minSdk 24`; safe to run twice)
4. `flutter pub get && flutter run`

## New in v2
- **Own media picker**: gallery browser with albums, All / Video / Photo tabs, multi-select with order numbers
  (several picks are laid out one after another on the timeline). Replaces the system image picker.
- **Video layers**: import from the picker, trim/split/move on the timeline, all effects work on video.
  Videos are converted to frames on import (max 60 s per clip), so the preview and the export match.
- **Adjust & Filters**: Vivid, Warm, Cool, Cinema, B&W, Noir, Vintage, Fade + brightness / contrast / colour.
- **Effects**: blur, glow, drop shadow, glitch (RGB split). Every slider has a keyframe diamond.
- **Animations**: In / Out (fade, slide, zoom, pop, spin) and Loop (pulse, wiggle, float, shake, rotate).

## Not yet
Audio (imported videos are silent in the export), masks, transitions, text styles/fonts, curve editor.

## App size
Uses `ffmpeg_kit_flutter_new_min` (core FFmpeg only: JPEG frames, MPEG-4 / MediaCodec H.264, PNG input, all that this app needs)
instead of the full-GPL build, which is what made the APK ~200 MB.

## Name + icon
App name is **Kineflux**. `python3 patch_android.py` sets it in the manifest and installs the icon from `tool/android_res/`
(`python3 tool/make_icon.py` redraws it; needs Pillow).
