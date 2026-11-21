# Stage 22 — Shell isolation, Indonesian news RSS, cancellation, and chat export

## Changes
- Explicit `/shell ...` now returns the terminal's raw execution evidence directly. It no longer makes a second AI request to explain the shell output, so the next command cannot inherit/repeat the previous `dumpsys` output through that explanation prompt and consumes no extra model tokens for a shell summary.
- Direct shell and standalone news jobs are tracked. Clearing chat cancels these jobs, clears pending confirmation, and invalidates their callbacks so a late result cannot reappear in the cleared conversation.
- Shizuku shell processes started through the capture path are tracked and can be terminated when the user presses Stop or clears chat.
- Current-news requests use publisher RSS instead of an English-biased Brave context response. Indonesian RSS from ANTARA and CNN Indonesia is fetched and shown first, followed by international Noozra RSS items. Each article still requires a parseable publication date and original HTTPS publisher URL; there is no model-generated fallback.
- Added JSON chat export via the AI panel's **Ekspor** action. It includes role, message text, timestamp, error flag, and attachment names, but not API keys or other settings.

## Feed endpoints
- ANTARA: https://www.antaranews.com/rss/terkini.xml
- CNN Indonesia: https://www.cnnindonesia.com/rss

## Verification
- All 13 XML files parsed successfully.
- Eight structural checks passed for shell isolation, RSS routing, Indonesian feeds, export method/UI, and cancellation/process tracking.
- A full Gradle test/release build has not been run in this environment; do not treat this ZIP as a verified successful Android build until built with the project's Gradle/Android SDK environment.
- Active RSS HTTP connections are also disconnected on Stop/Clear, not merely ignored after completion.
