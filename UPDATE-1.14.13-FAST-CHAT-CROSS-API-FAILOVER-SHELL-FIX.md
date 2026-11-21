# Android Island 1.14.13 — Fast Chat, Cross-Provider Failover, Shell Reliability

## Changes
- Everyday Gemini chat and command interpretation now select the fastest verified chat-capable model from the live Gemini catalog, prioritizing Gemini 4 Hydrogen and Gemini 3.8 Flash when the account exposes them.
- Gemini model catalog is cached in memory for 10 minutes to avoid an extra `models.list` round-trip on every normal chat request.
- Added up to three explicitly configured cross-provider fallback profiles. Supported transports are Gemini native `generateContent` and OpenAI-compatible `chat/completions` endpoints.
- Automatic failover occurs on HTTP 408, 425, 429, 500, 502, 503, 504 and network I/O failures. It tries the primary provider's backup keys first, then configured fallback providers in order, and performs up to three short retry rounds.
- Fallback API credentials are persisted using the existing encrypted SecretVault path. No provider, model, URL, or API key is guessed.
- Added Settings > AI > API cadangan lintas provider. One profile per line:
  `provider|baseUrl|model|apiKey`
  Provider is `gemini` or `openai` (`openai` means an OpenAI-compatible chat-completions endpoint such as Groq or OpenRouter).
- Normal chat/command responses now have at least 4096 output tokens available; the setting can be raised to 8192. The system prompt no longer implies an arbitrary concise-answer limit.
- Explicit `shell:` / `run shell:` / `jalankan shell:` input is always handled as a direct shell request. Invalid commands are rejected locally instead of being reinterpreted by the AI as a MEMINFO diagnostic.
- Shizuku process completion no longer relies on the remote `waitFor(timeout)` implementation. Exit status is polled safely; stdout/stderr are drained concurrently with bounded retained output. This avoids the `process hasn't exited` race and limits memory use on large dumps.
- Diagnostic action logs now report failures rather than claiming a failed MEMINFO check was successfully run.

## Fallback profile examples
```text
openai|https://api.groq.com/openai/v1|llama-3.3-70b-versatile|YOUR_GROQ_KEY
openai|https://openrouter.ai/api/v1|YOUR_OPENROUTER_MODEL|YOUR_OPENROUTER_KEY
gemini|https://generativelanguage.googleapis.com/v1beta|gemini-3.8-flash|YOUR_GEMINI_KEY
```

Only enter providers and model IDs enabled for your own account. Fallback is for text chat and action-planning requests; image-generation requests still use their dedicated image client and are not silently rerouted through chat endpoints.

## Verification
- Added unit tests for fallback profile parsing and explicit shell-prefix handling.
- Full Gradle build/unit tests were attempted but could not start because the environment could not resolve `services.gradle.org` to download Gradle 8.9 (`UnknownHostException`). No APK build success is claimed.
