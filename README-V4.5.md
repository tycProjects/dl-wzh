# WEB2APK V4.5 — AI API + Code Editor file loading

## What changed
- Adds an optional OpenAI-compatible Chat Completions API helper to generated Website APKs.
- Adds an AI API setup button to generated apps. Enter HTTPS endpoint, model and API key on the installed device.
- API key is NOT embedded in the APK. It is encrypted with an Android Keystore AES-GCM key and stored in that installed app's private preferences.
- Adds `window.Web2APKAI.chat([{role: "user", content: "Hello"}])`, `window.Web2APKAI.configure()` and `window.Web2APKAI.isConfigured()`.
- Code Editor's Add file now opens HTML/CSS/JS and common text/code files in the code box; other files remain project assets.
- Extra text files are saved into the project ZIP and included when building the website APK.

## AI API usage in your HTML / JS
```js
async function askAI(question) {
  try {
    if (!window.Web2APKAI.isConfigured()) {
      window.Web2APKAI.configure();
      throw new Error('Configure AI API, then try again.');
    }
    const answer = await window.Web2APKAI.chat([
      { role: 'user', content: question }
    ]);
    return answer.content;
  } catch (error) {
    console.error(error);
    throw error;
  }
}
```

## Provider compatibility
- Uses the OpenAI-compatible `POST /chat/completions` format over HTTPS.
- Enter a full endpoint such as `https://api.openai.com/v1/chat/completions` or a compatible `/v1` base URL, plus the model and API key.
- It is not a universal adapter for every provider's proprietary API schema (for example, native Gemini or Anthropic formats).
- API use requires provider credentials, network access and may incur billing. The key is entered on each installed app/device, never copied into the APK.
- This local client-side approach cannot protect a key as strongly as a server-side proxy. Do not distribute a privileged production key in a client app.

## Build
Use the existing `bash ./build-v4.sh` installer build command. Existing Gradle dependencies and compile/target SDK values are unchanged.
