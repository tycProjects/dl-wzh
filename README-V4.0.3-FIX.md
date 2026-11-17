# WEB2APK V4.0.3 — Runtime template validation fix

The previous app checked that the bundled runtime template was at least 50 KB. A valid WebView runtime APK can be smaller, so the app incorrectly displayed “Bundled runtime template is missing or incomplete.”

V4.0.3 validates the APK as a ZIP archive and checks for `AndroidManifest.xml`, at least one `classes*.dex`, and `assets/site/index.html`. The Gradle bundle task checks the same structure and verifies the asset copy. Builder version is 4.0.3 (versionCode 3) so it can update the installed V4.0.2 app.

The installed app uses the included ARSCLib package editor and apksig signer in-process. It does not call Termux, execute a shell, or declare `com.termux.permission.RUN_COMMAND`.
