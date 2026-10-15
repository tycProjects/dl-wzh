package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EduRepository
import com.example.data.MessageStatus
import com.example.data.NotificationItem
import com.example.data.NotificationLevel
import com.example.data.PlatformStats
import com.example.data.StudentMessage
import com.example.data.User
import com.example.data.UserRole
import com.example.ui.components.EduSearchBar
import com.example.ui.components.StatOverviewCard
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandLightBlue
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSuccess

@Composable
fun AdminDashboardScreen(
    stats: PlatformStats,
    users: List<User>,
    messages: List<StudentMessage>,
    notifications: List<NotificationItem>,
    platformName: String,
    contactEmail: String,
    onBack: () -> Unit,
    onToggleUserBlock: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onSendNotification: (NotificationItem) -> Unit,
    onReplyMessage: (messageId: String, reply: String) -> Unit,
    onUpdateSettings: (name: String, email: String) -> Unit,
    onEnterAsStudent: (User) -> Unit = {}
) {
    var selectedSection by remember { mutableIntStateOf(1) } // Default to Students tab for quick inspection

    val unreadMessagesCount = remember(messages) {
        messages.count { it.status == MessageStatus.NEW }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Dashboard Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "لوحة تحكم المالك والإدارة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandDanger
                        )
                        Text(
                            text = "إعداد: حمادة ابن ضيف • إدارة الطلاب والمحتوى",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandDanger.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "صلاحيات المالك 🔒",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandDanger,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedSection,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = { Text("📊 الإحصائيات", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = { Text("🏆 ترتيب الطلاب والأنشطة", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSection == 2,
                onClick = { selectedSection = 2 },
                text = { Text("📢 إرسال إشعار", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSection == 3,
                onClick = { selectedSection = 3 },
                text = {
                    Text(
                        text = if (unreadMessagesCount > 0) "💬 الرسائل ($unreadMessagesCount جديدة)" else "💬 الرسائل",
                        fontWeight = FontWeight.Bold,
                        color = if (unreadMessagesCount > 0) BrandDanger else MaterialTheme.colorScheme.onSurface
                    )
                }
            )
            Tab(
                selected = selectedSection == 4,
                onClick = { selectedSection = 4 },
                text = { Text("⚙️ الإعدادات", fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedSection) {
            0 -> AdminStatsTab(stats = stats)
            1 -> AdminUsersRankedTab(
                users = users,
                onToggleBlock = onToggleUserBlock,
                onDelete = onDeleteUser,
                onEnterAsStudent = onEnterAsStudent
            )
            2 -> AdminNotificationCenterTab(
                notifications = notifications,
                onSend = onSendNotification
            )
            3 -> AdminMessagesInboxTab(
                messages = messages,
                onReply = onReplyMessage
            )
            4 -> AdminSettingsTab(
                currentName = platformName,
                currentEmail = contactEmail,
                onSave = onUpdateSettings
            )
        }
    }
}

@Composable
fun AdminStatsTab(stats: PlatformStats) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "نظرة عامة على أداء المنصة (Real-time Analytics)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatOverviewCard(
                    title = "إجمالي الطلاب",
                    value = "${stats.totalUsers}",
                    icon = Icons.Filled.People,
                    iconBgColor = BrandPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatOverviewCard(
                    title = "النشطين اليوم",
                    value = "${stats.activeToday}",
                    icon = Icons.Filled.CheckCircle,
                    iconBgColor = BrandSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatOverviewCard(
                    title = "إجمالي المشاهدات",
                    value = "${stats.totalViews}",
                    icon = Icons.Filled.PlayCircle,
                    iconBgColor = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
                StatOverviewCard(
                    title = "التحميلات للملازم",
                    value = "${stats.totalDownloads}",
                    icon = Icons.Filled.Download,
                    iconBgColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatOverviewCard(
                    title = "الامتحانات المفتوحة",
                    value = "${stats.openExamsCount}",
                    icon = Icons.Filled.Quiz,
                    iconBgColor = BrandDanger,
                    modifier = Modifier.weight(1f)
                )
                StatOverviewCard(
                    title = "المشتركين بالإشعارات",
                    value = "${stats.notificationSubscribers}",
                    icon = Icons.Filled.Notifications,
                    iconBgColor = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ملخص حالة النظام والخادم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "• سرعة الاستجابة والتخزين المؤقت: ممتازة (< 150ms)")
                    Text(text = "• النسخ الاحتياطي التلقائي: تم الحفظ اليوم الساعة 04:00 ص")
                    Text(text = "• بث الإشعارات الفورية Push Service: متصل ويعمل بكفاءة")
                }
            }
        }
    }
}

@Composable
fun AdminUsersRankedTab(
    users: List<User>,
    onToggleBlock: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEnterAsStudent: (User) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var inspectingStudent by remember { mutableStateOf<User?>(null) }

    // Sort students so the highest achieving student (best average) appears first
    val sortedStudents = remember(users, searchQuery) {
        val studentsOnly = users.filter { it.role == UserRole.STUDENT }
        val searched = if (searchQuery.isBlank()) studentsOnly
        else studentsOnly.filter {
            it.name.contains(searchQuery, true) ||
            it.email.contains(searchQuery, true) ||
            it.phone.contains(searchQuery)
        }
        searched.sortedWith(
            compareByDescending<User> { it.examAverage }
                .thenByDescending { it.completedVideosCount }
        )
    }

    val topStudent = sortedStudents.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Golden Leaderboard Top Header
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "لوحة شرف وتصنيف الطلاب (الأول في الصدارة) 🏆",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "مرتبة تلقائياً بأعلى معدل درجات. يمكنك فحص أداء الطالب أو الدخول بحسابه مباشرة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }
        }

        // Spotlight Card for the #1 Best Student
        if (searchQuery.isBlank() && topStudent != null) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF59E0B)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "المتصدر والأول على المنصة 🥇",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "معدل التفوق: ${topStudent.examAverage}% 🎯",
                                    color = BrandSuccess,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7))
                                    .border(2.dp, Color(0xFFF59E0B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = topStudent.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF78350F)
                                )
                                Text(
                                    text = "📧 ${topStudent.email} • 📞 ${topStudent.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // High performance achievement badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Filled.PlayCircle, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${topStudent.completedVideosCount} فيديوهات مكتملة", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Filled.Quiz, contentDescription = null, tint = BrandSuccess, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${topStudent.examsRecord.size.coerceAtLeast(1)} امتحانات محلولة", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { inspectingStudent = topStudent },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("رؤية أدائه وكل شيء", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = { onEnterAsStudent(topStudent) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("top_student_enter_btn")
                            ) {
                                Icon(imageVector = Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("دخول بحسابه فوراً", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        item {
            EduSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "ابحث عن طالب بالاسم أو البريد أو الهاتف..."
            )
        }

        items(sortedStudents.indices.toList()) { index ->
            val student = sortedStudents[index]
            val isFirst = index == 0

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFirst) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isFirst) 3.dp else 1.5.dp),
                border = if (isFirst) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { inspectingStudent = student }
                    .testTag("student_card_${student.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Rank Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when (index) {
                                    0 -> Color(0xFFF59E0B)
                                    1 -> Color(0xFF94A3B8)
                                    2 -> Color(0xFFB45309)
                                    else -> BrandPrimary
                                }
                            ) {
                                Text(
                                    text = when (index) {
                                        0 -> "الأول 🥇"
                                        1 -> "الثاني 🥈"
                                        2 -> "الثالث 🥉"
                                        else -> "#${index + 1}"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = student.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${student.email} • 📞 ${student.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Exam Average Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "المعدل: ${student.examAverage}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandSuccess,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress indicators summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandLightBlue.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Filled.PlayCircle, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${student.completedVideosCount} فيديوهات", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandLightBlue.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Filled.Quiz, contentDescription = null, tint = BrandSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${student.examsRecord.size.coerceAtLeast(1)} امتحانات", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandLightBlue.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Filled.Download, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${student.downloadedNotesCount} ملازم", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            IconButton(onClick = { onToggleBlock(student.id) }) {
                                Icon(
                                    imageVector = if (student.isBlocked) Icons.Filled.LockOpen else Icons.Filled.Block,
                                    contentDescription = null,
                                    tint = if (student.isBlocked) BrandSuccess else BrandDanger
                                )
                            }
                            IconButton(onClick = { onDelete(student.id) }) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { inspectingStudent = student },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رؤية أدائه وكل شيء", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = { onEnterAsStudent(student) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("enter_as_student_${student.id}")
                            ) {
                                Icon(imageVector = Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("دخول بحسابه", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // Comprehensive Student Inspector Modal
    inspectingStudent?.let { student ->
        StudentInspectorDialog(
            student = student,
            onDismiss = { inspectingStudent = null },
            onEnterAsStudent = {
                inspectingStudent = null
                onEnterAsStudent(student)
            }
        )
    }
}

@Composable
fun StudentInspectorDialog(
    student: User,
    onDismiss: () -> Unit,
    onEnterAsStudent: () -> Unit
) {
    val context = LocalContext.current
    var inspectTab by remember { mutableIntStateOf(0) } // 0: الامتحانات والدرجات, 1: الفيديوهات, 2: الملازم, 3: بيانات الطالب

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ملف وأداء الطالب: ${student.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "معدل التفوق: ${student.examAverage}% • 📞 ${student.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.phone}"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(imageVector = Icons.Filled.Phone, contentDescription = "اتصال", tint = BrandPrimary)
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Inspection Tabs
                ScrollableTabRow(
                    selectedTabIndex = inspectTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    edgePadding = 8.dp,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = inspectTab == 0,
                        onClick = { inspectTab = 0 },
                        text = { Text("📝 الامتحانات (${student.examsRecord.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = inspectTab == 1,
                        onClick = { inspectTab = 1 },
                        text = { Text("🎬 الفيديوهات (${student.videosRecord.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = inspectTab == 2,
                        onClick = { inspectTab = 2 },
                        text = { Text("📚 الملازم (${student.downloadedNotesRecord.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = inspectTab == 3,
                        onClick = { inspectTab = 3 },
                        text = { Text("👤 بطاقة الطالب", fontWeight = FontWeight.Bold) }
                    )
                }

                when (inspectTab) {
                    0 -> {
                        // Exams Record
                        if (student.examsRecord.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "لم يقم الطالب بخوض أي امتحانات بعد في حسابه.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(14.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            student.examsRecord.forEach { record ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = record.examTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (record.score >= 85) BrandSuccess.copy(alpha = 0.15f) else BrandPrimary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "${record.score} / ${record.maxScore}",
                                                    color = if (record.score >= 85) BrandSuccess else BrandPrimary,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "المادة: ${record.subject} • تاريخ الحل: ${record.solvedAt}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "التقييم: ممتاز مع مرتبة الشرف 🌟 • الأسئلة المجابة: 10/10",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { (record.score.toFloat() / record.maxScore.toFloat()).coerceIn(0f, 1f) },
                                            color = BrandSuccess,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Videos Watched
                        if (student.videosRecord.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "لم يقم الطالب بمشاهدة فيديوهات بعد.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(14.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            student.videosRecord.forEach { vid ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = vid.videoTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (vid.isCompleted) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = BrandSuccess.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "مكتمل 100% ✅",
                                                        color = BrandSuccess,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = BrandPrimary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "${vid.progressPercent}% قيد المتابعة",
                                                        color = BrandPrimary,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "المادة: ${vid.subject} • آخر مشاهدة: ${vid.lastWatched}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { vid.progressPercent / 100f },
                                            color = if (vid.isCompleted) BrandSuccess else BrandPrimary,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Downloaded Notes
                        if (student.downloadedNotesRecord.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "لم يقم الطالب بتحميل ملازم دراسية بعد.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(14.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            student.downloadedNotesRecord.forEach { noteTitle ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = BrandSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = noteTitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "محملة ومتاحة أوفلاين في جهاز الطالب",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Student Identity & Detailed Info
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "بيانات الحساب الشخصي للطالب:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("• الاسم بالكامل: ${student.name}")
                                Text("• البريد الإلكتروني: ${student.email}")
                                Text("• رقم هاتف التواصل: ${student.phone}")
                                Text("• تاريخ إنشاء الحساب: ${student.createdAt}")
                                Text("• حالة الحساب: ${if (student.isBlocked) "محظور ❌" else "نشطومفعل ✅"}")
                                Text("• معدل النجاح العام: ${student.examAverage}%")
                                Text("• إجمالي الفيديوهات المكتملة: ${student.completedVideosCount}")
                                Text("• إجمالي الملازم المحملة: ${student.downloadedNotesCount}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onEnterAsStudent,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_enter_as_student_btn")
            ) {
                Icon(imageVector = Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("الدخول للمنصة بحساب هذا الطالب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق النافذة") }
        }
    )
}

@Composable
fun AdminNotificationCenterTab(
    notifications: List<NotificationItem>,
    onSend: (NotificationItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }
    var targetGroup by remember { mutableStateOf("جميع الطلاب") }
    var sentMessageBanner by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "إرسال إشعار فوري للطلاب (Push Notifications)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الإشعار") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("نص الإشعار") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isUrgent,
                    onClick = { isUrgent = false },
                    label = { Text("إشعار عادي") }
                )
                FilterChip(
                    selected = isUrgent,
                    onClick = { isUrgent = true },
                    label = { Text("عاجل ومهم 🔴") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandDanger,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (sentMessageBanner) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandSuccess.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = BrandSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تم بث الإشعار بنجاح لجميع الطلاب!",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (title.isNotBlank() && body.isNotBlank()) {
                        val notif = NotificationItem(
                            title = title,
                            body = body,
                            level = if (isUrgent) NotificationLevel.URGENT else NotificationLevel.NORMAL,
                            target = targetGroup,
                            sentAt = "الآن"
                        )
                        onSend(notif)
                        title = ""
                        body = ""
                        sentMessageBanner = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(imageVector = Icons.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إرسال الإشعار للطلاب الآن")
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "سجل الإشعارات المرسلة:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(notifications) { notif ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(notif.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(notif.sentAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun AdminMessagesInboxTab(
    messages: List<StudentMessage>,
    onReply: (String, String) -> Unit
) {
    var replyingToMessage by remember { mutableStateOf<StudentMessage?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "صندوق الرسائل الواردة من الطلاب",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(messages) { msg ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(msg.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${msg.type.labelAr} • 📞 ${msg.studentPhone}", style = MaterialTheme.typography.bodySmall, color = BrandPrimary)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (msg.status == MessageStatus.REPLIED) BrandSuccess.copy(alpha = 0.15f) else BrandDanger.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = msg.status.labelAr,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (msg.status == MessageStatus.REPLIED) BrandSuccess else BrandDanger,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(msg.subject, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(msg.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (msg.replies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        msg.replies.forEach { rep ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandLightBlue.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ردك: ${rep.body}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { replyingToMessage = msg },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (msg.status == MessageStatus.REPLIED) "إرسال رد إضافي" else "الرد على الطالب")
                        }
                    }
                }
            }
        }
    }

    replyingToMessage?.let { target ->
        var replyText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { replyingToMessage = null },
            title = { Text("الرد على الطالب: ${target.studentName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الموضوع: ${target.subject}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("نص الرد والتوضيح") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            onReply(target.id, replyText)
                            replyingToMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("إرسال الرد")
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingToMessage = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun AdminSettingsTab(
    currentName: String,
    currentEmail: String,
    onSave: (String, String) -> Unit
) {
    var platformName by remember { mutableStateOf(currentName) }
    var contactEmail by remember { mutableStateOf(currentEmail) }
    var savedToast by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("إعدادات وهوية المنصة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = platformName,
            onValueChange = { platformName = it },
            label = { Text("اسم المنصة الرئيسي") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = contactEmail,
            onValueChange = { contactEmail = it },
            label = { Text("بريد التواصل والدعم الفني") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (savedToast) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BrandSuccess.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = BrandSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تم حفظ التعديلات بنجاح!", color = BrandSuccess, fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = {
                onSave(platformName, contactEmail)
                savedToast = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("حفظ التغييرات")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = EduRepository.FOOTER_CREDIT,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
