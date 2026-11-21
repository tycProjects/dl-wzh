# Android Island 1.11.3 — AI provider validation and model picker

## Changes
- Accept either a provider API root or a full `/chat/completions` URL and normalize it before model discovery and chat requests.
- Groq root URL is `https://api.groq.com/openai/v1`; full chat endpoint is normalized to the root so `/chat/completions` is never duplicated.
- Discover OpenAI-compatible models through `{root}/models` and Gemini chat-capable models through its native `/v1beta/models` catalog.
- Filter obvious non-chat model families from the selectable model list (Whisper, Prompt Guard, Llama Guard, embeddings, speech, transcription, moderation, safeguard).
- Display the provider's discovered models in a picker when more than one model is available. The user can refresh the list or enter a custom model ID manually.
- Add presets for DeepSeek, Mistral, Together AI, Fireworks, Cerebras, and xAI, in addition to OpenAI, Gemini, OpenRouter, Groq, Ollama, and Custom. Their API roots are provider-compatible presets; the in-app `/models` check remains the actual account-specific validation.
- Prefer Groq GPT-OSS models when available; model selection is based on the account's live `/models` response rather than a hard-coded assumption.
- Use Groq's current `max_completion_tokens` request field. Other compatible providers retain `max_tokens`.
- Retry a request once when a provider explicitly returns an HTTP 400 maximum-token limit, using the exact lower cap reported by the provider.
- Rework AI panel sizing: fixed bounded panel height, scrollable message area consumes remaining space, and the action chips/input stay pinned rather than being pushed off-screen.
- Version bumped to 1.11.3 (versionCode 124).

## Verification notes
- Added URL normalization unit tests for Groq base URL, full chat-completions URL, and `/models` URL.
- No API key is bundled in the source or build.
- Live provider model discovery requires the user's own API key and network access at runtime.
- Gradle build could not be run in this environment because Gradle 8.9 distribution download is blocked by DNS/network access (`UnknownHostException: services.gradle.org`).
