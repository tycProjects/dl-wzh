# Android Island 1.14.14 — Diagnostics & AI error tracing

- Diagnostics now writes an append-only full history to `files/diagnostics/internal.log`; the settings UI keeps only the latest 2,000 lines in RAM to protect low-memory devices. Export/Salin Semua reads the complete file, not just the RAM preview.
- Turning verbose recording off no longer deletes history. Errors are always persisted, even when verbose recording is disabled.
- Uncaught process exceptions are recorded with thread, exception, message and stack trace.
- AI requests log each route attempt and success, HTTP status failures, network exceptions, retry rounds, and final exhaustion of all configured routes. Provider/model/route context is included; credentials are redacted.
- Shell command exceptions are recorded in the same diagnostics history.
- The Diagnostics UI no longer truncates the visible in-memory tail to 500 entries.

The 503 shown in the screenshot means the upstream service returned Service Unavailable. It is not, by itself, proof that the API key is wrong. A fallback only succeeds if a separate configured provider endpoint is healthy and its model/key are valid. The new trace makes each attempted route and response visible.
