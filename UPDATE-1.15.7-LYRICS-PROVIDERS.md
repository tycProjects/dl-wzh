# Update 1.15.7 — Free lyrics providers

- Added Better Lyrics as a cache-first provider. Its public documentation states cached responses are free and do not require an API key; fresh cache misses may require a key, which is not currently issued to new users. Such misses fall through to LRCLIB.
- Added a bounded TTML parser that preserves word timing where available, validates timestamps and track duration, and rejects malformed/low-confidence results.
- Retained LRCLIB as the open, no-key fallback and its existing Lyricsfile/word timing support.
- Reduced per-request network timeouts to avoid making every provider miss stall playback for an excessive time.
- Kept provider requests sequential and existing small LRU/miss caches in place to reduce data, battery and memory use.

Not integrated in this change: lrc.red/BiniLyrics pending compliance with its required client identification (including contact details) and visible attribution; other screenshot entries (SimpMusic, Unison, Kugou, Paxsenix, LyricsPlus, Zemer, YouTube Subtitle) are not wired because a stable, verified, free Android API contract and usage terms were not established. No API keys or paid services are embedded.

Build/device verification remains required. The Gradle wrapper could not download Gradle 8.9 in this environment because `services.gradle.org` was unreachable.
