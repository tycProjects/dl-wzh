# WEB2APK V4.4 — Code Studio

This update adds a fifth bottom-navigation item, **Code**, while keeping Home, Create, Projects, and Settings intact.

## Supported editor workflow

- Create a local HTML/CSS/JavaScript website project.
- Open an existing ZIP project containing `index.html` or `index.htm`.
- Edit `index.html`, `style.css`, and `script.js` in separate tabs.
- Add images or other project assets from Android's document picker. They are saved under `assets/` and can be referenced from the HTML/CSS.
- Live-preview the current HTML with the editor CSS and JavaScript.
- Use formatting, find/replace, project search, go-to-line, snippets, trailing-space trim, word-wrap, font-size controls, and basic code checks.
- Save/export the project as a ZIP.
- Build a signed **Website/WebView APK** using the existing internal APK packager. The Code screen uses the current App Name, Package ID, version, icon, selected supported permissions, and creator-watermark settings from Create/Settings.

## Build scope

The editor builds HTML/CSS/JavaScript websites as Android WebView APKs. It is **not** a general Android Studio replacement and does not compile arbitrary Kotlin/Java Android projects or Flutter projects. Native Android/Flutter compiler toolchains are not bundled.

## Safety and preservation

- No new Gradle/Maven dependencies were added.
- Existing `compileSdk`, `targetSdk`, WebKit dependency, runtime template, APK signer, and internal build pipeline are unchanged.
- No Termux `RUN_COMMAND`, Usage Access, or broad storage permission was added to the installed app.
- Imported ZIPs are extracted into a temporary staging folder and validated before the active project is switched.
- Existing editor project folders are not deleted when creating/importing another project.
- Repeated asset filenames receive a numeric suffix rather than overwriting an earlier asset.

## Install/build this WEB2APK update

The one-time build of the WEB2APK installer itself still uses the existing source-build workflow in Termux. After installation, creating website APKs is done inside WEB2APK and does not invoke Termux.

```bash
cd ~
cp -a ~/WEB2APK-V4-Internal ~/WEB2APK-V4-Internal-backup-$(date +%Y%m%d-%H%M%S)
unzip -o /sdcard/Download/WEB2APK_V4_4_Code_Studio_Source.zip -d ~
cd ~/WEB2APK-V4-Internal
bash ./build-v4.sh
```

The script copies the installer APK to `Download/Web2APK/WEB2APK-V4-Internal.apk` and opens Android's installer when available.
