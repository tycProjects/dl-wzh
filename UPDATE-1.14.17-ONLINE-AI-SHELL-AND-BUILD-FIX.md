# Android Island 1.14.17 — Online AI + AI-operated Android Shell

## Build fix
- Fixed `AiClient.kt` unresolved reference `provider` in `discoverGeminiCatalog()` by identifying that request as the native Gemini catalog path.
- Kept provider-aware diagnostics for OpenAI-compatible `/models` discovery.

## AI architecture direction
- AI requests stay online through configured providers. When an online provider is unavailable, the app reports the failure instead of silently running a local intent parser and pretending the AI handled the request.
- Device actions still execute on-device after the online model returns structured actions; this is device control, not local model inference.
- No on-device LLM/NPU/runtime/model package was added.

## Shell behavior
- User messages are sent as natural-language intent to the online AI. The app no longer directly executes pasted shell commands or routes ordinary device requests through the local command parser when AI is unavailable.
- Only the AI-generated `shell` action reaches ShizukuShell.
- Shell remains a real Android `sh -c` terminal. The previous tiny command allowlist was replaced with a broad single-task command policy, a 2,048-character limit, control-character rejection, and explicit blocking of high-risk destructive/system operations unless the user's message clearly requests that type of action.
- AI prompt now tells the model to choose ordinary Android/Linux shell utilities needed for the requested task, keep commands concise, inspect outputs, and explain stdout/stderr/exit status.
- Added tests for common diagnostics, `pm trim-caches`, pipes, destructive commands, oversized commands, and ensuring direct chat input is never executed as shell.

## Verification
- ZIP integrity checked after packaging.
- Kotlin compiler syntax check performed for ShellPolicy.
- Full Gradle compile could not be run in this environment because the Gradle wrapper attempted to download Gradle 8.9 and `services.gradle.org` was unreachable. Re-run `:app:compileReleaseKotlin` in Newly to verify the complete Android build.
