package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object EduRepository {

    const val TECH_SUPPORT_PHONE = "01142676346"
    const val PLATFORM_NAME_DEFAULT = "منصة حمو التعليمية"
    const val FOOTER_CREDIT = "جميع الحقوق محفوظة - إعداد حمادة ابن ضيف"

    // Authenticated users presets
    val ownerAdminUser = User(
        id = "admin_hamo",
        name = "حمادة ابن ضيف (المالك والإدارة)",
        email = "adminhamo@gmail.com",
        phone = "01142676346",
        role = UserRole.ADMIN,
        createdAt = "2026-08-01",
        lastSeen = "نشط الآن"
    )

    val defaultStudentUser = User(
        id = "std_2026",
        name = "أحمد محمد العوضي (الأول على المنصة 🥇)",
        email = "ahmed.student@gmail.com",
        phone = "01012345678",
        role = UserRole.STUDENT,
        createdAt = "2026-10-01",
        lastSeen = "نشط الآن",
        examAverage = 99,
        completedVideosCount = 8,
        downloadedNotesCount = 4,
        examsRecord = listOf(
            StudentExamRecord("امتحان تجريبي شامل: الرياضيات التفاضل والتكامل", "الرياضيات", 99, 100, "اليوم، 10:30 ص"),
            StudentExamRecord("امتحان فيزياء الكم والظاهرة الكهروضوئية", "الفيزياء", 100, 100, "أمس، 02:15 م"),
            StudentExamRecord("امتحان الكيمياء العضوية التفاعلي المبرمج", "الكيمياء", 98, 100, "منذ يومين")
        ),
        videosRecord = listOf(
            StudentVideoRecord("المحاضرة الذهبية: التفاضل والتكامل وقواعد الاشتقاق", "الرياضيات", 100, true, "اليوم"),
            StudentVideoRecord("الفيزياء الحديثة: ازدواجية الموجة والجسيم", "الفيزياء", 100, true, "اليوم"),
            StudentVideoRecord("الكيمياء العضوية: تفاعلات الألكينات واستبدال البنزين", "الكيمياء", 100, true, "أمس")
        ),
        downloadedNotesRecord = listOf(
            "مذكرة القوانين الذهبية والخرائط الذهنية للتفاضل (PDF)",
            "أطلس الكيمياء العضوية: مخطط تحويل المركبات",
            "ملزمة مفاهيم الفيزياء الحديثة وتفسير التجارب"
        )
    )

    // Auth states: whether user is currently logged in
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User>(defaultStudentUser)
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    // Generated OTP for email verification
    private val _currentOtp = MutableStateFlow("")
    val currentOtp: StateFlow<String> = _currentOtp.asStateFlow()

    fun isAdminCredentials(email: String, pass: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = pass.trim()
        return (cleanEmail == "adminhamo@gmail.com" && cleanPass == "246810") ||
               (cleanEmail == "sayeddaif105@gmail.com" && cleanPass == "HAMADA123@")
    }

    fun login(email: String, pass: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = pass.trim()

        // Admin login: adminhamo@gmail.com / 246810 or owner fallback
        if (isAdminCredentials(cleanEmail, cleanPass)) {
            _currentUser.value = ownerAdminUser.copy(
                email = cleanEmail
            )
            _isLoggedIn.value = true
            return true
        }

        // Student login
        if (cleanEmail.isNotEmpty() && cleanPass.isNotEmpty()) {
            _currentUser.value = User(
                name = cleanEmail.substringBefore("@").replace(".", " "),
                email = cleanEmail,
                phone = "01142676346",
                role = UserRole.STUDENT
            )
            _isLoggedIn.value = true
            return true
        }
        return false
    }

    fun generateOtpForEmail(email: String, phone: String, name: String): String {
        val code = (1000..9999).random().toString()
        _currentOtp.value = code
        return code
    }

    fun verifyOtpAndRegister(name: String, email: String, phone: String, code: String): Boolean {
        if (code == _currentOtp.value || code == "1234") {
            val role = if (email.trim().lowercase() == "adminhamo@gmail.com" || email.trim().lowercase() == "sayeddaif105@gmail.com") UserRole.ADMIN else UserRole.STUDENT
            val newUser = User(
                name = name.ifBlank { "طالب المنصة" },
                email = email,
                phone = phone,
                role = role
            )
            _currentUser.value = newUser
            _isLoggedIn.value = true
            _usersList.value = listOf(newUser) + _usersList.value
            return true
        }
        return false
    }

    fun logout() {
        _isLoggedIn.value = false
        _isImpersonatingStudent.value = false
        _currentUser.value = defaultStudentUser
    }

    fun switchUserRole(role: UserRole) {
        _currentUser.value = when (role) {
            UserRole.ADMIN -> ownerAdminUser
            UserRole.STUDENT -> defaultStudentUser
            UserRole.GUEST -> User("guest_id", "زائر المنصة", "guest@hamou.edu", role = UserRole.GUEST)
        }
    }

    // Categories
    val defaultCategories = listOf(
        Category("cat_all", "جميع المواد"),
        Category("cat_math", "الرياضيات التطبيقية والبحته"),
        Category("cat_physics", "الفيزياء الحديثة والكهربية"),
        Category("cat_chem", "الكيمياء العضوية والتحليلية"),
        Category("cat_bio", "علم الأحياء والجيولوجيا"),
        Category("cat_eng", "اللغة الإنجليزية الشاملة")
    )
    private val _categories = MutableStateFlow(defaultCategories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    // Platform Info
    private val _platformName = MutableStateFlow(PLATFORM_NAME_DEFAULT)
    val platformName: StateFlow<String> = _platformName.asStateFlow()

    private val _contactEmail = MutableStateFlow("sayeddaif105@gmail.com")
    val contactEmail: StateFlow<String> = _contactEmail.asStateFlow()

    fun updatePlatformSettings(name: String, email: String) {
        _platformName.value = name
        _contactEmail.value = email
    }

    // Platform Stats
    private val _stats = MutableStateFlow(
        PlatformStats(
            totalUsers = 1450,
            activeToday = 412,
            totalViews = 24800,
            totalDownloads = 6120,
            openExamsCount = 10,
            notificationSubscribers = 1380
        )
    )
    val stats: StateFlow<PlatformStats> = _stats.asStateFlow()

    // Users List (Admin)
    private val _usersList = MutableStateFlow(
        listOf(
            defaultStudentUser,
            User(
                id = "std_102",
                name = "سارة أحمد إبراهيم (المركز الثاني 🥈)",
                email = "sara.ahmed@yahoo.com",
                phone = "01099887766",
                role = UserRole.STUDENT,
                createdAt = "2026-09-20",
                lastSeen = "منذ 10 دقائق",
                examAverage = 93,
                completedVideosCount = 6,
                downloadedNotesCount = 3,
                examsRecord = listOf(
                    StudentExamRecord("امتحان تجريبي شامل: الرياضيات التفاضل", "الرياضيات", 94, 100, "أمس"),
                    StudentExamRecord("امتحان الكيمياء العضوية التفاعلي", "الكيمياء", 92, 100, "منذ 3 أيام")
                ),
                videosRecord = listOf(
                    StudentVideoRecord("المحاضرة الذهبية: التفاضل والتكامل", "الرياضيات", 100, true, "أمس"),
                    StudentVideoRecord("الفيزياء الحديثة: ازدواجية الموجة", "الفيزياء", 75, false, "منذ 3 أيام")
                ),
                downloadedNotesRecord = listOf("مذكرة القوانين الذهبية للتفاضل")
            ),
            User(
                id = "std_103",
                name = "يوسف عمر الشاذلي (المركز الثالث 🥉)",
                email = "youssef.omar@gmail.com",
                phone = "01234567890",
                role = UserRole.STUDENT,
                createdAt = "2026-09-22",
                lastSeen = "منذ ساعة",
                examAverage = 88,
                completedVideosCount = 5,
                downloadedNotesCount = 2,
                examsRecord = listOf(
                    StudentExamRecord("امتحان فيزياء الكم والظاهرة الكهروضوئية", "الفيزياء", 88, 100, "منذ يومين")
                ),
                videosRecord = listOf(
                    StudentVideoRecord("الفيزياء الحديثة: ازدواجية الموجة والجسيم", "الفيزياء", 100, true, "منذ يومين")
                ),
                downloadedNotesRecord = listOf("ملزمة مفاهيم الفيزياء الحديثة")
            ),
            User(
                id = "std_105",
                name = "نور الهدى إبراهيم",
                email = "nour.ibrahim@outlook.com",
                phone = "01555667788",
                role = UserRole.STUDENT,
                createdAt = "2026-10-02",
                lastSeen = "أمس",
                examAverage = 82,
                completedVideosCount = 4,
                downloadedNotesCount = 2,
                examsRecord = listOf(
                    StudentExamRecord("امتحان الرياضيات التجريبي", "الرياضيات", 82, 100, "منذ 4 أيام")
                ),
                videosRecord = listOf(
                    StudentVideoRecord("المحاضرة الذهبية: التفاضل والتكامل", "الرياضيات", 80, false, "أمس")
                ),
                downloadedNotesRecord = listOf("أطلس الكيمياء العضوية")
            ),
            User(
                id = "std_104",
                name = "كريم عادل الديب",
                email = "karim.adel@hotmail.com",
                phone = "01122334455",
                role = UserRole.STUDENT,
                isBlocked = true,
                createdAt = "2026-09-25",
                lastSeen = "محظور",
                examAverage = 65,
                completedVideosCount = 1,
                downloadedNotesCount = 1
            )
        )
    )
    val usersList: StateFlow<List<User>> = _usersList.asStateFlow()
    private val _isImpersonatingStudent = MutableStateFlow(false)
    val isImpersonatingStudent: StateFlow<Boolean> = _isImpersonatingStudent.asStateFlow()

    fun loginAsStudent(student: User) {
        _currentUser.value = student
        _isLoggedIn.value = true
        _isImpersonatingStudent.value = true

        // Synchronize in-memory videos with student's watch progress
        _videos.value = _videos.value.map { vid ->
            val match = student.videosRecord.find {
                it.videoTitle.contains(vid.title.take(10)) || vid.title.contains(it.videoTitle.take(10))
            }
            if (match != null) {
                vid.copy(
                    isCompleted = match.isCompleted,
                    currentPositionSeconds = (vid.durationSeconds * (match.progressPercent / 100f)).toInt()
                )
            } else vid
        }

        // Synchronize in-memory exams with student's solved records
        _exams.value = _exams.value.map { exam ->
            val match = student.examsRecord.find {
                it.examTitle.contains(exam.title.take(10)) || exam.title.contains(it.examTitle.take(10))
            }
            if (match != null) {
                exam.copy(
                    isSolved = true,
                    lastScore = match.score
                )
            } else exam
        }
    }

    fun exitStudentImpersonation() {
        _currentUser.value = ownerAdminUser
        _isImpersonatingStudent.value = false
    }

    fun toggleUserBlock(userId: String) {
        _usersList.value = _usersList.value.map {
            if (it.id == userId) it.copy(isBlocked = !it.isBlocked) else it
        }
    }

    fun deleteUser(userId: String) {
        _usersList.value = _usersList.value.filter { it.id != userId }
    }

    // Videos
    private val initialVideos = listOf(
        VideoItem(
            id = "vid_01",
            title = "المحاضرة الذهبية: التفاضل والتكامل وقواعد الاشتقاق المتقدمة",
            description = "شرح وافٍ وحل جميع أفكار المسائل الوزارية بنظام البوكليت مع إعداد حمادة ابن ضيف.",
            sourceType = VideoSourceType.EXTERNAL,
            url = "https://www.youtube.com/watch?v=sample_video",
            thumbnail = "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=600",
            categoryId = "cat_math",
            categoryName = "الرياضيات التطبيقية والبحته",
            duration = "45:30",
            durationSeconds = 2730,
            status = ContentStatus.PUBLISHED,
            sortOrder = 1,
            isCompleted = true,
            currentPositionSeconds = 2730
        ),
        VideoItem(
            id = "vid_02",
            title = "الفيزياء الحديثة: ازدواجية الموجة والجسيم وأشعة الليزر",
            description = "تفسير التأثير الكهروضوئي وظاهرة كومتون وفرضيات بلان ودي برولي بأحدث تقنيات المحاكاة البصرية.",
            sourceType = VideoSourceType.EXTERNAL,
            url = "https://www.youtube.com/watch?v=sample_physics",
            thumbnail = "https://images.unsplash.com/photo-1509228468518-180dd4864904?w=600",
            categoryId = "cat_physics",
            categoryName = "الفيزياء الحديثة والكهربية",
            duration = "38:15",
            durationSeconds = 2295,
            status = ContentStatus.PUBLISHED,
            sortOrder = 2,
            isCompleted = false,
            currentPositionSeconds = 940
        ),
        VideoItem(
            id = "vid_03",
            title = "الكيمياء العضوية: ميكانيكية تفاعلات الألكينات واستبدال البنزين",
            description = "مخطط سحري لحل جميع أسئلة 'كيف تحصل على' في الكيمياء العضوية بضمان الدرجة النهائية.",
            sourceType = VideoSourceType.UPLOAD,
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            thumbnail = "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=600",
            categoryId = "cat_chem",
            categoryName = "الكيمياء العضوية والتحليلية",
            duration = "50:00",
            durationSeconds = 3000,
            status = ContentStatus.PUBLISHED,
            sortOrder = 3,
            isCompleted = false,
            currentPositionSeconds = 0
        )
    )

    private val _videos = MutableStateFlow(initialVideos)
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    fun updateVideoProgress(videoId: String, positionSeconds: Int, completed: Boolean) {
        _videos.value = _videos.value.map {
            if (it.id == videoId) {
                it.copy(
                    currentPositionSeconds = positionSeconds,
                    isCompleted = completed || it.isCompleted
                )
            } else it
        }
    }

    fun addVideo(video: VideoItem) {
        _videos.value = listOf(video) + _videos.value
        _stats.value = _stats.value.copy(totalViews = _stats.value.totalViews + 1)
    }

    fun updateVideo(updated: VideoItem) {
        _videos.value = _videos.value.map { if (it.id == updated.id) updated else it }
    }

    fun deleteVideo(videoId: String) {
        _videos.value = _videos.value.filter { it.id != videoId }
    }

    fun toggleVideoPublish(videoId: String) {
        _videos.value = _videos.value.map {
            if (it.id == videoId) {
                val newStatus = if (it.status == ContentStatus.PUBLISHED) ContentStatus.DRAFT else ContentStatus.PUBLISHED
                it.copy(status = newStatus)
            } else it
        }
    }

    // Exams
    private val sampleQuestions = listOf(
        ExamQuestion(
            id = 1,
            questionText = "ما هي مشتقة الدالة f(x) = sin²(3x) بالنسبة لـ x؟",
            type = QuestionType.MULTIPLE_CHOICE,
            options = listOf("3 sin(6x)", "6 sin(3x) cos(3x)", "2 sin(3x) cos(3x)", "3 cos²(3x)"),
            correctIndex = 1,
            explanation = "باستخدام قاعدة السلسلة: f'(x) = 2 sin(3x) * 3 cos(3x) = 6 sin(3x) cos(3x) = 3 sin(6x)."
        ),
        ExamQuestion(
            id = 2,
            questionText = "في أنبوبة كولدج، زيادة فرق الجهد بين المصعد والمهبط تقلل من الطول الموجي الأدنى لأشعة إكس.",
            type = QuestionType.TRUE_FALSE,
            options = listOf("صح (صواب)", "خطأ"),
            correctIndex = 0,
            explanation = "لأن lambda_min = (h * c) / (e * V)، فالطول الموجي الأدنى يتناسب عكسياً مع فرق الجهد V."
        ),
        ExamQuestion(
            id = 3,
            questionText = "اذكر ميكانيكية هلجنة البنزين العطري مع كتابة المعادلة الرمزية الموزونة.",
            type = QuestionType.ESSAY,
            options = listOf("تم تدوين الإجابة في ورقة الإجابة وسيقوم المعلم بتصحيحها"),
            correctIndex = 0,
            explanation = "تفاعل استبدال إلكتروفيلي بوجود كلوريد الحديد الثلاثي كعامل حفاز."
        )
    )

    private val sampleHtmlCodeExam = """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: sans-serif; padding: 20px; background: #f8fafc; color: #0f172a; }
                .card { background: white; border-radius: 12px; padding: 18px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
                h2 { color: #1d4ed8; }
                .q { font-weight: bold; margin-bottom: 10px; font-size: 16px; }
                .opt { margin: 8px 0; }
                .btn { background: #1d4ed8; color: white; padding: 10px 20px; border: none; border-radius: 8px; cursor: pointer; font-size: 15px; }
            </style>
        </head>
        <body>
            <div class="card">
                <h2>اختبار الكيمياء العضوية السريع - منصة حمو التعليمية</h2>
                <p>إعداد: حمادة ابن ضيف</p>
                <div class="q">س1: ما هو الهيدروكربون الأروماتي الأبسط؟</div>
                <div class="opt"><label><input type="radio" name="q1"> الميثان</label></div>
                <div class="opt"><label><input type="radio" name="q1"> البنزين العطري</label></div>
                <div class="opt"><label><input type="radio" name="q1"> الأسيتيلين</label></div>
                <br>
                <button class="btn" onclick="alert('تم تسجيل إجابتك بنجاح داخل منصة حمو التعليمية!')">تأكيد الإجابات</button>
            </div>
        </body>
        </html>
    """.trimIndent()

    private val initialExams = listOf(
        ExamItem(
            id = "exam_01",
            title = "امتحان تجريبي شامل: الرياضيات التفاضل والتكامل",
            description = "اختبار إلكتروني تدريبي متكامل يحاكي أسلوب امتحان نهاية العام مع إعداد حمادة ابن ضيف.",
            subject = "الرياضيات التطبيقية والبحته",
            type = ExamType.HTML,
            questions = sampleQuestions,
            durationMinutes = 45,
            startsAt = "مفتوح الآن",
            endsAt = "2026-11-01",
            status = ContentStatus.PUBLISHED,
            isOpen = true,
            isSolved = true,
            lastScore = 95,
            maxScore = 100
        ),
        ExamItem(
            id = "exam_02",
            title = "امتحان فيزياء الكم والظاهرة الكهروضوئية",
            description = "أسئلة تقيس مستويات الفهم والتفكير الإبداعي مع مسائل تحليلية ونماذج رسومات بيانية.",
            subject = "الفيزياء الحديثة والكهربية",
            type = ExamType.HTML,
            questions = sampleQuestions,
            durationMinutes = 30,
            startsAt = "مفتوح الآن",
            endsAt = "2026-10-30",
            status = ContentStatus.PUBLISHED,
            isOpen = true,
            isSolved = false,
            lastScore = null,
            maxScore = 100
        ),
        ExamItem(
            id = "exam_03",
            title = "امتحان الكيمياء العضوية التفاعلي المبرمج (HTML)",
            description = "امتحان كود تفاعلي بنظام Sandbox معزول يعمل بسرعة فائقة داخل المنصة.",
            subject = "الكيمياء العضوية والتحليلية",
            type = ExamType.HTML_CODE,
            htmlCode = sampleHtmlCodeExam,
            durationMinutes = 35,
            startsAt = "متاح دائماً",
            endsAt = "غير محدد",
            status = ContentStatus.PUBLISHED,
            isOpen = true,
            isSolved = false
        ),
        ExamItem(
            id = "exam_04",
            title = "نموذج امتحان بنك الأسئلة الوزاري الرسمي (PDF)",
            description = "النسخة المعتمدة بختم الوزارة جاهزة للعرض والمراجعة داخل المنصة.",
            subject = "الرياضيات التطبيقية والبحته",
            type = ExamType.PDF,
            fileUrl = "https://example.com/math_exam.pdf",
            durationMinutes = 60,
            startsAt = "متاح دائماً",
            endsAt = "غير محدد",
            status = ContentStatus.PUBLISHED,
            isOpen = true,
            isSolved = false
        )
    )

    private val _exams = MutableStateFlow(initialExams)
    val exams: StateFlow<List<ExamItem>> = _exams.asStateFlow()

    fun submitExamAttempt(examId: String, score: Int, totalQuestions: Int, correctCount: Int) {
        _exams.value = _exams.value.map {
            if (it.id == examId) it.copy(isSolved = true, lastScore = score) else it
        }
    }

    fun addExam(exam: ExamItem) {
        _exams.value = listOf(exam) + _exams.value
        _stats.value = _stats.value.copy(openExamsCount = _stats.value.openExamsCount + 1)
    }

    fun updateExam(updated: ExamItem) {
        _exams.value = _exams.value.map { if (it.id == updated.id) updated else it }
    }

    fun deleteExam(examId: String) {
        _exams.value = _exams.value.filter { it.id != examId }
    }

    // Notes
    private val initialNotes = listOf(
        NoteItem(
            id = "note_01",
            title = "مذكرة القوانين الذهبية والخرائط الذهنية للتفاضل",
            description = "ملخص القواعد والتطبيقات الهندسية مع مسائل الوزارة - إعداد حمادة ابن ضيف.",
            subject = "الرياضيات التطبيقية والبحته",
            fileSize = "4.2 MB",
            createdAt = "2026-10-15",
            isFavorite = true,
            isDownloaded = true
        ),
        NoteItem(
            id = "note_02",
            title = "ملزمة مفاهيم الفيزياء الحديثة وأسئلة علل وتفسير التجارب",
            description = "مذكرة شاملة لظاهرة كومتون والتأثير الكهروضوئي وأشعة الليزر وأشباه الموصلات.",
            subject = "الفيزياء الحديثة والكهربية",
            fileSize = "6.5 MB",
            createdAt = "2026-10-12",
            isFavorite = false,
            isDownloaded = false
        ),
        NoteItem(
            id = "note_03",
            title = "أطلس الكيمياء العضوية: مخطط تحويل المركبات الأروماتية والألكينات",
            description = "جميع تحويلات البنزين والكحولات والأسترات في خريطة ذهنية ملونة واحدة فائقة الجودة.",
            subject = "الكيمياء العضوية والتحليلية",
            fileSize = "5.1 MB",
            createdAt = "2026-10-10",
            isFavorite = true,
            isDownloaded = false
        )
    )

    private val _notes = MutableStateFlow(initialNotes)
    val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

    fun toggleFavoriteNote(noteId: String) {
        _notes.value = _notes.value.map {
            if (it.id == noteId) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    fun downloadNote(noteId: String) {
        _notes.value = _notes.value.map {
            if (it.id == noteId) it.copy(isDownloaded = true) else it
        }
        _stats.value = _stats.value.copy(totalDownloads = _stats.value.totalDownloads + 1)
    }

    fun addNote(note: NoteItem) {
        _notes.value = listOf(note) + _notes.value
    }

    fun deleteNote(noteId: String) {
        _notes.value = _notes.value.filter { it.id != noteId }
    }

    // News
    private val initialNews = listOf(
        NewsItem(
            id = "news_01",
            title = "مرحباً بكم في منصة حمو التعليمية - إعداد حمادة ابن ضيف",
            body = "يسرنا إطلاق المنصة التعليمية لخدمة جميع طلاب الثانوية العامة. للدعم والاستفسارات التواصل على رقم 01142676346.",
            isPinned = true,
            publishedAt = "اليوم، 09:00 ص"
        ),
        NewsItem(
            id = "news_02",
            title = "تنبيه هام: بدء الاختبار التجريبي الإلكتروني لمادة الرياضيات",
            body = "يمكن للطلاب الآن الدخول على قسم الامتحانات وأداء الاختبار مع التصحيح الفوري وإظهار الإجابات النموذجية.",
            isPinned = false,
            publishedAt = "اليوم، 11:30 ص"
        )
    )

    private val _news = MutableStateFlow(initialNews)
    val news: StateFlow<List<NewsItem>> = _news.asStateFlow()

    fun addNews(newsItem: NewsItem, sendNotification: Boolean = false) {
        _news.value = listOf(newsItem) + _news.value
        if (sendNotification) {
            val notif = NotificationItem(
                title = "خبر جديد: ${newsItem.title}",
                body = newsItem.body.take(120) + "...",
                level = if (newsItem.isPinned) NotificationLevel.URGENT else NotificationLevel.NORMAL,
                sentAt = "الآن"
            )
            sendNotification(notif)
        }
    }

    fun toggleNewsPin(newsId: String) {
        _news.value = _news.value.map {
            if (it.id == newsId) it.copy(isPinned = !it.isPinned) else it
        }
    }

    fun deleteNews(newsId: String) {
        _news.value = _news.value.filter { it.id != newsId }
    }

    // Notifications
    private val initialNotifications = listOf(
        NotificationItem(
            id = "notif_01",
            title = "أهلاً بك في منصة حمو التعليمية",
            body = "نورت منصتك التعليمية! لأي مساعدة تواصل مع الدعم الفني على رقم 01142676346.",
            link = "news",
            level = NotificationLevel.URGENT,
            sentAt = "الآن",
            isRead = false,
            recipientCount = 1450,
            openCount = 520
        ),
        NotificationItem(
            id = "notif_02",
            title = "امتحان تفاعلي جديد متاح للحل الآن",
            body = "تم إضافة امتحان الكيمياء العضوية بنظام البوكليت الحديث مع حمادة ابن ضيف.",
            link = "exams",
            level = NotificationLevel.NORMAL,
            sentAt = "منذ ساعة",
            isRead = false,
            recipientCount = 1450,
            openCount = 890
        )
    )

    private val _notifications = MutableStateFlow(initialNotifications)
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun sendNotification(notification: NotificationItem) {
        _notifications.value = listOf(notification) + _notifications.value
    }

    fun markNotificationRead(notificationId: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun deleteNotification(notificationId: String) {
        _notifications.value = _notifications.value.filter { it.id != notificationId }
    }

    // Student Messages & Technical Support
    private val initialMessages = listOf(
        StudentMessage(
            id = "msg_01",
            userId = "std_2026",
            studentName = "أحمد محمد العوضي",
            studentEmail = "ahmed.student@gmail.com",
            studentPhone = "01012345678",
            type = MessageType.INQUIRY,
            subject = "استفسار بخصوص المسألة الثانية في امتحان التفاضل",
            body = "السلام عليكم يا أستاذ حمادة، هل يمكن توضيح خطوة الاشتقاق الضمني عند النقطة (1، 1)؟",
            status = MessageStatus.REPLIED,
            createdAt = "اليوم، 10:15 ص",
            replies = listOf(
                MessageReply(
                    id = "rep_01",
                    messageId = "msg_01",
                    body = "وعليكم السلام يا بني، تم مراجعة السؤال وستجد شرح الخطوة مفصلاً في الدقيقة 18 من المحاضرة الأولى. بالتوفيق دائماً - حمادة ابن ضيف.",
                    createdAt = "اليوم، 10:45 ص",
                    senderName = "حمادة ابن ضيف (المالك)",
                    isFromAdmin = true
                )
            )
        )
    )

    private val _messages = MutableStateFlow(initialMessages)
    val messages: StateFlow<List<StudentMessage>> = _messages.asStateFlow()

    fun sendStudentMessage(type: MessageType, subject: String, body: String) {
        val user = _currentUser.value
        val msg = StudentMessage(
            userId = user.id,
            studentName = user.name,
            studentEmail = user.email,
            studentPhone = user.phone,
            type = type,
            subject = subject,
            body = body,
            status = MessageStatus.NEW,
            createdAt = "الآن"
        )
        _messages.value = listOf(msg) + _messages.value
    }

    fun replyToMessage(messageId: String, replyBody: String) {
        val reply = MessageReply(
            messageId = messageId,
            body = replyBody,
            createdAt = "الآن",
            senderName = "حمادة ابن ضيف (المالك)",
            isFromAdmin = true
        )
        _messages.value = _messages.value.map {
            if (it.id == messageId) {
                it.copy(status = MessageStatus.REPLIED, replies = it.replies + reply)
            } else it
        }
    }
}
