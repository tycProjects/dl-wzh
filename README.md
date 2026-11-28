# File Max

مدير ملفات وأرشيف لأندرويد (Kotlin + Jetpack Compose) بأسلوب ZArchiver، يعمل دون إنترنت. الأيقونة: مجلد مع سحّاب بدون خلفية (Adaptive + Monochrome).

## البناء
1. افتح المشروع بـ Android Studio (JDK 17) وانتظر مزامنة Gradle (SDK 35).
2. `./gradlew testDebugUnitTest` ثم `./gradlew assembleDebug`.

> لم يُترجَم الكود ولم يُجرَّب على جهاز في بيئة الإنشاء (لا وصول إلى Gradle/Maven). راجع `FEATURE_STATUS.md`.

## البنية
- `ArchiveFormats.kt` قراءة/تصفح/اختبار/استخراج: ZIP(+JAR/APK/EPUB) · 7Z · RAR/RAR5 · TAR(.gz/.bz2/.xz) · GZ/BZ2/XZ · ARJ · ZST · ISO، مع كشف الصيغة بالـ Magic Bytes وحماية Zip-Slip.
- `ArchiveWriter.kt` إنشاء: ZIP (AES-256 / ZipCrypto، مستويات، تعليق) · 7Z (LZMA2) · TAR.* · GZ/BZ2/XZ + تقسيم إلى `.001`.
- `FileTools.kt` سلة محذوفات، باحث تكرار، محلل تخزين، بحث متعمق، هاش، إعادة تسمية جماعية.
- `MainActivity.kt` / `Ui.kt` / `ArchiveScreens.kt` / `ToolScreens.kt` الواجهات.
المكتبات: zip4j، Apache Commons Compress، XZ، junrar.
