# Gemini automatic model routing

## Source of truth

The app does not assume that every model shown in a documentation page is enabled for every API key. It calls Google's native `GET /v1beta/models?pageSize=1000` endpoint and uses the returned model IDs and `supportedGenerationMethods` as the live availability check. A model is selectable only when the live API lists it and the required method is present. Documentation names are routing hints, not proof of account entitlement.

## Automatic task selection

| User task | Route | Verification |
|---|---|---|
| Ordinary chat / writing / translation / summaries | Gemini chat model (`generateContent`) | Must be listed by the API and support `generateContent`; prefer Gemini 4 Hydrogen, then Gemini 3.8 Flash, then available Gemini 4 reasoning models |
| Complex coding, debugging, algorithms, math, deep analysis | Reasoning-capable Gemini chat model | Must be listed and support `generateContent`; prefer Gemini 4 Carbon, then Argon, Gemini 3.8 Flash, then Gemini 3.1 Pro Preview |
| Screenshot / attached-image understanding | Multimodal `generateContent` model | Must be listed, support `generateContent`, and match a known image-input family; preserve the user's selected model when it passes this check |
| Image generation/editing | Nano Banana / Gemini image model | Must be listed, support `generateContent`, and match the Gemini image catalog; never send this task to a text-only chat model |
| Live voice, Gemini transcription, Gemini TTS, video generation, embeddings, Deep Research | Not routed through ordinary chat | Dedicated transports are not yet implemented; the router returns no Gemini model instead of silently sending the request to an incompatible chat endpoint |

## Existing Android-native voice behavior

Speech-to-text currently uses Android `SpeechRecognizer` and speech output uses Android `TextToSpeech`. The Gemini Live, Gemini Transcribe, and Gemini TTS model IDs in the supplied model list are not treated as integrated merely because they exist in the catalog. Dedicated API transport, permissions, streaming, cancellation, and result handling are still required before those models can be enabled.

## Safety and fallback behavior

- The live model catalog is checked before a non-default task-specific Gemini route is selected.
- A user-selected model is retained when it passes the task capability check.
- If a task has no compatible live model, the app returns a clear Indonesian error instead of silently choosing an incompatible model.
- Gemini 4 names are not hardcoded as guaranteed availability: they are candidates only if Google's live API returns them for the configured key/project.
- Model IDs that are preview, live, TTS, image, transcription, embedding, agent, or specialized task models are excluded from ordinary chat selection unless their dedicated client path is implemented.
