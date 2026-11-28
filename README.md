# File Max

مدير ملفات وأرشيف لأندرويد (Kotlin + Jetpack Compose) بأسلوب ZArchiver، يعمل دون إنترنت. الأيقونة: مجلد مع سحّاب بدون خلفية (Adaptive + Monochrome).

## البناء
1. افتح المشروع بـ Android Studio (JDK 17) وانتظر مزامنة Gradle (SDK 35).
2. `./gradlew testDebugUnitTest` ثم `./gradlew assembleDebug`.

> لم يُترجَم الكود ولم يُجرَّب على جهاز في بيئة الإنشاء (لا وصول إلى Gradle/Maven). راجع `FEATURE_STATUS.md`.

## البنية
- `ArchiveFormats.kt` قراءة/تصفح/اختبار/استخراج: ZIP(+JAR/APK/EPUB) · 7Z · RAR/RAR5 · TAR(.gz/.bz2/.xz) · GZ/BZ2/XZ · ARJ · CAB · LZH · ZST · ISO، مع كشف الصيغة بالـ Magic Bytes وحماية Zip-Slip.
- `ArchiveWriter.kt` إنشاء: ZIP (AES-256 / ZipCrypto، مستويات، تعليق) · 7Z (LZMA2، ومشفّر AES-256 عبر 7-Zip-JBinding) · TAR.* · GZ/BZ2/XZ + تقسيم إلى `.001`.
- `FileTools.kt` سلة محذوفات، باحث تكرار، محلل تخزين، بحث متعمق، هاش، إعادة تسمية جماعية.
- `MainActivity.kt` / `Ui.kt` / `ArchiveScreens.kt` / `ToolScreens.kt` الواجهات.
المكتبات: zip4j، Apache Commons Compress، XZ، junrar.

- `Priv.kt` / `PrivService.kt` / `aidl/IPrivService.aidl`: وصول مميّز لـ Android/data وAndroid/obb عبر Shizuku UserService أو Root.

## الإصدار 1.6.0
- اختصارات Launcher (الضغط المطوّل على الأيقونة): التخزين الداخلي، التنزيلات، Android/data. وويدجت «مجلدات سريعة» بثلاثة أزرار (`QuickWidget.kt`).
- حفظ كلمات السر اختيارياً (`SecureStore.kt`، Keystore).
- حوار الاستبدال عند النسخ/النقل/اللصق، وZIP متعدد الأجزاء الأصلي (.z01).
- اختبارات Instrumented: `app/src/androidTest/.../ArchiveInstrumentedTest.kt` (تشغيل: `./gradlew connectedDebugAndroidTest` على جهاز/محاكي).

## توقيع Release
1. أنشئ مفتاحاً: `keytool -genkeypair -v -keystore release.jks -alias filemax -keyalg RSA -keysize 4096 -validity 10000`
2. انسخ `keystore.properties.example` إلى `keystore.properties` واملأه (الملف مُستبعد في `.gitignore`).
3. `./gradlew assembleRelease` أو `bundleRelease`؛ بدون الملف يُبنى Release غير موقّع.
