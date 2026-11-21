# Stage 26 — AI provider selection and downloadable artifacts

## Changes

- Gemini chat now honors the exact model selected by the user when that model is present in the live Gemini model catalog and supports the requested input modality.
- Removed silent Gemini model switching for ordinary prompts and removed the 503 overload fallback that could substitute another model without the user's consent.
- Updated the shared Gemini task selector so an explicitly selected model wins whenever it supports the requested task; automatic preference is used only when no valid explicit choice is supplied.
- Added/updated unit-test expectations for explicit Gemini model selection.
- Removed unverified model IDs from provider presets and the new-install model default. The app now discovers models from the selected provider's `/models` catalog and picks a valid catalog member only when the user has not selected one.
- Removed generic `sk-` key auto-detection because several providers use that prefix. Provider choice remains what the user selected; only distinctive Groq, OpenRouter, and Google key prefixes auto-detect a provider.
- Kept ready-to-use endpoint presets for supported providers. Custom endpoint editing remains an advanced option, not a prerequisite for built-in providers.
- Strengthened generated-file instructions to request named `file:<filename>` code blocks, which the app can convert into real downloadable attachments.
- Sanitized single-file download names, preserved relative paths inside ZIP exports, and cleaned up partially-created MediaStore entries when writing fails.

## Important behavior

- A user-selected Gemini model is not silently replaced because a prompt contains coding/reasoning keywords.
- If the selected Gemini model cannot accept an image, the app explains that the selected model lacks that capability and asks the user to choose a multimodal model.
- Other OpenAI-compatible providers continue to send the selected model ID as the request's `model` field. The live catalog is used for validation/default selection, not to replace a valid explicit choice.
- Downloadable source files are still generated from named code blocks in model output; this is not a general binary-file-generation transport. ZIP exports contain the complete extracted text files.

## Verification status

- Source ZIP integrity and XML parsing are checked after packaging.
- A full Gradle/Kotlin build could not be completed in this environment because the Gradle 8.9 wrapper distribution could not be downloaded (`UnknownHostException: services.gradle.org`). This package must therefore be treated as source-updated but not build-verified. Device testing is also pending.
