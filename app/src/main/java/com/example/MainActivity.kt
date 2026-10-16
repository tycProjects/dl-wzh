package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EduRepository
import com.example.data.ExamItem
import com.example.data.NewsItem
import com.example.data.NoteItem
import com.example.data.NotificationItem
import com.example.data.UserRole
import com.example.data.VideoItem
import com.example.ui.components.EduBottomNavigation
import com.example.ui.components.EduTopBar
import com.example.ui.components.NavTab
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExamViewerScreen
import com.example.ui.screens.ExamsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.NewsScreen
import com.example.ui.screens.NoteViewerScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.EduPlatformTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EduPlatformTheme {
                EduPlatformApp()
            }
        }
    }
}

sealed class SubScreen {
    data class VideoPlayer(val videoId: String) : SubScreen()
    data class ExamViewer(val examId: String) : SubScreen()
    data class NoteViewer(val noteId: String) : SubScreen()
    object Notifications : SubScreen()
    object Messages : SubScreen()
    object AdminDashboard : SubScreen()
    object Profile : SubScreen()
}

@Composable
fun EduPlatformApp() {
    val isLoggedIn by EduRepository.isLoggedIn.collectAsStateWithLifecycle()
    val currentUser by EduRepository.currentUser.collectAsStateWithLifecycle()
    val videos by EduRepository.videos.collectAsStateWithLifecycle()
    val exams by EduRepository.exams.collectAsStateWithLifecycle()
    val notes by EduRepository.notes.collectAsStateWithLifecycle()
    val news by EduRepository.news.collectAsStateWithLifecycle()
    val notifications by EduRepository.notifications.collectAsStateWithLifecycle()
    val messages by EduRepository.messages.collectAsStateWithLifecycle()
    val stats by EduRepository.stats.collectAsStateWithLifecycle()
    val categories by EduRepository.categories.collectAsStateWithLifecycle()
    val platformName by EduRepository.platformName.collectAsStateWithLifecycle()
    val contactEmail by EduRepository.contactEmail.collectAsStateWithLifecycle()

    val isImpersonating by EduRepository.isImpersonatingStudent.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var currentSubScreen by remember { mutableStateOf<SubScreen?>(null) }

    val unreadNotificationsCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    // If user is not logged in, show AuthScreen (Mandatory login / sign up before entering)
    if (!isLoggedIn) {
        AuthScreen(
            onLoginSuccess = {
                // Check if user is admin
                if (currentUser.role == UserRole.ADMIN) {
                    currentSubScreen = SubScreen.AdminDashboard
                } else {
                    currentTab = NavTab.HOME
                    currentSubScreen = null
                }
            }
        )
        return
    }

    // Hardware back navigation handler
    BackHandler(enabled = currentSubScreen != null || currentTab != NavTab.HOME) {
        if (currentSubScreen != null) {
            currentSubScreen = null
        } else if (currentTab != NavTab.HOME) {
            currentTab = NavTab.HOME
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            Column {
                if (isImpersonating) {
                    Surface(
                        color = BrandDanger,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تتصفح الآن كطالب: ${currentUser.name} 🏆",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Button(
                                onClick = {
                                    EduRepository.exitStudentImpersonation()
                                    currentSubScreen = SubScreen.AdminDashboard
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = "العودة للوحة الإدارة 🔒",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandDanger
                                )
                            }
                        }
                    }
                }
                if (currentSubScreen == null) {
                    EduTopBar(
                        title = when (currentTab) {
                            NavTab.HOME -> EduRepository.PLATFORM_NAME_DEFAULT
                            NavTab.VIDEOS -> "الفيديوهات والمحاضرات"
                            NavTab.EXAMS -> "الامتحانات والاختبارات"
                            NavTab.NOTES -> "الملازم والمذكرات"
                            NavTab.NEWS -> "الأخبار والتنبيهات"
                        },
                        unreadNotificationsCount = unreadNotificationsCount,
                        currentRole = currentUser.role,
                        canGoBack = currentTab != NavTab.HOME,
                        onBackClick = { currentTab = NavTab.HOME },
                        onNotificationsClick = { currentSubScreen = SubScreen.Notifications },
                        onAdminClick = {
                            if (currentUser.role == UserRole.ADMIN) {
                                currentSubScreen = SubScreen.AdminDashboard
                            }
                        },
                        onProfileClick = { currentSubScreen = SubScreen.Profile }
                    )
                }
            }
        },
        bottomBar = {
            if (currentSubScreen == null) {
                EduBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val sub = currentSubScreen) {
                is SubScreen.VideoPlayer -> {
                    val video = videos.find { it.id == sub.videoId } ?: videos.first()
                    VideoPlayerScreen(
                        video = video,
                        onBack = { currentSubScreen = null },
                        onProgressUpdate = { pos, completed ->
                            EduRepository.updateVideoProgress(video.id, pos, completed)
                        }
                    )
                }
                is SubScreen.ExamViewer -> {
                    val exam = exams.find { it.id == sub.examId } ?: exams.first()
                    ExamViewerScreen(
                        exam = exam,
                        onBack = { currentSubScreen = null },
                        onSubmitAttempt = { score, total, correct ->
                            EduRepository.submitExamAttempt(exam.id, score, total, correct)
                        }
                    )
                }
                is SubScreen.NoteViewer -> {
                    val note = notes.find { it.id == sub.noteId } ?: notes.first()
                    NoteViewerScreen(
                        note = note,
                        onBack = { currentSubScreen = null },
                        onToggleFavorite = { EduRepository.toggleFavoriteNote(note.id) },
                        onDownload = { EduRepository.downloadNote(note.id) }
                    )
                }
                is SubScreen.Notifications -> {
                    NotificationsScreen(
                        notifications = notifications,
                        onBack = { currentSubScreen = null },
                        onNotificationClick = { item ->
                            EduRepository.markNotificationRead(item.id)
                            when (item.link) {
                                "exams" -> {
                                    currentSubScreen = null
                                    currentTab = NavTab.EXAMS
                                }
                                "notes" -> {
                                    currentSubScreen = null
                                    currentTab = NavTab.NOTES
                                }
                                "news" -> {
                                    currentSubScreen = null
                                    currentTab = NavTab.NEWS
                                }
                                "messages" -> {
                                    currentSubScreen = SubScreen.Messages
                                }
                            }
                        },
                        onMarkAllRead = { EduRepository.markAllNotificationsRead() },
                        onDeleteNotification = { EduRepository.deleteNotification(it) }
                    )
                }
                is SubScreen.Messages -> {
                    MessagesScreen(
                        messages = messages,
                        onBack = { currentSubScreen = null },
                        onSendMessage = { type, subject, body ->
                            EduRepository.sendStudentMessage(type, subject, body)
                        }
                    )
                }
                is SubScreen.AdminDashboard -> {
                    AdminDashboardScreen(
                        stats = stats,
                        users = EduRepository.usersList.collectAsStateWithLifecycle().value,
                        messages = messages,
                        notifications = notifications,
                        platformName = platformName,
                        contactEmail = contactEmail,
                        onBack = { currentSubScreen = null },
                        onToggleUserBlock = { EduRepository.toggleUserBlock(it) },
                        onDeleteUser = { EduRepository.deleteUser(it) },
                        onSendNotification = { EduRepository.sendNotification(it) },
                        onReplyMessage = { msgId, reply -> EduRepository.replyToMessage(msgId, reply) },
                        onUpdateSettings = { name, email -> EduRepository.updatePlatformSettings(name, email) },
                        onEnterAsStudent = { student ->
                            EduRepository.loginAsStudent(student)
                            currentSubScreen = null
                            currentTab = NavTab.HOME
                        }
                    )
                }
                is SubScreen.Profile -> {
                    val completedCount = videos.count { it.isCompleted }
                    val solvedCount = exams.count { it.isSolved }
                    val favCount = notes.count { it.isFavorite }

                    ProfileScreen(
                        user = currentUser,
                        completedVideosCount = completedCount,
                        solvedExamsCount = solvedCount,
                        favoriteNotesCount = favCount,
                        onBack = { currentSubScreen = null },
                        onSwitchRole = { newRole ->
                            EduRepository.switchUserRole(newRole)
                        },
                        onOpenAdminDashboard = {
                            currentSubScreen = SubScreen.AdminDashboard
                        },
                        onLogout = {
                            EduRepository.logout()
                            currentSubScreen = null
                        }
                    )
                }
                null -> {
                    when (currentTab) {
                        NavTab.HOME -> {
                            HomeScreen(
                                currentUser = currentUser,
                                videos = videos,
                                exams = exams,
                                notes = notes,
                                news = news,
                                onVideoClick = { currentSubScreen = SubScreen.VideoPlayer(it) },
                                onExamClick = { currentSubScreen = SubScreen.ExamViewer(it) },
                                onNoteClick = { currentSubScreen = SubScreen.NoteViewer(it) },
                                onNewsClick = { currentTab = NavTab.NEWS },
                                onSeeAllVideos = { currentTab = NavTab.VIDEOS },
                                onSeeAllExams = { currentTab = NavTab.EXAMS },
                                onSeeAllNotes = { currentTab = NavTab.NOTES },
                                onSeeAllNews = { currentTab = NavTab.NEWS },
                                onContactAdminClick = { currentSubScreen = SubScreen.Messages }
                            )
                        }
                        NavTab.VIDEOS -> {
                            VideosScreen(
                                videos = videos,
                                categories = categories,
                                userRole = currentUser.role,
                                onVideoClick = { currentSubScreen = SubScreen.VideoPlayer(it) },
                                onAddVideo = { EduRepository.addVideo(it) },
                                onUpdateVideo = { EduRepository.updateVideo(it) },
                                onDeleteVideo = { EduRepository.deleteVideo(it) },
                                onTogglePublish = { EduRepository.toggleVideoPublish(it) }
                            )
                        }
                        NavTab.EXAMS -> {
                            ExamsScreen(
                                exams = exams,
                                categories = categories,
                                userRole = currentUser.role,
                                onExamClick = { currentSubScreen = SubScreen.ExamViewer(it) },
                                onAddExam = { EduRepository.addExam(it) },
                                onUpdateExam = { EduRepository.updateExam(it) },
                                onDeleteExam = { EduRepository.deleteExam(it) }
                            )
                        }
                        NavTab.NOTES -> {
                            NotesScreen(
                                notes = notes,
                                categories = categories,
                                userRole = currentUser.role,
                                onNoteClick = { currentSubScreen = SubScreen.NoteViewer(it) },
                                onToggleFavorite = { EduRepository.toggleFavoriteNote(it) },
                                onDownloadNote = { EduRepository.downloadNote(it) },
                                onAddNote = { EduRepository.addNote(it) },
                                onDeleteNote = { EduRepository.deleteNote(it) }
                            )
                        }
                        NavTab.NEWS -> {
                            NewsScreen(
                                newsList = news,
                                userRole = currentUser.role,
                                onAddNews = { item, sendNotif -> EduRepository.addNews(item, sendNotif) },
                                onTogglePin = { EduRepository.toggleNewsPin(it) },
                                onDeleteNews = { EduRepository.deleteNews(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
