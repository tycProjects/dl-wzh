# WEB2APK V4.5.2 — AI API + Code Editor reliability

## AI API
- Supports OpenAI-compatible Chat Completions APIs over HTTPS.
- In Create, enable **OpenAI-compatible AI API** before building.
- After installing the generated APK, tap its floating **AI API** control and configure endpoint, model, and your own key.
- Key is encrypted at rest with Android Keystore; it is not bundled into the APK.
- The JavaScript-to-native AI transport uses AndroidX WebView `addWebMessageListener`, restricts to the configured generated page origin, rejects iframe calls, and sends replies on the Android UI thread. For remote sites, the final page origin must match the website URL entered when building (redirects to a different host require rebuilding with that final URL). Requires an Android System WebView that supports `WEB_MESSAGE_LISTENER`; AndroidX WebKit added this listener API in 1.3.0, and this project already uses WebKit 1.5.0.
- The default endpoint is `https://api.openai.com/v1/chat/completions`. Other providers work only if they support the same request/response format. Vendor-native Gemini/Anthropic schemas are not automatically converted.
- API usage requires an active provider key, available network, and may incur charges. No API key can be created by the builder. For a public consumer app, use your own authenticated backend/proxy rather than sharing a secret key in an APK.

## Code Editor
- Add File opens recognized code/text files directly in the code box: HTML/HTM, CSS, JS/MJS/CJS, JSON, XML, Java, Kotlin, TypeScript, JSX/TSX, Python, Markdown, YAML, SQL, shell, Gradle, properties, SCSS, LESS and SVG.
- The AI API quick-start action adds a sample HTML form, CSS and JavaScript calling `window.Web2APKAI.chat()`.
- Active extra code file is included synchronously before Build APK and Export ZIP, avoiding a save/build race.
- Existing HTML/CSS/JS tabs, file paths, and asset workspace remain.

## Stability / preservation
- No Gradle dependency changes.
- `compileSdk 33`, `targetSdk 33`, AndroidX WebKit 1.5.0 unchanged.
- Existing runtime template bundling, APK packaging, signing, permissions, app icon, theme, and watermark retained.
- Build the installer using the existing `bash ./build-v4.sh`.
