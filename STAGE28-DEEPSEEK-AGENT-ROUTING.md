# Stage 28 — DeepSeek endpoint and agent routing fix

## Changes
- Corrected the DeepSeek public API root to `https://api.deepseek.com` (without the legacy `/v1` path). The app still stores a ready-to-use chat-completions URL, while request construction strips that suffix and appends `/chat/completions` exactly once.
- Corrected DeepSeek provider identity from generic `openai` to `deepseek` in the provider preset. The transport remains OpenAI Chat Completions-compatible, but diagnostics and model selection now know which provider is actually in use.
- Set `deepseek-flash` as the ready-to-use preset model. Model discovery still validates against the account's live `/models` response before connection testing.
- Added DeepSeek model preference ordering: `deepseek-flash` first for fast agent work, then `deepseek-v4-pro` if Flash is not offered. User-selected valid models remain respected.
- Added clearer DeepSeek-specific explanations for authentication, balance/quota, access, missing endpoint/model, and rate-limit failures.
- Added unit tests for endpoint normalization and live model preference.

## Why this matters
The previous preset pointed to `/v1/chat/completions`. Current DeepSeek API documentation specifies `GET /models` and `POST /chat/completions` from the API root, and lists `deepseek-flash` and `deepseek-v4-pro` as current model IDs. This mismatch could make both model discovery and chat execution fail before the agent could take an action.

## Verification
- ZIP integrity checked after packaging.
- XML parsing and source-level checks run.
- Full Gradle tests/build could not run because Gradle 8.9 distribution download failed with `UnknownHostException: services.gradle.org` in this environment.
- No live API key was available, so no real DeepSeek account request or device action was claimed as tested.
