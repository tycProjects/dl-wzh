# Update: Gemini model capability verification and task routing

- Added live Gemini catalog parsing with `displayName`, `supportedGenerationMethods`, `inputTokenLimit`, and `outputTokenLimit`.
- Gemini chat discovery now uses Google's `models.list` response instead of accepting every model ID or relying on a fixed list.
- Added task routing for ordinary chat, complex reasoning/code work, screenshot/image understanding, and image generation.
- Gemini 4 Hydrogen is preferred for efficient everyday chat when the live API lists it; complex reasoning prefers Carbon, then Argon, Hydrogen, Gemini 3.8 Flash, and Gemini 3.1 Pro Preview when actually available.
- Screenshot analysis verifies that the selected model is a known multimodal family and supports `generateContent`; otherwise it selects a compatible model from the live catalog or returns a clear error.
- Image generation selects only a live-listed Gemini image model that supports `generateContent`.
- Gemini Live, Gemini Transcribe, Gemini TTS, video generation, embeddings, and Deep Research are explicitly prevented from falling through to ordinary chat because their dedicated transports are not yet implemented. Android `SpeechRecognizer` and `TextToSpeech` remain the active local voice paths.
- Fixed the nullable `AccessibilityNodeInfo` fallback in `IslandAccessibilityService.typeIntoSearch` by grouping the conditional fallback and explicitly typing the resolved node.
- Added unit tests for model routing and capability boundaries.

Build/test status: not verified in this environment because the Gradle 8.9 wrapper download failed with `UnknownHostException` for `services.gradle.org`.
