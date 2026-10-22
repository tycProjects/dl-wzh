# SPERSH Hybrid HTML5 + Android Media3

هذا المشروع يربط واجهة HTML5 الحالية مع Media3 على Android:

- الواجهة الرئيسية: HTML/CSS/JS داخل WebView.
- عند الضغط على فيلم أو قناة داخل تطبيق Android، JavaScript يرسل بيانات المصدر إلى Android عبر `AndroidMedia3`.
- Android يفتح `PlayerActivity` ويشغّل المصدر داخليًا بواسطة Media3/ExoPlayer.
- إذا فتحت نفس `index.html` في متصفح عادي، لا يوجد Android bridge، فيرجع تلقائيًا إلى مشغل HTML5/HLS.js/Shaka/mpegts.js الموجود بالمشروع.
- الأفلام والقنوات ما زالت فارغة في `android-app/src/main/assets/www/data.js`.
- Media3 أفضل من HTML5 في Android لملفات مثل MKV وبث HLS/DASH، لكن دعم codec الفعلي يظل مرتبطًا بالجهاز/decoder.
- الترجمات الخارجية المضافة في data.js تمر إلى Media3. هذا المثال يعرّفها كـ WebVTT؛ يمكن توسيع MIME حسب امتداد الترجمة.

Build:
`./gradlew :android-app:assembleDebug`

المشروع مصدر Android وليس APK جاهزًا.
