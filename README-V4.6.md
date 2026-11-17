# WEB2APK V4.6 Pro AI Studio

## API settings in Notifications
- API setup is now shown directly under the Notifications option inside the permissions expander.
- Default Gemini OpenAI-compatible endpoint: `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`.
- Default model: `gemini-3.8-flash`.
- API keys are encrypted in WEB2APK's private preferences using an Android Keystore AES-GCM key.
- Save API, Test API and Clear key actions are available before building.
- “Insert API key into generated HTML / APK” is enabled by default to match the requested one-click build workflow. The builder injects a configuration script into local HTML and also packages a runtime config so a live URL can use the same settings.
- **Security warning:** An embedded key is recoverable from the APK by anyone who receives it. Use this only for private/personal builds. Turn embedding off for a safer release and configure the key separately in the installed app, or use a private authenticated backend for public distribution.
- Gemini/OpenAI-compatible API requests are HTTPS Chat Completions calls. API usage can incur provider charges; the Test API action sends a small `Reply with OK` request.

## UI and editor updates
- Removed the prominent brand icon from the builder's top header; the generated APK icon picker and launcher icon resources are retained.
- Reworked dark mode to a true black surface with higher-contrast text, input, permission, navigation and Code Studio colors.
- Code Editor supports Ctrl/Cmd+S, Tab indentation, simple auto-indent on Enter, explicit Save now, and more text/code file extensions in Add file.
- Fixed switching to a newly opened extra code file while an extra tab was already active, which could previously replace the newly loaded text with the old buffer.
- Existing HTML/CSS/JS tabs, ZIP import/export, previews, local workspace, permission settings, history, internal APK packaging, signing and app-icon replacement remain in place.

## Build / compatibility
- Builder version: 4.6.0 (versionCode 14).
- `compileSdk 33`, `targetSdk 33`, Java 17 compatibility and existing dependencies are unchanged.
- Build using the included script: `bash ./build-v4.sh`.
- Source checks do not guarantee a successful build on every Android/Termux installation; verify with the complete Gradle build and install test.
