# WEB2APK V4.6.3 — Gemini AI Fix

- AI in generated apps is **OFF by default**. Enable it only for builds that need the AI helper.
- API key embedding is **OFF by default**. The key remains encrypted in WEB2APK's Android Keystore storage; with embedding disabled, enter the key once in the generated app's **AI API** settings after installation.
- Native Google Gemini Interactions API is supported (`https://generativelanguage.googleapis.com/v1beta/interactions`) with the default model `gemini-3.8-flash`.
- OpenAI-compatible Chat Completions endpoints remain supported for other compatible providers.
- Builder **Test API** works even while the generated-app AI toggle is OFF.
- Code Studio’s **AI API quick start** button inserts a working sample chat UI that calls `window.Web2APKAI.chat(...)`.
- When AI is enabled for a build, endpoint/model defaults are bundled even when the API key is not embedded, so the installed app's settings dialog is prefilled.
- AI requests use `store=false` for Gemini Interactions API. Network access and a valid provider API key are required; provider usage may incur charges.

## Build

Run the existing `build-v4.sh` command from this project folder. The builder version is 4.6.3 (versionCode 17). Existing source files and unrelated features were retained.

## Build and AI usage

AI is an optional feature inside the generated website app; it does not create the HTML/ZIP source automatically. Select an HTML file, a ZIP containing `index.html`, or enter a live website URL before building. To add a ready-made AI chat example, open Code Studio and use **AI API quick start**.

## Important security note

Never enable API key embedding for an APK that will be shared publicly. Anyone who receives an APK containing a key may extract it and use it. The safer default is to keep embedding OFF and configure the key on each installed device.
