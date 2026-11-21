# Stage 46 — User-Origin Command Authorization & Diagnostics Gate

## Changes
- Device actions in the observe/act loop are authorized from the original user message only. AI-generated prose is never treated as a new user command.
- When the local Indonesian/English imperative matcher does not recognize the user's language, a strict intent resolver receives the original user message and must return an allowlisted action before the proposed device action can run.
- Malformed agent output uses the same original-user-message resolver path; free-form AI text is never executed.
- Diagnostics initialization is inert while disabled: it does not read/create the log, publish a ready event, or write logs.
- Both normal events and errors are recorded only while Diagnostics is enabled. Pending disk writes and UI snapshot publication are also suppressed after the feature is switched off.

## Verification
- Source-level checks completed for the changed control paths.
- Gradle compile was attempted but could not start because Gradle 8.9 distribution download failed with `UnknownHostException: services.gradle.org`.
- Android runtime/device tests have not been run.
