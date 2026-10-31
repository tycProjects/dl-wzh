# Fake Social — مولّد صور محاكاة لمنصات التواصل

تطبيق أندرويد (Kotlin + Jetpack Compose + Material3) يصنع **صور محاكاة (Mockup)** لمنشورات ومحادثات وتعليقات بتصميم يشبه منصات التواصل، لأغراض الترفيه والتصميم والعروض التوضيحية.

> ⚠️ **الاستخدام المسؤول:** ممنوع استخدام التطبيق للاحتيال أو انتحال شخصية أو التضليل أو التشهير أو تلفيق أدلة.
> لذلك: يظهر تنبيه عند أول تشغيل، وتوجد علامة «Mockup • محاكاة» اختيارية (مفتاح داخل المحرر، مقفولة افتراضياً لأن الاستخدام الأساسي ميمز وفيديوهات ترفيهية).

## المزايا
- **10 منصات:** Facebook، Instagram (منشور + رسائل خاصة)، WhatsApp، X/Twitter، TikTok، Snapchat، Telegram، YouTube، LinkedIn، Messenger.
- **3 أنواع قوالب:** منشور (+ردود)، محادثة، تعليقات.
- تخصيص: صورة البروفايل (معرض صور بدون أذونات / كاميرا)، الاسم، اسم المستخدم، النص، الوقت، الإعجابات، التعليقات، الحالة، قائمة الرسائل/الردود (إضافة/حذف/تعديل، ورسالتي/رسالة الطرف الآخر).
- شريط حالة (ساعة/بطارية) وشريحة «Today» وعلامات قراءة ملونة في قوالب الشات.
- وضع فاتح/داكن، واتجاه المحتوى LTR/RTL (للعربية).
- معاينة حيّة وتصدير **PNG** إلى `Pictures/FakeSocial` + مشاركة.
- **سجل المشاريع** محفوظ في Room (فتح/تعديل/حذف).

## التشغيل في Android Studio
1. افتح Android Studio (Koala 2024.1.1 أو أحدث) → **Open** → اختر مجلد المشروع.
2. انتظر **Gradle Sync** (يحتاج إنترنت أول مرة). المطلوب JDK 17 (الـ JDK المدمج في Android Studio يكفي) وAndroid SDK 34.
3. اختر جهازاً/محاكياً (Android 8.0+ / API 26) واضغط **Run ▶**.

## البناء من سطر الأوامر
```bash
gradle wrapper --gradle-version 8.7   # مرة واحدة فقط لتوليد gradle-wrapper.jar (غير مرفق، انظر الملاحظات)
./gradlew assembleDebug               # الناتج: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest           # اختبارات الوحدة
```

## البنية
```
app/src/main/java/com/fakesocial/app/
├── data/        Models, ItemsCodec, Room (Entity/Dao/Database), ProjectRepository
├── util/        ImageStore (صور البروفايل), GallerySaver (MediaStore)
├── ui/          AppNav, HomeScreen, EditorScreen, HistoryScreen, theme/
├── ui/templates Common + Post/Chat/Comment templates (10 منصات)
├── EditorViewModel.kt, HistoryViewModel.kt   (StateFlow)
└── FakeSocialApp.kt, MainActivity.kt
```

## الإصدارات
AGP 8.5.2 · Gradle 8.7 · Kotlin 1.9.24 · Compose compiler 1.5.14 · KSP 1.9.24-1.0.20 · Compose BOM 2024.09.00 · Room 2.6.1 · Navigation Compose 2.7.7 · minSdk 26 · target/compile 34.

## ملاحظات معروفة (اقرأها)
- **لم يُبنَ المشروع ولم تُشغَّل اختباراته** في بيئة الإنشاء (لا Gradle/SDK/إنترنت). تم فقط فحص ثابت (توازن الأقواس، الـ imports، موارد `R.string`، سلامة XML). توقّع احتمال أخطاء بسيطة عند أول Sync/Build.
- **`gradle-wrapper.jar` غير مرفق** (ملف ثنائي). Android Studio يستخدم `gradle-wrapper.properties` لتنزيل Gradle 8.7 تلقائياً؛ ولاستخدام `./gradlew` ولّد الـ jar بالأمر أعلاه.
- **يحتاج تحققاً من الإصدار:** التقاط الصورة يعتمد على `rememberGraphicsLayer` / `GraphicsLayer.toImageBitmap()` (Compose UI 1.7، أي BOM 2024.09). لو رفضها الـ compiler فتأكد من وصول إصدار Compose UI إلى 1.7.x.
- لا يوجد خط مخصص: يُستخدم خط النظام. الأيقونات إيموجي/نصوص لتفادي شعارات المنصات الرسمية ومكتبة الأيقونات الموسّعة.
- **غير منفَّذ مقارنةً بالخطة الأصلية:** ستوري انستقرام، حالة واتساب، فيديو تيك توك، وسناب (صورة). التصدير بحجم شاشة الجهاز (دقة الكاميرا في الصورة الشخصية مصغّرة). تدوير EXIF لصور البروفايل غير مطبّق.
- على Android 8–9 يُطلب إذن التخزين عند الحفظ. ملفات الصور الشخصية القديمة لا تُحذف تلقائياً عند حذف المشروع.
- WorkManager غير مستخدم لعدم الحاجة إليه.

## التراخيص والعلامات التجارية
- كود المشروع: MIT (ملف `LICENSE`).
- المكتبات: AndroidX / Jetpack Compose / Room / Navigation / Kotlin Coroutines — Apache License 2.0.
- أسماء المنصات علامات تجارية لأصحابها؛ التطبيق **غير تابع لها ولا مدعوم منها** ولا يستخدم شعاراتها الرسمية. للنشر على Google Play راجع سياسة «الانتحال» و«المحتوى المضلِّل» وسمِّ التطبيق بشكل لا يوحي بالارتباط بأي منصة.
