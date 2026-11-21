# Lyricsfile + LRCLIB integration (1.9.24)

## What changed
- Added `LyricsFileCodec` for the Lyricsfile 1.0 draft: safe YAML loading, duplicate-key rejection, input-size/cue/word limits, validation, and canonical export.
- Added a rich lyric document model with line timing, optional end times, word/segment timing, plain lyrics, language, duration, and instrumental metadata.
- Extended `FoundLyrics` with a validation report, rich document, and source label while retaining the existing `List<LyricLine>` playback interface.
- LRCLIB remains the primary provider. Synchronized LRC results are parsed and validated before being accepted; search candidates are ranked using duration difference and timing-quality warnings.
- Added import/export entry points to `LyricsRepository` for `.lyricsfile.yaml`.
- Plain lyrics remain a low-confidence fallback with estimated line positions. They are explicitly marked `synced = false` and `source = plain-estimate`; estimated positions must not be treated as verified synchronization.
- Added unit tests for line/word timing, encode/parse round-trip, unsupported versions, and invalid timestamps.

## Important format/provider boundaries
- Lyricsfile 1.0 is still a draft. Its `offset_ms` semantics are unresolved, so this implementation does not apply it.
- LRCLIB's synchronized endpoint supplies LRC line timestamps, not word timestamps. Word-level timing is preserved when present in a Lyricsfile document, but is never fabricated from LRCLIB lines.
- Overlapping lyric lines remain allowed. Equal starts are not automatically rejected.
- The existing playback UI still consumes `LyricLine(timeMs, text)`. Rich word cues are retained in `FoundLyrics.document` for future UI integration, but the current renderer has not yet been changed to karaoke-highlight individual words.
- The Lyricsfile codec is an API-level import/export facility; this patch does not add a new file-picker screen or automatically persist downloaded lyrics to disk.
- Strong/weak timestamp auto-correction is not introduced here. This patch validates and scores results; it does not overwrite a user's saved correction.

## Build verification
The Gradle wrapper could not download Gradle 8.9 in this environment because network DNS access to `services.gradle.org` is unavailable. Compile/test execution therefore remains unverified.
