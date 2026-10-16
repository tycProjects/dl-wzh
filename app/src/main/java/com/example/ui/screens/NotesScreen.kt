package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.EduRepository
import com.example.data.NoteItem
import com.example.data.UserRole
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.EduSearchBar
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandLightBlue
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NotesScreen(
    notes: List<NoteItem>,
    categories: List<Category>,
    userRole: UserRole,
    onNoteClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadNote: (String) -> Unit,
    onAddNote: (NoteItem) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("cat_all") }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val downloadingProgressMap = remember { mutableStateMapOf<String, Float>() }

    val filteredNotes = remember(notes, searchQuery, selectedSubject, showFavoritesOnly) {
        notes.filter { note ->
            val matchesSubject = selectedSubject == "cat_all" || note.subject.contains(
                categories.find { it.id == selectedSubject }?.name?.replace("جميع المواد", "") ?: "",
                true
            )
            val matchesSearch = searchQuery.isBlank() ||
                note.title.contains(searchQuery, true) ||
                note.description.contains(searchQuery, true)
            val matchesFav = if (showFavoritesOnly) note.isFavorite else true
            matchesSubject && matchesSearch && matchesFav
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
                    text = "قسم الملازم والمذكرات الذهبية",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تحميل مباشر وفوري داخل المنصة دون الخروج منها - إعداد حمادة ابن ضيف",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                EduSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "ابحث في المذكرات والخرائط الذهنية..."
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showFavoritesOnly,
                        onClick = { showFavoritesOnly = false },
                        label = { Text("جميع الملازم (${notes.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = showFavoritesOnly,
                        onClick = { showFavoritesOnly = true },
                        label = { Text("المفضلة ⭐") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            item {
                CategoryFilterRow(
                    categories = categories,
                    selectedCategoryId = selectedSubject,
                    onCategorySelected = { selectedSubject = it }
                )
            }

            if (filteredNotes.isEmpty()) {
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
                                text = "لا توجد ملازم مطابقة للبحث",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                items(filteredNotes) { note ->
                    val progress = downloadingProgressMap[note.id]
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNoteClick(note.id) }
                            .testTag("note_card_${note.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BrandLightBlue
                                    ) {
                                        Text(
                                            text = note.subject,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = note.fileSize,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onToggleFavorite(note.id) }) {
                                        Icon(
                                            imageVector = if (note.isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                            contentDescription = "المفضلة",
                                            tint = if (note.isFavorite) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (userRole == UserRole.ADMIN) {
                                        IconButton(onClick = { onDeleteNote(note.id) }) {
                                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف", tint = BrandDanger)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Dynamic In-App Download Progress Bar
                            if (progress != null && progress < 1f) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "جاري التحميل داخل المنصة: ${(progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BrandPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "السرعة: 2.8 MB/s",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BrandSuccess
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = BrandPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تاريخ الرفع: ${note.createdAt}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onNoteClick(note.id) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("معاينة")
                                    }

                                    Button(
                                        onClick = {
                                            if (!note.isDownloaded) {
                                                coroutineScope.launch {
                                                    // Progressive download simulation in-app
                                                    downloadingProgressMap[note.id] = 0.2f
                                                    delay(300)
                                                    downloadingProgressMap[note.id] = 0.55f
                                                    delay(350)
                                                    downloadingProgressMap[note.id] = 0.85f
                                                    delay(300)
                                                    downloadingProgressMap[note.id] = 1.0f
                                                    delay(100)
                                                    downloadingProgressMap.remove(note.id)
                                                    onDownloadNote(note.id)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (note.isDownloaded) BrandSuccess else BrandPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("download_note_btn_${note.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (note.isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (note.isDownloaded) "تم التحميل بنجاح" else "تحميل بالمنصة")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = EduRepository.FOOTER_CREDIT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Admin Add Note FAB
        if (userRole == UserRole.ADMIN) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("admin_add_note_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "إضافة ملزمة")
            }
        }
    }

    if (showAddDialog) {
        CreateNoteWithFetchDialog(
            categories = categories.filter { it.id != "cat_all" },
            onDismiss = { showAddDialog = false },
            onSave = { newNote ->
                onAddNote(newNote)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CreateNoteWithFetchDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (NoteItem) -> Unit
) {
    var externalUrl by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(categories.firstOrNull()?.name ?: "الرياضيات") }
    var fileSize by remember { mutableStateOf("4.5 MB") }
    var isFetched by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("رفع ملزمة جديدة (برابط خارجي أو مباشر)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = externalUrl,
                    onValueChange = { externalUrl = it },
                    label = { Text("الرابط الخارجي للملزمة (PDF / Drive)") },
                    placeholder = { Text("https://drive.google.com/file/...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Link, contentDescription = null, tint = BrandPrimary)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        isFetched = true
                        if (title.isBlank()) {
                            title = "مذكرة التوقعات المرئية - إعداد حمادة ابن ضيف"
                        }
                        if (description.isBlank()) {
                            description = "تم جلب بيانات الملف بنجاح وتجهيزه للتحميل المباشر داخل منصة حمو."
                        }
                        fileSize = "5.8 MB"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandLightBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Filled.Sync, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جلب بيانات الملزمة تلقائياً", color = BrandPrimary, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الملزمة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف ومحتويات المذكرة") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fileSize,
                    onValueChange = { fileSize = it },
                    label = { Text("حجم الملف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val note = NoteItem(
                        title = title.ifBlank { "مذكرة جديدة - منصة حمو التعليمية" },
                        description = description.ifBlank { "ملزمة شاملة إعداد حمادة ابن ضيف" },
                        subject = selectedSubject,
                        fileUrl = externalUrl.ifBlank { "https://example.com/handout.pdf" },
                        fileSize = fileSize,
                        createdAt = "الآن"
                    )
                    onSave(note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("حفظ وتثبيت بالمنصة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
