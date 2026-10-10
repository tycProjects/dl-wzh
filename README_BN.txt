ARAFAT AI VISION — Offline Android Starter
============================================

এই প্রজেক্টটি Android ফোনে Termux + Android SDK দিয়ে সরাসরি build করার বদলে,
Android IDE অ্যাপের মাধ্যমে build করার জন্য একটি native Kotlin starter project।

প্রজেক্টে আছে:
- Neon dark HUD UI
- Animated AI orb
- Offline local commands and limited FAQ
- Android SpeechRecognizer (অফলাইন recognition ডিভাইস/ভাষা প্যাকের ওপর নির্ভরশীল)
- Android TextToSpeech
- Battery status
- Clock
- No API key and no network permission

ফোনে APK বানানোর সহজ পথ:
1. Android ফোনে AndroidIDE-এর মতো Android Gradle project build করতে পারে এমন IDE ইনস্টল করুন।
   শুধু HTML editor/WebCode দিয়ে native APK compile করা যায় না।
2. ZIP extract করে ARAFAT_AI_VISION_Offline ফোল্ডারটি IDE-তে open/import করুন।
3. Gradle sync সম্পন্ন হতে দিন; IDE-তে Android SDK/JDK/Gradle support থাকতে হবে।
4. Build/Run চাপুন অথবা Gradle task :app:assembleDebug চালান।
5. APK সাধারণত app/build/outputs/apk/debug/app-debug.apk-এ তৈরি হবে।
6. APK ইনস্টল করার আগে Android-এর অনুমতি অনুযায়ী ওই IDE/file manager-কে Install unknown apps permission দিন।

Build compatibility:
- Android Gradle Plugin 8.6.1
- Gradle 8.7
- JDK 17
- compileSdk 35
- minSdk 26
যদি IDE-তে এই SDK/Gradle version না থাকে, compatible version install/adjust করতে হবে।

Voice notes:
- Speech recognition-এর জন্য RECORD_AUDIO permission শুধু mic চাপলে চাওয়া হয়।
- Offline speech recognition guaranteed নয়। Android Settings-এ offline speech/language data install থাকলে কাজ করতে পারে।
- TextToSpeech-এর Bengali voice সব ফোনে preinstalled নাও থাকতে পারে। TTS engine/language data অনুযায়ী ফল ভিন্ন হবে।
- App নিজে background-এ রেকর্ড করে না।

এই starter-এ কোনো APK আগে থেকে build/test করা হয়নি। Build error হলে IDE-এর সম্পূর্ণ error message দিয়ে সাহায্য নিন।
