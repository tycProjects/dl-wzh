# Stage 21 — AI Agent Truth, Tool Calling, News Grounding, and Shell Reliability

This stage is merged into the Stage 20 source archive. It intentionally preserves the existing Android Island features and safety gates.

## Changes

### AI tool orchestration
- OpenAI-compatible and Gemini non-streaming requests now advertise a generic `android_action` native function tool using each provider's request format.
- The tool arguments carry the actual action in `type`; `IntentResolver` remains the allowlist and normalizes the action before execution.
- Native `tool_calls` responses are normalized without overwriting the actual action type.
- If a compatible endpoint explicitly rejects the optional tools declaration with HTTP 400 mentioning tools/function calling, the client retries once without the declaration. Gemini has the same one-time fallback for explicit function-declaration/tool-config rejection. Other HTTP errors are not hidden.
- Streaming requests omit native tools because fragmented tool-call deltas are not assembled by this client yet.
- Gemini `functionCall` response parts are normalized into the same allowlisted action envelope as OpenAI-compatible `tool_calls`.

### Malformed model response recovery
- Invalid action envelopes are marked as broken, including malformed `actions` types and non-empty arrays containing no recognized actions.
- The agent asks the model for one strict-format retry.
- If the retry is still invalid, the task is stopped and the app explicitly reports that no action was executed; malformed model text is not treated as a successful answer.
- Existing observe/act bounds and action security policies remain in place.

### Grounded current-news path
- Current-news requests are sent directly to `WebSearch.deepSearch` before model generation.
- Retrieved source payloads are displayed as source material, not rewritten into model-generated headlines or URLs.
- If Brave/RSS retrieval fails or yields no verifiable items, the app reports the retrieval failure rather than allowing model memory to create a news roundup.
- Ordinary non-news web research continues to use the agent search action.

### Shell execution and evidence
- An explicit `/shell <command>` now runs the exact supplied command through `IslandTerminal` without asking the model to rewrite or invent the command.
- Existing ShellPolicy refusal and confirmation gates remain authoritative.
- The AI is used only for optional explanation after execution; the original command and raw terminal evidence are always shown.
- Confirmed risky commands also retain the raw terminal evidence in the final message.
- Shell evidence formatting has a larger cap so stdout/stderr metadata is less likely to be clipped at the default formatter boundary.
- Device diagnostics now fail closed on non-zero exit, timeout, or truncated output. Successful diagnostic responses retain command, status, exit code, stderr, structured summary, and raw stdout.

## Added tests
- Generic native `android_action` tool-call normalization preserves the inner action type for OpenAI-compatible and Gemini responses.
- Malformed action fields are marked broken.
- OpenAI-compatible and Gemini request payloads include native tools for non-streaming requests.
- Streaming and compatibility-fallback payloads omit native tools.

## Verification status
- ZIP integrity, XML well-formedness, changed-source structural checks, and Kotlin parser pass (no Kotlin syntax/`expecting` errors) were checked in the build workspace.
- Full Gradle unit tests and Android release build could not be executed in this environment: the Gradle 8.9 wrapper distribution was not cached and network access to `services.gradle.org` was unavailable. This is an environment limitation, not a passing build result.
- Runtime Shizuku execution, provider-specific native tool support, and live web retrieval still require testing on a device with the relevant permissions/API keys.

## Known compatibility boundaries
- Native function tools are offered on OpenAI-compatible and Gemini non-streaming agent requests. OpenAI-compatible endpoints that reject tools receive one compatibility retry without tools.
- Gemini provider-specific native function calling is implemented; streaming tool-call deltas remain disabled until the client can safely assemble fragmented arguments.
- A direct `/shell` command still requires Shizuku readiness and may require explicit user confirmation according to `ShellPolicy`.
