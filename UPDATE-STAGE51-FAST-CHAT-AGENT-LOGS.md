# Stage 51 — Fast chat, resilient agent, and usable diagnostics

## Implemented
- Ordinary questions, greetings, explanations, and attachments now use a direct chat-completions path. They no longer enter the device-action JSON protocol or observe/act loop.
- Explicit device commands continue through the protected observe/act agent.
- Common app-search-first-result tasks keep the deterministic first action: open/continue the requested app, locate its search field, type the query, press Enter, then tap the requested first result and re-observe.
- The unknown-language intent resolver is called at most once per agent task, rather than once per observation cycle. Its confirmation is reused only for task navigation; privileged action families still require matching intent in the original user text.
- Malformed agent JSON receives one repair attempt. If still malformed, only a deterministic local parse of the original user message is allowed; no extra network resolver call is made.
- Agent budget increased to 20 steps / 180 seconds / 4 repeated failures, while repeated completed and failed actions remain guarded.
- API request, API response latency, action duration, agent-loop duration, and total request duration are recorded.
- Diagnostics lines now include a local date/time timestamp and monotonic uptime.
- Log copy now flushes queued disk writes on an IO dispatcher, copies the complete persisted log, and displays success/failure feedback.
- API 2 is attempted only after an explicit quota/rate-limit exhaustion signal from the primary route and only if a backup profile is configured. Network and unrelated server/authentication errors do not silently trigger API 2 or repeated waits.

## Added tests
- Ordinary greetings and factual questions are not classified as device commands.
- Agent loop limits and completion semantics.
- The TikTok search → Enter → first video request is recognized as one deterministic task.

## Verification status
- ZIP integrity is checked after packaging.
- Full Gradle unit tests/build could not run because Gradle 8.9 distribution download failed with `UnknownHostException: services.gradle.org`.
- No device/runtime test is claimed.
