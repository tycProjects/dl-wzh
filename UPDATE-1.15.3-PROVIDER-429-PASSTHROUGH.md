# Stage 34 — Provider 429 passthrough and no local quota simulation

- Removed the per-API-key `keyExhaustedUntil` map and all 429-derived key cooldown/quarantine logic.
- HTTP 429 now exits the request immediately. Android Island does not retry, wait, rotate to another key, or call a backup provider for that same request after a 429.
- A later user-initiated request is sent normally; there is no app-side lockout window.
- Removed local parsing/estimation of daily quota, reset duration, token limits, and cooldown duration. `RateLimitPolicy` now only recognizes the unrelated known tool-call compatibility error.
- HTTP 429 display now passes through the provider's `error.message` (or raw response body when no message field exists), instead of adding a generic instruction or invented reset time.
- Retry behavior remains bounded for selected network/server failures (not HTTP 429).

Validation: source/static checks and ZIP integrity only. Gradle build/test must be run in an environment with the required Gradle distribution and Android dependencies available; no build success is claimed here.
