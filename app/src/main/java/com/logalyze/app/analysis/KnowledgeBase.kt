package com.logalyze.app.analysis

class L(
    val type: String,
    val summary: String,
    val cause: String,
    val fixes: List<String>
)

class KnownError(
    patterns: List<String>,
    val priority: Int,
    val baseConfidence: Int,
    val en: L,
    val ar: L
) {
    val regexes: List<Regex> = patterns.map { Regex(it, RegexOption.IGNORE_CASE) }
    fun text(lang: String): L = if (lang == "ar") ar else en
}

object KnowledgeBase {
    val entries: List<KnownError> = listOf(
        KnownError(
            patterns = listOf("OutOfMemoryError", "Java heap space", "GC overhead limit exceeded"),
            priority = 5, baseConfidence = 90,
            en = L(
                "OutOfMemoryError",
                "The game ran out of available Java heap memory.",
                "Not enough memory is allocated to Java, or the modpack needs more RAM than the launcher gives it. Heavy mods and a high render distance make it worse.",
                listOf(
                    "Increase the RAM allocation in the launcher (4–6 GB for modded, but less than your device RAM)",
                    "Remove or replace memory-heavy mods",
                    "Lower the render distance",
                    "Use a 64-bit Java version if possible"
                )
            ),
            ar = L(
                "OutOfMemoryError",
                "نفدت ذاكرة Java المتاحة للعبة.",
                "الذاكرة المخصصة لـ Java غير كافية، أو أن حزمة المودات تحتاج ذاكرة أكبر مما يمنحه اللانشر. المودات الثقيلة ومسافة الرسم العالية تزيد المشكلة.",
                listOf(
                    "زِد الذاكرة المخصصة في اللانشر (من 4 إلى 6 جيجابايت للمودات، بشرط أن تكون أقل من ذاكرة جهازك)",
                    "احذف المودات الثقيلة أو استبدلها",
                    "قلّل مسافة الرسم (Render Distance)",
                    "استخدم إصدار Java بنظام 64 بت إن أمكن"
                )
            )
        ),
        KnownError(
            patterns = listOf("Could not reserve enough space", "Invalid maximum heap size", "Too small initial heap"),
            priority = 5, baseConfidence = 92,
            en = L(
                "Invalid heap size",
                "Java could not reserve the amount of RAM set in the launcher.",
                "The RAM value is higher than what the device or the Java version can reserve.",
                listOf(
                    "Lower the maximum RAM in the launcher settings",
                    "Close other apps to free memory",
                    "Use a 64-bit Java version if possible"
                )
            ),
            ar = L(
                "حجم الذاكرة غير صالح",
                "تعذّر على Java حجز كمية الذاكرة المحددة في اللانشر.",
                "القيمة المحددة أكبر مما يستطيع الجهاز أو إصدار Java حجزه.",
                listOf(
                    "قلّل الحد الأقصى للذاكرة في إعدادات اللانشر",
                    "أغلق التطبيقات الأخرى لتحرير الذاكرة",
                    "استخدم إصدار Java بنظام 64 بت إن أمكن"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "UnsupportedClassVersionError",
                "class file version \\d+\\.\\d",
                "compiled by a more recent version of the Java Runtime"
            ),
            priority = 5, baseConfidence = 95,
            en = L(
                "Java version mismatch",
                "The game or a mod needs a newer Java version than the one in use.",
                "Minecraft and its mods are built for a specific Java version, and the launcher is using an older one.",
                listOf(
                    "Use Java 8 for Minecraft 1.16 and older",
                    "Use Java 17 for 1.18 to 1.20.4",
                    "Use Java 21 for 1.20.5 and newer",
                    "Pick the Java version in the launcher's settings for this instance"
                )
            ),
            ar = L(
                "عدم توافق إصدار Java",
                "اللعبة أو أحد المودات يحتاج إصدار Java أحدث من المستخدم.",
                "ماينكرافت والمودات مبنية لإصدار Java محدد، واللانشر يستخدم إصدارًا أقدم.",
                listOf(
                    "استخدم Java 8 لإصدارات ماينكرافت 1.16 وما قبلها",
                    "استخدم Java 17 للإصدارات من 1.18 إلى 1.20.4",
                    "استخدم Java 21 للإصدار 1.20.5 وما بعده",
                    "اختر إصدار Java من إعدادات اللانشر لهذه النسخة"
                )
            )
        ),
        KnownError(
            patterns = listOf("DuplicateModsFoundException", "Duplicate mods? (found|detected)", "Found duplicate mods"),
            priority = 5, baseConfidence = 93,
            en = L(
                "Duplicate mods",
                "The same mod is installed more than once.",
                "Two copies of the same mod (often different versions) are in the mods folder, so the loader stops.",
                listOf(
                    "Open the mods folder and delete the extra copy of the mod named in the error",
                    "Keep only the version that matches your Minecraft version"
                )
            ),
            ar = L(
                "مودات مكررة",
                "نفس المود مثبّت أكثر من مرة.",
                "توجد نسختان من المود نفسه (غالبًا بإصدارين مختلفين) في مجلد المودات، فيتوقف التحميل.",
                listOf(
                    "افتح مجلد المودات واحذف النسخة الزائدة من المود المذكور في الخطأ",
                    "احتفظ فقط بالنسخة المناسبة لإصدار ماينكرافت"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "Missing or unsupported mandatory dependencies",
                "which is missing!",
                "Unmet dependency listing",
                "Incompatible mod set",
                "Incompatible mods found",
                "Mod resolution failed",
                "requires (any )?version .* of"
            ),
            priority = 4, baseConfidence = 88,
            en = L(
                "Missing dependency",
                "A mod needs another mod or library that is missing or has the wrong version.",
                "One or more mods depend on a mod or library that is not installed, or the installed version does not match what they require.",
                listOf(
                    "Read the lines below to find the missing mod name and the version it needs",
                    "Download that mod for your exact Minecraft version and loader",
                    "Update the mods that report the problem"
                )
            ),
            ar = L(
                "اعتمادية ناقصة",
                "أحد المودات يحتاج مودًا أو مكتبة أخرى غير موجودة أو بإصدار غير مناسب.",
                "مود أو أكثر يعتمد على مود أو مكتبة غير مثبتة، أو أن الإصدار المثبت لا يطابق المطلوب.",
                listOf(
                    "اقرأ الأسطر أدناه لمعرفة اسم المود الناقص والإصدار المطلوب",
                    "نزّل ذلك المود لنفس إصدار ماينكرافت واللودر",
                    "حدّث المودات التي تُبلغ عن المشكلة"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "Mixin apply failed",
                "MixinApplyError",
                "InvalidInjectionException",
                "Mixin transformation of .* failed",
                "MixinTransformerError"
            ),
            priority = 4, baseConfidence = 85,
            en = L(
                "Mixin conflict",
                "A mod's code patch failed to apply.",
                "A mixin from one mod could not be applied. This usually means two mods conflict, or a mod was built for another game version.",
                listOf(
                    "Find the mod name in the error lines and update it",
                    "Remove the mod you added most recently",
                    "Make sure every mod matches your Minecraft version and loader"
                )
            ),
            ar = L(
                "تعارض Mixin",
                "فشل تطبيق تعديل برمجي من أحد المودات.",
                "تعذّر تطبيق Mixin من أحد المودات. غالبًا هناك مودان متعارضان، أو مود مبني لإصدار لعبة مختلف.",
                listOf(
                    "ابحث عن اسم المود في أسطر الخطأ وحدّثه",
                    "احذف آخر مود أضفته",
                    "تأكد أن كل المودات تطابق إصدار ماينكرافت واللودر"
                )
            )
        ),
        KnownError(
            patterns = listOf("GLFW error \\d+", "Pixel format not accelerated", "Failed to create window", "glfwInit"),
            priority = 4, baseConfidence = 85,
            en = L(
                "Graphics / GLFW error",
                "The game could not start the graphics system.",
                "The renderer or the graphics driver is not compatible with this game version or setup.",
                listOf(
                    "Try a different renderer in the launcher settings",
                    "Turn off shaders and heavy graphics mods",
                    "Update your graphics driver if you play on PC",
                    "Lower the graphics settings"
                )
            ),
            ar = L(
                "خطأ الرسوميات / GLFW",
                "تعذّر على اللعبة تشغيل نظام الرسوميات.",
                "المُصيّر (Renderer) أو تعريف كرت الشاشة غير متوافق مع إصدار اللعبة أو الإعداد الحالي.",
                listOf(
                    "جرّب مُصيّرًا مختلفًا من إعدادات اللانشر",
                    "أوقف الشيدرز ومودات الرسوميات الثقيلة",
                    "حدّث تعريف كرت الشاشة إن كنت تلعب على الكمبيوتر",
                    "قلّل إعدادات الرسوميات"
                )
            )
        ),
        KnownError(
            patterns = listOf("ZipException", "zip END header not found", "invalid LOC header", "Invalid or corrupt jarfile", "bad zip"),
            priority = 4, baseConfidence = 85,
            en = L(
                "Corrupted file",
                "A game or mod file is damaged.",
                "A jar or archive was not downloaded completely or is corrupted.",
                listOf(
                    "Download the mod or file again",
                    "Delete the damaged file from the mods folder",
                    "Reinstall the game version if it is the main jar"
                )
            ),
            ar = L(
                "ملف تالف",
                "ملف في اللعبة أو أحد المودات تالف.",
                "ملف jar أو أرشيف لم يُنزَّل بالكامل أو تالف.",
                listOf(
                    "نزّل المود أو الملف مرة أخرى",
                    "احذف الملف التالف من مجلد المودات",
                    "أعد تثبيت إصدار اللعبة إذا كان الملف هو الـ jar الرئيسي"
                )
            )
        ),
        KnownError(
            patterns = listOf("NoClassDefFoundError", "ClassNotFoundException"),
            priority = 3, baseConfidence = 75,
            en = L(
                "NoClassDefFoundError",
                "A class that a mod needs could not be found.",
                "A required library or mod is not installed, or a mod was built for a different Minecraft or loader version.",
                listOf(
                    "Check the mod page for required dependencies and install them",
                    "Make sure the mod versions match your Minecraft version and loader",
                    "Remove the mods you added most recently"
                )
            ),
            ar = L(
                "NoClassDefFoundError",
                "تعذّر العثور على كلاس يحتاجه أحد المودات.",
                "مكتبة أو مود مطلوب غير مثبّت، أو أن المود مبني لإصدار ماينكرافت أو لودر مختلف.",
                listOf(
                    "راجع صفحة المود لمعرفة الاعتماديات المطلوبة وثبّتها",
                    "تأكد أن إصدارات المودات تطابق إصدار ماينكرافت واللودر",
                    "احذف آخر المودات التي أضفتها"
                )
            )
        ),
        KnownError(
            patterns = listOf("StackOverflowError"),
            priority = 3, baseConfidence = 80,
            en = L(
                "StackOverflowError",
                "Code called itself too many times and overflowed the stack.",
                "Usually a bug in a mod, or two mods that call each other in a loop.",
                listOf(
                    "Find the mod name in the repeated lines of the stack trace",
                    "Update or remove that mod",
                    "Remove the mod you added most recently"
                )
            ),
            ar = L(
                "StackOverflowError",
                "الكود استدعى نفسه مرات كثيرة جدًا فامتلأ المكدس (stack).",
                "غالبًا خطأ في أحد المودات، أو مودان يستدعي كلٌّ منهما الآخر في حلقة.",
                listOf(
                    "ابحث عن اسم المود في الأسطر المتكررة داخل الـ stack trace",
                    "حدّث ذلك المود أو احذفه",
                    "احذف آخر مود أضفته"
                )
            )
        ),
        KnownError(
            patterns = listOf("NullPointerException"),
            priority = 2, baseConfidence = 60,
            en = L(
                "NullPointerException",
                "Code tried to use something that does not exist.",
                "Most often a mod bug or a conflict between mods. The mod name usually appears in the stack trace.",
                listOf(
                    "Look at the lines under the error for a mod name",
                    "Update that mod, or remove it",
                    "Remove the mods you added most recently"
                )
            ),
            ar = L(
                "NullPointerException",
                "حاول الكود استخدام شيء غير موجود.",
                "في الغالب خطأ في أحد المودات أو تعارض بين المودات، واسم المود يظهر عادة في الـ stack trace.",
                listOf(
                    "انظر إلى الأسطر تحت الخطأ بحثًا عن اسم مود",
                    "حدّث ذلك المود أو احذفه",
                    "احذف آخر المودات التي أضفتها"
                )
            )
        ),
        KnownError(
            patterns = listOf("---- Minecraft Crash Report ----", "This crash report has been saved to", "Game crashed whilst"),
            priority = 1, baseConfidence = 45,
            en = L(
                "Game crash",
                "The game crashed, but the exact cause is not in the knowledge base yet.",
                "The log contains a crash report without a known pattern. The line after \"Description:\" in the crash report usually explains what failed.",
                listOf(
                    "Open the crash report and read the \"Description:\" line",
                    "Remove the mods you added most recently",
                    "Update your mods and the loader"
                )
            ),
            ar = L(
                "انهيار اللعبة",
                "انهارت اللعبة لكن السبب الدقيق غير موجود في قاعدة المعرفة بعد.",
                "السجل فيه تقرير انهيار بدون نمط معروف. السطر الذي يلي كلمة Description في التقرير يشرح عادة ما الذي فشل.",
                listOf(
                    "افتح تقرير الانهيار واقرأ سطر Description",
                    "احذف آخر المودات التي أضفتها",
                    "حدّث المودات واللودر"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "wrong version is present",
                "requires version \\S+ of minecraft",
                "incompatible with (this )?(minecraft|game) version"
            ),
            priority = 4, baseConfidence = 88,
            en = L(
                "Wrong game version",
                "A mod was built for a different Minecraft version.",
                "The installed mod does not support your Minecraft version, so the loader refuses to start.",
                listOf(
                    "Check which Minecraft version the mod supports on its download page",
                    "Download the mod build that matches your exact Minecraft version and loader",
                    "Remove the mod if no matching build exists"
                )
            ),
            ar = L(
                "إصدار لعبة غير مناسب",
                "أحد المودات مبني لإصدار ماينكرافت مختلف.",
                "المود المثبّت لا يدعم إصدار ماينكرافت لديك، لذلك يرفض اللودر التشغيل.",
                listOf(
                    "تحقق من إصدار ماينكرافت الذي يدعمه المود في صفحة تحميله",
                    "نزّل النسخة المطابقة لإصدار ماينكرافت واللودر بالضبط",
                    "احذف المود إذا لم تتوفر نسخة مناسبة"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "UnsatisfiedLinkError",
                "Failed to locate library",
                "cannot open shared object file",
                "dlopen failed"
            ),
            priority = 4, baseConfidence = 84,
            en = L(
                "Native library error",
                "A native library needed by the game could not be loaded.",
                "The renderer or a native library (like LWJGL) is missing or does not match your device or Java version.",
                listOf(
                    "Reinstall the game version from the launcher",
                    "Try a different renderer in the launcher settings",
                    "Make sure the Java version and device architecture match the game version"
                )
            ),
            ar = L(
                "خطأ مكتبة native",
                "تعذّر تحميل مكتبة أساسية تحتاجها اللعبة.",
                "المُصيّر أو مكتبة native مثل LWJGL ناقصة أو لا تطابق جهازك أو إصدار Java.",
                listOf(
                    "أعد تثبيت إصدار اللعبة من اللانشر",
                    "جرّب مُصيّرًا مختلفًا من إعدادات اللانشر",
                    "تأكد أن إصدار Java ومعمارية الجهاز يناسبان إصدار اللعبة"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "NoSuchMethodError",
                "NoSuchFieldError",
                "AbstractMethodError",
                "IncompatibleClassChangeError"
            ),
            priority = 3, baseConfidence = 78,
            en = L(
                "Mod version mismatch",
                "A mod calls code that does not exist in the installed version.",
                "A mod and the game (or another mod it depends on) have different versions, so a method it needs is missing.",
                listOf(
                    "Update all your mods and their libraries to the latest matching versions",
                    "Make sure every mod matches your Minecraft version and loader",
                    "Remove the mod named in the error lines"
                )
            ),
            ar = L(
                "عدم تطابق إصدارات المودات",
                "أحد المودات يستدعي كودًا غير موجود في الإصدار المثبّت.",
                "إصدار المود يختلف عن إصدار اللعبة (أو مود آخر يعتمد عليه)، فتنقص دالة يحتاجها.",
                listOf(
                    "حدّث كل المودات ومكتباتها إلى أحدث إصدار مطابق",
                    "تأكد أن كل المودات تطابق إصدار ماينكرافت واللودر",
                    "احذف المود المذكور في أسطر الخطأ"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "UnknownHostException",
                "SocketTimeoutException",
                "Connect timed out",
                "SSLHandshakeException",
                "Failed to download"
            ),
            priority = 3, baseConfidence = 80,
            en = L(
                "Network / download error",
                "The launcher could not download a game file.",
                "The internet connection dropped, a server was unreachable, or a VPN or DNS setting blocked the download.",
                listOf(
                    "Check your internet connection and try again",
                    "Turn off VPN or change your DNS (for example to 8.8.8.8)",
                    "Delete the incomplete download and retry"
                )
            ),
            ar = L(
                "خطأ في الشبكة أو التحميل",
                "تعذّر على اللانشر تحميل ملف من ملفات اللعبة.",
                "انقطع الإنترنت، أو تعذّر الوصول للسيرفر، أو أن VPN أو إعداد DNS يمنع التحميل.",
                listOf(
                    "تأكد من اتصال الإنترنت وحاول مرة أخرى",
                    "أوقف الـ VPN أو غيّر الـ DNS (مثلًا إلى 8.8.8.8)",
                    "احذف التحميل الناقص وأعد المحاولة"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "Invalid session",
                "Failed to verify username",
                "Authentication servers are down",
                "Bad login"
            ),
            priority = 4, baseConfidence = 85,
            en = L(
                "Login / session error",
                "The game could not verify your account session.",
                "Your login session expired, or the authentication servers could not be reached.",
                listOf(
                    "Sign out of your account in the launcher and sign in again",
                    "Check your internet connection",
                    "Try again later if the authentication servers are down"
                )
            ),
            ar = L(
                "خطأ في تسجيل الدخول",
                "تعذّر على اللعبة التحقق من جلسة حسابك.",
                "انتهت صلاحية جلسة الدخول، أو تعذّر الوصول لسيرفرات التوثيق.",
                listOf(
                    "سجّل الخروج من حسابك في اللانشر ثم سجّل الدخول من جديد",
                    "تأكد من اتصال الإنترنت",
                    "حاول لاحقًا إذا كانت سيرفرات التوثيق متوقفة"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "Failed to bind to port",
                "Address already in use",
                "FAILED TO BIND TO PORT"
            ),
            priority = 5, baseConfidence = 92,
            en = L(
                "Server port in use",
                "The server could not start because its port is already used.",
                "Another server or program is using the same port.",
                listOf(
                    "Stop the other server or program that uses the port",
                    "Change server-port in server.properties to a free port",
                    "Restart the server"
                )
            ),
            ar = L(
                "منفذ السيرفر مستخدم",
                "تعذّر تشغيل السيرفر لأن المنفذ مستخدم بالفعل.",
                "هناك سيرفر أو برنامج آخر يستخدم نفس المنفذ.",
                listOf(
                    "أوقف السيرفر أو البرنامج الآخر الذي يستخدم المنفذ",
                    "غيّر server-port في ملف server.properties إلى منفذ غير مستخدم",
                    "أعد تشغيل السيرفر"
                )
            )
        ),
        KnownError(
            patterns = listOf("agree to the EULA", "Failed to load eula\\.txt"),
            priority = 5, baseConfidence = 97,
            en = L(
                "EULA not accepted",
                "The server will not start until you accept the Minecraft EULA.",
                "The eula.txt file still says eula=false.",
                listOf(
                    "Open eula.txt in the server folder",
                    "Change eula=false to eula=true and save",
                    "Start the server again"
                )
            ),
            ar = L(
                "لم تتم الموافقة على EULA",
                "لن يعمل السيرفر قبل الموافقة على اتفاقية ماينكرافت (EULA).",
                "ملف eula.txt ما زال مكتوبًا فيه eula=false.",
                listOf(
                    "افتح ملف eula.txt في مجلد السيرفر",
                    "غيّر eula=false إلى eula=true واحفظ",
                    "شغّل السيرفر مرة أخرى"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "Failed to read level\\.dat",
                "Corrupt(ed)? chunk",
                "Exception reading .*\\.mca",
                "Chunk file at .* is in the wrong location"
            ),
            priority = 4, baseConfidence = 82,
            en = L(
                "Corrupted world data",
                "A world or chunk file is damaged.",
                "The world was saved incorrectly (for example after a crash or a full storage), so a file cannot be read.",
                listOf(
                    "Back up the world folder first",
                    "Restore level.dat_old over level.dat if the problem is level.dat",
                    "Delete only the damaged region file named in the error (after taking a backup)",
                    "Make sure the device has free storage"
                )
            ),
            ar = L(
                "بيانات عالم تالفة",
                "ملف عالم أو شانك تالف.",
                "تم حفظ العالم بشكل غير سليم (مثلًا بعد انهيار أو امتلاء التخزين)، فلا يمكن قراءة الملف.",
                listOf(
                    "اعمل نسخة احتياطية من مجلد العالم أولًا",
                    "انسخ level.dat_old فوق level.dat إذا كانت المشكلة في level.dat",
                    "احذف ملف الـ region التالف المذكور في الخطأ فقط (بعد أخذ نسخة احتياطية)",
                    "تأكد أن في الجهاز مساحة تخزين كافية"
                )
            )
        ),
        KnownError(
            patterns = listOf("Failed to load datapacks", "Couldn't parse data file"),
            priority = 5, baseConfidence = 90,
            en = L(
                "Datapack error",
                "A datapack failed to load.",
                "A datapack is broken or built for another game version, so the world cannot load.",
                listOf(
                    "Find the datapack name in the error lines",
                    "Remove it from the world's datapacks folder",
                    "Use a version of the datapack that matches your Minecraft version"
                )
            ),
            ar = L(
                "خطأ في الداتا باك",
                "فشل تحميل داتا باك.",
                "الداتا باك تالف أو مبني لإصدار لعبة آخر، فلا يمكن تحميل العالم.",
                listOf(
                    "ابحث عن اسم الداتا باك في أسطر الخطأ",
                    "احذفه من مجلد datapacks الخاص بالعالم",
                    "استخدم إصدارًا منه يطابق إصدار ماينكرافت"
                )
            )
        ),
        KnownError(
            patterns = listOf("unable to create (new )?native thread"),
            priority = 6, baseConfidence = 90,
            en = L(
                "Can't create native thread",
                "The system could not create a new thread for the game.",
                "The device ran out of threads or native memory. Giving Java too much RAM can make it worse.",
                listOf(
                    "Lower the RAM allocation in the launcher",
                    "Close other apps",
                    "Reduce the number of heavy mods"
                )
            ),
            ar = L(
                "تعذّر إنشاء thread",
                "تعذّر على النظام إنشاء thread جديد للعبة.",
                "نفدت الـ threads أو الذاكرة native في الجهاز. تخصيص ذاكرة كبيرة جدًا لـ Java قد يزيد المشكلة.",
                listOf(
                    "قلّل الذاكرة المخصصة في اللانشر",
                    "أغلق التطبيقات الأخرى",
                    "قلّل عدد المودات الثقيلة"
                )
            )
        ),
        KnownError(
            patterns = listOf("code:? ?137\\b", "signal 9\\b", "SIGKILL"),
            priority = 5, baseConfidence = 85,
            en = L(
                "Killed by the system",
                "The system stopped the game, most likely because memory ran low.",
                "The operating system (often Android) killed the process to free memory, so there is usually no Java error in the log.",
                listOf(
                    "Lower the RAM allocation in the launcher",
                    "Close background apps before playing",
                    "Reduce render distance and heavy mods"
                )
            ),
            ar = L(
                "أغلق النظام اللعبة",
                "أوقف النظام اللعبة، غالبًا بسبب نقص الذاكرة.",
                "نظام التشغيل (غالبًا أندرويد) أنهى العملية لتحرير ذاكرة، لذلك لا يظهر عادة خطأ Java في السجل.",
                listOf(
                    "قلّل الذاكرة المخصصة في اللانشر",
                    "أغلق التطبيقات في الخلفية قبل اللعب",
                    "قلّل مسافة الرسم والمودات الثقيلة"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "SIGSEGV",
                "EXCEPTION_ACCESS_VIOLATION",
                "A fatal error has been detected by the Java Runtime Environment",
                "hs_err_pid"
            ),
            priority = 5, baseConfidence = 88,
            en = L(
                "JVM native crash",
                "Java crashed inside native code.",
                "A graphics driver, a native library, or unstable JVM arguments crashed the Java runtime itself.",
                listOf(
                    "Try a different renderer in the launcher settings",
                    "Remove custom JVM arguments you added",
                    "Update your graphics driver if you play on PC",
                    "Try a different Java build"
                )
            ),
            ar = L(
                "انهيار Java الأصلي",
                "انهارت Java داخل كود native.",
                "تعريف كرت الشاشة أو مكتبة native أو وسائط JVM غير مستقرة أسقطت بيئة Java نفسها.",
                listOf(
                    "جرّب مُصيّرًا مختلفًا من إعدادات اللانشر",
                    "احذف وسائط JVM التي أضفتها",
                    "حدّث تعريف كرت الشاشة إن كنت تلعب على الكمبيوتر",
                    "جرّب إصدار Java مختلفًا"
                )
            )
        ),
        KnownError(
            patterns = listOf("Could not find or load main class", "Unable to access jarfile"),
            priority = 5, baseConfidence = 90,
            en = L(
                "Game jar or main class missing",
                "Java could not start the game because its main file is missing.",
                "The game jar is missing or damaged, or the launch arguments point to the wrong class or file.",
                listOf(
                    "Reinstall the game version from the launcher",
                    "Reset custom launch or JVM arguments to default",
                    "Check the instance still has its game files"
                )
            ),
            ar = L(
                "ملف اللعبة الرئيسي غير موجود",
                "تعذّر على Java تشغيل اللعبة لأن ملفها الرئيسي غير موجود.",
                "ملف jar الخاص باللعبة ناقص أو تالف، أو أن وسائط التشغيل تشير إلى كلاس أو ملف خطأ.",
                listOf(
                    "أعد تثبيت إصدار اللعبة من اللانشر",
                    "أعد وسائط التشغيل أو JVM المخصصة إلى الوضع الافتراضي",
                    "تأكد أن النسخة ما زالت تحتوي على ملفات اللعبة"
                )
            )
        ),
        KnownError(
            patterns = listOf(
                "Unrecognized VM option",
                "Unrecognized option",
                "Unknown option",
                "Improperly specified VM option"
            ),
            priority = 5, baseConfidence = 93,
            en = L(
                "Invalid JVM arguments",
                "Java does not accept one of the launch arguments.",
                "A custom JVM argument is wrong or not supported by your Java version (some flags were removed in newer versions).",
                listOf(
                    "Open the JVM arguments in the launcher and remove the option named in the error",
                    "Reset the arguments to default",
                    "Use only flags supported by your Java version"
                )
            ),
            ar = L(
                "وسائط JVM غير صالحة",
                "Java لا تقبل أحد وسائط التشغيل.",
                "وسيط JVM مخصص خاطئ أو غير مدعوم في إصدار Java لديك (بعض الوسائط أُزيلت في الإصدارات الأحدث).",
                listOf(
                    "افتح وسائط JVM في اللانشر واحذف الخيار المذكور في الخطأ",
                    "أعد الوسائط إلى الوضع الافتراضي",
                    "استخدم فقط الوسائط المدعومة في إصدار Java لديك"
                )
            )
        )
    )
}
