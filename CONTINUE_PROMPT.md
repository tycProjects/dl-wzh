# برومبت المتابعة — File Max (انسخه كاملاً في محادثة جديدة مع إرفاق ملف ZIP للمشروع)

أنت مطوّر أندرويد خبير (Kotlin + Jetpack Compose). مرفق مشروع Android Studio باسم **File Max** (حزمة `com.filemax.manager`)، تطبيق أرشيف ومدير ملفات على طراز ZArchiver. أيقونة التطبيق جاهزة (لا تغيّرها). اقرأ `FEATURE_STATUS.md` و`README.md` أولاً. اعمل بدون أسئلة وبأقل استهلاك للتوكن (عدّل الملفات المتأثرة فقط، لا تعِد كتابة المشروع). سلّمني ZIP محدّثاً بعد كل خطوة.

## قواعد
- كل زر له عمل حقيقي، لا TODO ولا محاكاة. الواجهة عربية RTL + إنجليزية.
- المشروع كُتب دون أن يُترجَم: **أول مهمة** إن أرسلتُ لك أخطاء Gradle هي إصلاحها فقط.
- حافظ على: فحص zip-slip (`ArchiveEngine.safeTarget`)، حذف الناتج الجزئي عند الفشل/الإلغاء، عدم تسجيل كلمات المرور.

## الملفات
`ArchiveFormats.kt` (قراءة/استخراج/اختبار كل الصيغ عبر `ArchiveReader.visit`) · `ArchiveWriter.kt` (إنشاء) · `FileTools.kt` · `Ui.kt` (TaskRunner/TaskDialog…) · `ArchiveScreens.kt` · `ToolScreens.kt` · `MainActivity.kt`.

## ما أُنجز بالفعل (لا تعِد كتابته)
واجهة عربية/إنجليزية (طبقة `Strings.kt`: تترجم النصوص العربية عند العرض، أضف أي نص جديد لقاموس `EN`) · Foreground Service بإشعار تقدم وإلغاء + إيقاف مؤقت (`OperationService`, `TaskRunner`) · دعم SAF في الإنشاء والاستخراج عبر Cache · ZST وTAR.ZST (zstd-jni) · قارئ ISO مدمج (ISO9660 + Joliet) · كل ما في FEATURE_STATUS.md.

## المتبقي (بالترتيب)
1. تشفير 7Z عند الإنشاء (AES-256 + تشفير الأسماء) وفك/تصفح CAB وLZH: أضف **7-Zip-JBinding-4Android** (JitPack: `com.github.omicronapps:7-Zip-JBinding-4Android`، أضف مستودع jitpack إلى settings.gradle.kts) كمحرك إضافي، مع إبقاء المكتبات الحالية احتياطاً.
2. Root/Shizuku فعلي: `UserService` لعمليات الملفات، وتصفح `Android/data` و`Android/obb`.
3. حفظ كلمات السر بأمان (Keystore) اختيارياً، وZIP متعدد الأجزاء الأصلي (.z01) عند الإنشاء، وحوار الاستبدال عند اللصق/النسخ (الاستخراج فيه حوار جاهز).
4. اختصارات Launcher وويدجت، اختبارات Instrumented، توقيع Release، README محدّث.
