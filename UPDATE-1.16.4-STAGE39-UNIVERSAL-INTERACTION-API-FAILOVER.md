# Stage 39 — Universal Visual Interaction & API Failover Recovery

## Universal interaction changes
- Split semantic `scroll` / `gulir` from directional `swipe` / `geser`; direct intent parsing no longer maps every gesture word to scrolling.
- Added validated agent actions: `scroll_to`, `swipe`, `long_press`, `drag`, and two-finger `pinch`.
- `scroll_to` checks visible Accessibility text after each bounded scroll (1–12 swipes) and reports honestly when the target is not exposed or cannot be found.
- Scroll-container selection now compares visible scrollable regions and chooses the largest visible region instead of blindly selecting the first node in tree order.
- Swipe supports all four directions. Long-press and drag require visible, named source/target elements. Pinch is available only when the app content supports zoom.
- Visible-text collection now ignores nodes not marked visible to the user, reducing false matches from off-screen content.
- Updated the agent protocol to explain when to choose scroll versus swipe and when to use the new gestures. The existing observe-after-action loop remains in place.

## API failover changes
- Fixed the critical HTTP 429 path: rate-limited/quota-exhausted primary routes no longer throw immediately before trying configured backup keys/providers.
- On 429, 402, and provider-specific quota messages (including quota/`RESOURCE_EXHAUSTED` patterns), the affected route is marked exhausted for the current request and the next configured route is tried.
- 401/403 credentials/access failures also mark the failed primary key route so it is not retried in later rounds; other configured keys and backup providers can still be attempted.
- Added quota classification for common provider message variants, including HTTP 400/403 quota payloads.
- Expanded accepted explicit backup provider identities to supported OpenAI-compatible providers such as DeepSeek, OpenRouter, Mistral, Together, Fireworks, Cerebras, xAI, Ollama, and custom-compatible endpoints. Anthropic remains rejected because this client does not implement its Messages API.
- A fallback profile matching the primary endpoint is only filtered out when its API key is also identical; a different key on the same endpoint can now act as a real backup. Local Ollama fallback profiles may omit the API key.
- The malformed-plan intent resolver now uses the same routed chat function, so its recovery request also benefits from primary-key rotation and configured backup providers.
- Clears partial streamed text before switching routes so primary fragments cannot be mixed into the backup response.
- Added diagnostics for route failover without logging API keys or other secrets.

## Tests added/updated
- `ApiFailoverPolicyTest`: HTTP 429, quota messages, ordinary bad requests, credential and transient statuses.
- `AiFallbackProfileTest`: accepts DeepSeek/OpenRouter and continues rejecting malformed/unsupported provider profiles.
- `AgentGestureActionTest`: validates distinction between scroll and swipe and parsing of scroll-to, long-press, drag, and pinch.
- `AiCommandRouterTest`: verifies directional swipe is not parsed as semantic scroll.

## Important limitations
- Failover only works when at least one valid backup key or backup profile has been configured, has quota, is reachable, and supports the requested model/payload. The app cannot create a backup API key or guarantee that an external provider is available.
- HTTP 429 can mean rate limiting as well as exhausted token quota; the app tries configured alternatives rather than assuming the exact cause.
- `scroll_to` can match text exposed by Accessibility. Visual-only content, inaccessible canvases, protected screens, CAPTCHAs, authentication prompts, and app-specific gestures may still require OCR/visual observation or user action.
- Gesture dispatch acceptance is not proof that the app completed the gesture; the agent must observe the screen afterward and verify the goal.
- Drag uses visible text labels as source and destination anchors; pinch uses a centered two-finger gesture and may not suit every app layout.

## Verification
- Pure Kotlin compilation of `ApiFailoverPolicy.kt` and `AiFallbackProfile.kt` with a minimal endpoint/client stub: passed.
- Gradle unit tests/build: not run. Gradle 8.9 wrapper attempted to download from `services.gradle.org`, but the environment returned `UnknownHostException`.
- Android device tests for gestures and real provider quota/failover: not run. These require a device and valid primary/backup credentials.
