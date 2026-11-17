# WEB2APK V4.5.1 — AI API + Code Editor File Loading

## What changed
- Added an optional OpenAI-compatible Chat Completions API bridge to generated website APKs.
- Generated app owner configures HTTPS endpoint, model and API key from the floating **AI API** button after installation.
- API key is encrypted at rest using Android Keystore on that device; it is not embedded in the generated APK.
- Code Editor file picker opens recognized text/code files directly in the editor text box (HTML, CSS, JS, JSON, XML, Java, Kotlin, TypeScript, JSX/TSX, Python, Markdown, YAML, SQL, shell, Gradle, properties, SCSS, LESS and SVG).
- The currently open extra code file is synchronously saved before Build APK and Export ZIP, avoiding a delayed-save race.

## AI API use in a website
1. Enable **OpenAI-compatible AI API** in Create before building.
2. Build and install the generated app.
3. Tap the floating **AI API** button and enter an HTTPS Chat Completions endpoint, model name and your API key.
4. In website JavaScript call:

```js
async function askAI(question) {
  const result = await window.Web2APKAI.chat([
    { role: 'user', content: question }
  ]);
  return result.content;
}
```

This is for APIs that implement the OpenAI Chat Completions request/response format (for example, OpenAI-compatible services). It is not a universal adapter for every vendor-specific Gemini/Anthropic API schema. API keys are credentials: per-user local entry is convenient for personal apps, but do not distribute one shared private key in a public APK. For a public production app, route requests through your own authenticated backend/proxy. Usage may incur provider charges and requires internet access.

## Build
Use the same `bash ./build-v4.sh` command as the previous project. No new Gradle dependencies or SDK changes were introduced.
