# English Grammar Study — Android project

This is a corrected, importable Android Studio project based on the supplied **EXTREME ENGLISH.pdf**.

## What is fixed
- The PDF is bundled at `app/src/main/assets/extreme_english.pdf` for offline reading.
- The broken PDF file-descriptor handling was removed; the PDF is copied safely to the app cache before `PdfRenderer` opens it.
- Portrait phone layout and purple study UI are included.
- The app has a dedicated **🔥 Entrance Exam** indicator path. It is intentionally not guessed from ordinary MCQs; verified exam pages can be added to `isExamPage()`.
- Project uses Android Gradle Plugin 8.5.2 / Gradle 8.7 / compileSdk 35.

## Build an installable APK
1. Open the `EnglishGrammarStudyApp` folder in **Android Studio**.
2. Allow Gradle to sync and download Gradle 8.7 if prompted.
3. Choose **Build → Build APK(s)**.
4. The debug APK will be generated under `app/build/outputs/apk/debug/`.
5. Transfer that APK to your Android phone and install it.

### Important
I cannot honestly claim that an APK was compiled in this chat environment because the available build environment does not contain the Android SDK/Gradle toolchain. The previous ZIP was therefore a source project, not an APK. This corrected ZIP is intended to be opened in Android Studio and built there.
