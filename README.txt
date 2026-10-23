ZEXO Android wrapper project

This is an Android Studio project that opens your live ZEXO website inside an Android WebView:
https://zxko-hub-q7vmo1.v2.appdeploy.ai/

How to build an APK:
1. Install Android Studio on a computer.
2. Extract this ZIP.
3. In Android Studio choose Open and select the extracted ZEXO_Android_Project folder.
4. Let Gradle sync and install any requested Android SDK components.
5. Select Build > Build Bundle(s) / APK(s) > Build APK(s).
6. Find the APK from the notification or app/build/outputs/apk/debug/app-debug.apk.

Important:
- This ZIP is the project source, not a precompiled APK.
- You need Android Studio (or another Android build environment) to compile the APK.
- The app loads the live website, so it needs an internet connection.
- Replace the website URL in MainActivity.java if you later connect a custom domain.
- Add your ZEXO logo as Android launcher icon assets before publishing to Google Play.
