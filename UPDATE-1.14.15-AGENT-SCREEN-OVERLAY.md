# Update 1.14.15 — Agent screen observation without AI-panel obstruction

- Temporarily hide the AI conversation panel and island overlay before reading accessibility content or capturing pixels.
- Apply/restore view alpha on the main thread and restore in `finally` even when capture fails.
- Wait 220 ms for the hidden frame to settle before observation.
- Keep the existing preference for external application windows; Android 14+ screenshot capture uses the foreground app window when available.
- No periodic/background screen capture is introduced.

Validation: source-level checks only. Gradle build and device-level overlay/capture behavior were not run in this environment.
