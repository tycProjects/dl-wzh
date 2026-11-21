# Stage 29 — Gemini OpenAI-Compatible Endpoint + Editable Base URL

## Changes
- Gemini preset now uses `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`.
- Gemini text chat and model discovery now use the shared OpenAI-compatible `/chat/completions` and `/models` flow, with Bearer API-key authentication.
- The `/v1beta/openai` path is preserved by URL normalization.
- Gemini's native `generateContent` route remains available specifically for the existing image-generation/catalog path.
- Base URL remains editable and is persisted through the existing settings repository.
- Provider selection and API-key auto-detection preserve a user-entered Base URL instead of silently replacing it with a preset.
- Known-provider custom paths and complete custom `/chat/completions` URLs are retained rather than forcibly canonicalized to a provider default.
- Added unit tests for Gemini endpoint normalization, native image API root preservation, manual Base URL retention, and custom paths.

## Verification
- Static source checks passed for the Gemini endpoint, common Chat Completions routing, `/models` discovery, and Base URL preservation helper.
- Gradle unit tests/build could not run: Gradle 8.9 distribution download failed because `services.gradle.org` was unreachable (`UnknownHostException`).
- No live Gemini API key was available, so live model discovery, chat, tool-calling, and image-generation behavior have not been claimed as verified.
