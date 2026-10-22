# Motion Editor (Flutter) - v1

Layers, keyframes, timeline, live preview, MP4 export.

## Setup
1. `flutter create --org com.example --project-name motion_editor .` (inside this folder, creates android/ios)
2. Keep this repo's `lib/` and `pubspec.yaml` (overwrite the generated ones)
3. `android/app/build.gradle`: set `minSdkVersion 24` (needed by ffmpeg_kit)
4. `flutter pub get && flutter run`

## What's in v1
- Projects (save/load as JSON, autosave), presets 9:16 / 16:9 / 1:1 / 4:5
- Layers: rectangle, ellipse, text, image. Reorder, duplicate, split at playhead, delete
- Properties: X, Y, scale, rotation, opacity - each with keyframes + easing
- Blend modes (multiply, screen, overlay, ...), colors, corner radius, text + size
- Canvas gestures: drag to move, pinch to scale + rotate (keyframes automatically if animated)
- Timeline: ruler scrub, drag/trim clips, keyframe diamonds, zoom, duration
- Undo / redo
- Export: renders every frame with the same painter as the preview, encodes with FFmpeg, saves to gallery

## Not in v1 (next stages)
Video/audio layers, masks, effects (blur/glow/etc.), shape paths, vector drawing, curve editor, fonts import, motion blur.
