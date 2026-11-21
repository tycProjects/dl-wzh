# NOMAN GO 3.7 - test report

**The APK was NOT built in the environment where this project was changed** (no Android SDK, Gradle or internet there).
Build it with GitHub Actions (see README / BUILD-GUIDE); the first build is the real compile test of the Kotlin code.

## Checked by running it
| What | How | Result |
|---|---|---|
| Home page: Frequently visited (counts, a site seen once is not listed, letter fallback, long-press remove, remembered after reload) | Playwright, browser build | pass |
| Same with the app bridge (`NAT.frequent`, `NAT.icon`, an icon that arrives later, `NAT.removeFrequent`) | Playwright, mock bridge | pass |
| Shortcut remove button (found broken by an old rename, fixed) | Playwright | pass |
| Settings > Privacy "Fetch site icons" visible in the app, hidden in the browser build | Playwright | pass |
| Viewer, Settings, History, Downloads (browser and app build), Translate settings | Playwright smoke | pass, no page errors |
| `clipboard.js`: Paste button fills the field (text with quotes, & and Bangla), no tap = refused, blocked site = refused, two reads together, permission queries, `read()`, `write()`, `writeText` fallback, injecting twice, buttons added later, no bridge = left alone | real Chromium, mock bridge | 11 / 11 pass |
| Link routing, `intent://`, `googlechrome://`, browser packages, fallback URLs, tel:, market:, file:, popup rule, upload accept list to MIME types, camera/recorder choice, camera result handling | Python mirror of the Kotlin logic | all pass |
| Tab manager logic (pin order, search, Close all, duplicates, back on a pinned tab) | Python mirror | 20 / 20 pass |
| Kotlin: brackets, imports, resource ids, manifest, FileProvider paths, every new function defined and used, XML and YAML parse | static checks | clean |

## Not checked (needs a phone or a real build)
* That the Kotlin compiles and the APK builds (it uses one new dependency, `androidx.core:core:1.12.0`).
* Camera and file picker on a phone, and uploads on ChatGPT / Gemini themselves.
* Favicon downloads (needs internet), the download card with a real download, the native clipboard read and the Allow dialog.
* 60 / 120 FPS and scrolling smoothness of the two-column tab grid on a real device.

## Bugs found and fixed
1. Uploads: `allowContentAccess = false` stopped the web engine reading files from the picker.
2. Links: `intent://` and `googlechrome://` were started as Android intents without checking the target, which opened Chrome.
3. Paste: WebView refuses `navigator.clipboard.readText()` (no permission prompt).
4. Shortcut remove button in Edit mode had stopped working (handler looked for a renamed class).
