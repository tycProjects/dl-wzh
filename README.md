# HTML to APK — GitHub Actions Starter

This is a starter Android WebView wrapper. GitHub Actions builds a debug APK in the cloud.

## Important limitation
This starter builds **one Android app per build**. It is not a public multi-user APK-conversion service. A real public converter needs a backend, authentication, upload validation, build queue, and storage.

## Build from an Android phone
1. Create a GitHub account and a **new public or private repository**.
2. Upload the contents of this folder to the repository (not the outer ZIP).
3. Open `app/src/main/assets/config.properties`.
4. Set `mode=url` and `url=https://example.com`, OR set `mode=local` and put your website files under `app/src/main/assets/site/`.
5. Open **Actions** → allow workflows if GitHub asks → choose **Build APK** → **Run workflow**.
6. Wait for the workflow to finish successfully.
7. Open that workflow run → **Artifacts** → download `html-to-apk-debug-apk` → extract the ZIP to get `app-debug.apk`.
8. Install the APK. Android may ask you to allow installs from your browser/files app.

## HTML / ZIP input
GitHub's web interface does not automatically unpack a ZIP into the Android assets folder. Extract the website ZIP first, then upload the extracted files into `app/src/main/assets/site/` while preserving folder structure. The entry file must be `index.html`.
For URL mode, the website must be reachable by the phone and allow loading in a WebView. Sites that block embedding, require special login flows, or rely on unsupported browser features may not work.

## Security
Only build projects and websites you trust. Do not put passwords, API keys, or personal tokens into source files. The APK is a debug-signed test build, not a Play Store release. Never give a GitHub personal access token to anyone or paste it into repository files.

## Customize
- App name: `app/src/main/res/values/strings.xml`
- URL/local mode: `app/src/main/assets/config.properties`
- Local website: `app/src/main/assets/site/`
- Package ID: `app/build.gradle`
