# Stage 38 — Chat formatting and agent reliability

## Changes
- Added `AssistantTextFormatter` to normalize assistant paragraph spacing, headings, lists, quotations, and fenced code blocks without changing user messages.
- Applied formatting to both completed and streaming assistant bubbles.
- Increased observe/act limits to 36 action steps and 5 minutes; repeated-failure stop threshold is now four.
- Expanded failure detection for results such as “tidak menemukan hasil”, “tidak muncul”, “tidak terbaca”, and timeout so recovery can select a different route.
- Strengthened recovery instructions: do not stop after one failed route; try distinct safe supported alternatives, while respecting permissions and confirmation gates.
- Expanded device-command intent recognition from 10 to 40 words. This fixes the supplied 13-word TikTok example being misclassified as ordinary chat.
- During screen observation, temporarily set the island overlay alpha to zero and visibility to `INVISIBLE`, then restore both in `finally`.

## Verification
- Source-level changes and regression tests were added/updated.
- Gradle unit tests could not be executed: the wrapper attempted to download Gradle 8.9 but the environment could not resolve `services.gradle.org` (`UnknownHostException`).
- Device-level TikTok execution and overlay observation remain unverified until installed and tested on an Android device with Accessibility permission enabled.
