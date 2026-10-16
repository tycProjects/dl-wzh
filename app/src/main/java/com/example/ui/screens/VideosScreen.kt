package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Category
import com.example.data.ContentStatus
import com.example.data.UserRole
import com.example.data.VideoItem
import com.example.data.VideoSourceType
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.EduSearchBar
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandLightBlue
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSuccess

@Composable
fun VideosScreen(
    videos: List<VideoItem>,
    categories: List<Category>,
    userRole: UserRole,
    onVideoClick: (String) -> Unit,
    onAddVideo: (VideoItem) -> Unit,
    onUpdateVideo: (VideoItem) -> Unit,
    onDeleteVideo: (String) -> Unit,
    onTogglePublish: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("cat_all") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingVideo by remember { mutableStateOf<VideoItem?>(null) }

    val filteredVideos = remember(videos, searchQuery, selectedCategoryId, userRole) {
        videos.filter { video ->
            val matchesCategory = selectedCategoryId == "cat_all" || video.categoryId == selectedCategoryId
            val matchesSearch = searchQuery.isBlank() ||
                video.title.contains(searchQuery, true) ||
                video.description.contains(searchQuery, true)
            val matchesRole = if (userRole == UserRole.ADMIN) true else video.status == ContentStatus.PUBLISHED
            matchesCategory && matchesSearch && matchesRole
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "مكتبة المحاضرات والشروحات",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "مشاهدة مباشرة فائقة السرعة مع حفظ موضع المشاهدة والتحكم بالسرعة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                EduSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "ابحث في عناوين المحاضرات والشروحات..."
                )
            }

            item {
                CategoryFilterRow(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { selectedCategoryId = it }
                )
            }

            if (filteredVideos.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "لم يتم العثور على فيديوهات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                items(filteredVideos) { video ->
                    VideoCardItem(
                        video = video,
                        isAdmin = userRole == UserRole.ADMIN,
                        onClick = { onVideoClick(video.id) },
                        onEdit = { editingVideo = video },
                        onDelete = { onDeleteVideo(video.id) },
                        onTogglePublish = { onTogglePublish(video.id) }
                    )
                }
            }
        }

        // Admin Add Video FAB
        if (userRole == UserRole.ADMIN) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("admin_add_video_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "إضافة فيديو")
            }
        }
    }

    if (showAddDialog) {
        VideoUploadAndFetchDialog(
            categories = categories.filter { it.id != "cat_all" },
            onDismiss = { showAddDialog = false },
            onSave = { newVid ->
                onAddVideo(newVid)
                showAddDialog = false
            }
        )
    }

    editingVideo?.let { target ->
        VideoUploadAndFetchDialog(
            initialVideo = target,
            categories = categories.filter { it.id != "cat_all" },
            onDismiss = { editingVideo = null },
            onSave = { updated ->
                onUpdateVideo(updated)
                editingVideo = null
            }
        )
    }
}

@Composable
fun VideoCardItem(
    video: VideoItem,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePublish: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("video_card_${video.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(BrandPrimary.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )

                // Source Type Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (video.sourceType == VideoSourceType.UPLOAD) "📱 مرفوع من الهاتف" else "🌐 رابط خارجي مدمج",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = video.duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Completed badge
                if (video.isCompleted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandSuccess)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تمت المشاهدة",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Progress bar
                if (!video.isCompleted && video.currentPositionSeconds > 0) {
                    val progressFraction = (video.currentPositionSeconds.toFloat() / video.durationSeconds.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = BrandPrimary,
                        trackColor = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandLightBlue
                    ) {
                        Text(
                            text = video.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (video.status == ContentStatus.DRAFT) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BrandDanger.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "مسودة (مخفي)",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandDanger,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onTogglePublish) {
                            Icon(
                                imageVector = if (video.status == ContentStatus.PUBLISHED) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "تبديل النشر",
                                tint = BrandPrimary
                            )
                        }
                        IconButton(onClick = onEdit) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "تعديل", tint = BrandPrimary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف", tint = BrandDanger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoUploadAndFetchDialog(
    initialVideo: VideoItem? = null,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (VideoItem) -> Unit
) {
    var sourceType by remember { mutableStateOf(initialVideo?.sourceType ?: VideoSourceType.EXTERNAL) }
    var urlOrPath by remember { mutableStateOf(initialVideo?.url ?: "") }
    var title by remember { mutableStateOf(initialVideo?.title ?: "") }
    var description by remember { mutableStateOf(initialVideo?.description ?: "") }
    var duration by remember { mutableStateOf(initialVideo?.duration ?: "42:00") }
    var selectedCatId by remember { mutableStateOf(initialVideo?.categoryId ?: categories.firstOrNull()?.id ?: "cat_math") }
    var isFetching by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var isUploadingFromPhone by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initialVideo == null) "إضافة فيديو (رابط خارجي أو من الهاتف)" else "تعديل المحاضرة")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Source Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sourceType == VideoSourceType.EXTERNAL,
                        onClick = { sourceType = VideoSourceType.EXTERNAL },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رابط خارجي")
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = sourceType == VideoSourceType.UPLOAD,
                        onClick = {
                            sourceType = VideoSourceType.UPLOAD
                            if (urlOrPath.isBlank() || urlOrPath.startsWith("http")) {
                                urlOrPath = "storage/emulated/0/DCIM/Lectures/lesson_part1.mp4"
                            }
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("من الهاتف")
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                if (sourceType == VideoSourceType.EXTERNAL) {
                    OutlinedTextField(
                        value = urlOrPath,
                        onValueChange = { urlOrPath = it },
                        label = { Text("رابط الفيديو (YouTube / Drive / Vimeo)") },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Auto-fetch metadata button
                    Button(
                        onClick = {
                            isFetching = true
                            // Auto-fetch simulation
                            if (title.isBlank()) {
                                title = "شرح وحل مسائل الدرس النموذجي - إعداد حمادة ابن ضيف"
                            }
                            if (description.isBlank()) {
                                description = "تم جلب بيانات الفيديو بنجاح من الرابط الخارجي ويعرض مباشرة داخل المنصة بدون الخروج منها."
                            }
                            duration = "48:20"
                            isFetching = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandLightBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.Sync, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جلب بيانات الفيديو تلقائياً", color = BrandPrimary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Upload from Phone
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("ملف الفيديو المختار من ذاكرة الهاتف:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(urlOrPath.ifBlank { "lesson_recorded_2026.mp4 (145 MB)" }, style = MaterialTheme.typography.labelSmall, color = BrandPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    isUploadingFromPhone = true
                                    uploadProgress = 1.0f
                                    if (title.isBlank()) {
                                        title = "محاضرة مسجلة من الهاتف: المراجعة النهائية"
                                    }
                                    if (description.isBlank()) {
                                        description = "فيديو عالي الدقة تم رفعه مباشرة من جهاز المالك إلى خادم المنصة."
                                    }
                                    duration = "35:10"
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اختيار فيديو من ملفات الهاتف")
                            }

                            if (isUploadingFromPhone) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { uploadProgress },
                                    color = BrandSuccess,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("تم تحميل وتجهيز الفيديو للعرض داخل المنصة بنجاح!", color = BrandSuccess, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الفيديو") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("المدة (مثال: 45:00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val catName = categories.find { it.id == selectedCatId }?.name ?: "عام"
                    val item = initialVideo?.copy(
                        title = title.ifBlank { "محاضرة جديدة" },
                        description = description,
                        sourceType = sourceType,
                        url = urlOrPath.ifBlank { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" },
                        duration = duration,
                        categoryId = selectedCatId,
                        categoryName = catName
                    ) ?: VideoItem(
                        title = title.ifBlank { "محاضرة جديدة - منصة حمو التعليمية" },
                        description = description.ifBlank { "شرح متكامل يعرض داخل المنصة دون الخروج منها." },
                        sourceType = sourceType,
                        url = urlOrPath.ifBlank { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" },
                        duration = duration,
                        categoryId = selectedCatId,
                        categoryName = catName
                    )
                    onSave(item)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("حفظ ونشر داخل المنصة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
