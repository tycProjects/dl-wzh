# Update 1.14.8 — Android AI shell bridge, opaque assistant panel, and WhatsApp priority recovery

## Android shell bridge
- Expanded the Shizuku command policy for Android-focused diagnostics and platform queries: `dumpsys` for supported services, `getprop`, `df`, `settings get`, package queries, `wm size/density`, `service list`, process/system summaries, selected `/proc` reads, battery sysfs reads, and restricted system-directory listing.
- Directly pasted supported Android commands in the AI chat now execute through Shizuku.
- Shell execution now captures stdout, stderr, exit code, and timeout status separately.
- The full result is displayed in chat and sent back to the configured AI model as untrusted data for explanation. The model is explicitly instructed not to treat command output as instructions or automatically run follow-up commands.
- Existing device diagnostic parsers retain the legacy plain-output `capture()` behavior.
- Shell composition, pipes, redirection, substitutions, scripts, arbitrary binaries, and unlisted commands remain blocked. Supported settings changes retain explicit-intent and value-range checks.

## AI panel appearance
- Replaced the translucent AI panel substrate with an opaque AMOLED-friendly dark surface.
- Made the header, chat viewport, assistant bubbles, error bubbles, and attachment surfaces opaque to prevent the app behind the panel from bleeding through.
- Kept Liquid Glass styling for the rest of the island; only the AI reading/work surface is made solid.

## WhatsApp banner recovery while AI is open
- Incoming WhatsApp notifications now temporarily take visual priority over the expanded AI panel.
- The AI conversation remains stored in `AiSession`; after the WhatsApp banner timeout, the expanded AI panel is restored.
- Expanding the WhatsApp stack allows the user to read it; collapsing the stack returns to the suspended AI panel.
- Opening WhatsApp explicitly releases the suspended AI state so the AI overlay does not cover the app the user chose to open.
- Calls retain higher priority.

## AI behavior
- Removed the hardcoded date from the system prompt; the model is instructed to use runtime-provided date context.
- Added contextual assistance, writing, rewriting, translation, supplied-content analysis, shell-result interpretation, and capability-honesty instructions while preserving the one-object JSON action protocol.

## Validation
- ShellPolicy compiled independently with kotlinc and representative allowlist/denylist commands were exercised.
- ShizukuShell compiled independently against lightweight Shizuku/state stubs.
- Full Gradle unit/build validation could not run in this environment because Gradle 8.9 was not cached and the environment could not resolve `services.gradle.org` to download it.
