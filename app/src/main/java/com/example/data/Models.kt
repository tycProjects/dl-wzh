package com.example.data

import java.util.UUID

enum class UserRole(val titleAr: String) {
    GUEST("زائر"),
    STUDENT("طالب"),
    ADMIN("المالك والإدارة (حمادة ابن ضيف)")
}

data class StudentExamRecord(
    val examTitle: String,
    val subject: String,
    val score: Int,
    val maxScore: Int = 100,
    val solvedAt: String = "اليوم"
)

data class StudentVideoRecord(
    val videoTitle: String,
    val subject: String,
    val progressPercent: Int,
    val isCompleted: Boolean,
    val lastWatched: String = "اليوم"
)

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val phone: String = "01142676346",
    val role: UserRole = UserRole.STUDENT,
    val isBlocked: Boolean = false,
    val createdAt: String = "2026-10-15",
    val lastSeen: String = "الآن",
    val examAverage: Int = 85,
    val completedVideosCount: Int = 3,
    val downloadedNotesCount: Int = 2,
    val examsRecord: List<StudentExamRecord> = emptyList(),
    val videosRecord: List<StudentVideoRecord> = emptyList(),
    val downloadedNotesRecord: List<String> = emptyList()
)

enum class CategoryType {
    VIDEO, EXAM, PDF, ALL
}

data class Category(
    val id: String,
    val name: String,
    val type: CategoryType = CategoryType.ALL
)

enum class VideoSourceType(val labelAr: String) {
    UPLOAD("من الهاتف / ملف محلي"),
    EXTERNAL("رابط خارجي (YouTube / Drive / Vimeo)")
}

enum class ContentStatus(val labelAr: String) {
    PUBLISHED("منشور"),
    DRAFT("مسودة")
}

data class VideoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val sourceType: VideoSourceType = VideoSourceType.EXTERNAL,
    val url: String = "https://www.youtube.com/watch?v=sample",
    val thumbnail: String = "",
    val categoryId: String,
    val categoryName: String,
    val duration: String, // e.g. "45:20"
    val durationSeconds: Int = 2720,
    val status: ContentStatus = ContentStatus.PUBLISHED,
    val sortOrder: Int = 1,
    val isCompleted: Boolean = false,
    val currentPositionSeconds: Int = 0
)

enum class ExamType(val labelAr: String) {
    PDF("امتحان PDF"),
    HTML("امتحان إلكتروني تفاعلي"),
    HTML_CODE("امتحان بكود HTML معزول")
}

enum class QuestionType(val labelAr: String) {
    MULTIPLE_CHOICE("اختيار من متعدد"),
    TRUE_FALSE("صح أو خطأ"),
    ESSAY("سؤال مقالي / كتابي")
}

data class ExamQuestion(
    val id: Int,
    val questionText: String,
    val type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val imageUrl: String? = null,
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = ""
)

data class ExamItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val subject: String,
    val type: ExamType = ExamType.HTML,
    val fileUrl: String = "",
    val htmlCode: String = "",
    val questions: List<ExamQuestion> = emptyList(),
    val durationMinutes: Int = 30,
    val startsAt: String = "متاح الآن",
    val endsAt: String = "متاح دائماً",
    val status: ContentStatus = ContentStatus.PUBLISHED,
    val isOpen: Boolean = true,
    val isSolved: Boolean = false,
    val lastScore: Int? = null,
    val maxScore: Int = 100
)

data class ExamAttempt(
    val id: String = UUID.randomUUID().toString(),
    val examId: String,
    val examTitle: String,
    val userId: String,
    val openedAt: String,
    val score: Int,
    val totalQuestions: Int,
    val correctAnswersCount: Int
)

data class NewsItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val coverImage: String = "",
    val isPinned: Boolean = false,
    val publishedAt: String = "اليوم"
)

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val subject: String,
    val fileUrl: String = "https://example.com/handout.pdf",
    val fileSize: String = "4.2 MB",
    val fileSizeBytes: Long = 4404019L,
    val categoryId: String = "1",
    val createdAt: String = "2026-10-15",
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false
)

enum class NotificationLevel(val labelAr: String) {
    NORMAL("عادي"),
    URGENT("عاجل ومهم")
}

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val link: String = "",
    val level: NotificationLevel = NotificationLevel.NORMAL,
    val target: String = "الجميع",
    val scheduledAt: String = "فوري",
    val sentAt: String = "منذ قليل",
    val isRead: Boolean = false,
    val recipientCount: Int = 450,
    val openCount: Int = 312
)

enum class MessageType(val labelAr: String) {
    INQUIRY("استفسار دراسي"),
    COMPLAINT("شكوى"),
    SUGGESTION("اقتراح تطوير"),
    TECHNICAL("مشكلة تقنية")
}

enum class MessageStatus(val labelAr: String) {
    NEW("جديدة"),
    READ("مقروءة"),
    REPLIED("تم الرد"),
    ARCHIVED("مؤرشفة")
}

data class MessageReply(
    val id: String = UUID.randomUUID().toString(),
    val messageId: String,
    val body: String,
    val createdAt: String,
    val senderName: String,
    val isFromAdmin: Boolean = true
)

data class StudentMessage(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val studentName: String,
    val studentEmail: String,
    val studentPhone: String = "01142676346",
    val type: MessageType = MessageType.INQUIRY,
    val subject: String,
    val body: String,
    val status: MessageStatus = MessageStatus.NEW,
    val createdAt: String = "اليوم، 10:30 ص",
    val replies: List<MessageReply> = emptyList()
)

data class PlatformStats(
    val totalUsers: Int = 1450,
    val activeToday: Int = 412,
    val totalViews: Int = 22400,
    val totalDownloads: Int = 5890,
    val openExamsCount: Int = 12,
    val notificationSubscribers: Int = 1380
)
