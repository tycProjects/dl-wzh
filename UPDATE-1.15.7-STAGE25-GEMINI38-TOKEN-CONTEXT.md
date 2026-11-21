# Stage 25 — Gemini 3.8 model selection and token context control

- Updated Gemini default model and provider preset to `gemini-3.8-flash`.
- Reordered Gemini model selection to prefer `gemini-3.8-flash`, then newer 3.x Flash models; legacy 2.5 models are fallback only when newer models are not returned by the live model catalog.
- Kept native Gemini `generateContent` transport. Google documents `generateContent` as still supported for mainline models; Interactions API is now the recommended default for new agentic integrations. This avoids an unverified protocol rewrite in this patch while fixing the concrete 2.5 model selection bug.
- Reduced ordinary chat history replay from 12 to 8 messages.
- Added bounded agent-loop history compaction that preserves the original user goal and the latest observations, trimming oversized individual text blocks.
- Added model-selection regression tests.

## Token behavior

The system instruction is still sent on every stateless API request. This is required by the current client protocol; it is not stored server-side as a permanent one-time prompt. Context trimming reduces repeated conversation payload. Gemini 3.8 supports implicit context caching according to Google's model documentation, but actual cached-token billing depends on provider-side behavior and must be checked in usage metrics.

## Verification limitations

No full Gradle build or live API request was performed in this environment. The user's API key must never be embedded in source or logs.
