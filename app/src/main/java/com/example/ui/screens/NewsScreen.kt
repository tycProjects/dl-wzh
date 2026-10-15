package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NewsItem
import com.example.data.UserRole
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandLightBlue
import com.example.ui.theme.BrandPrimary

@Composable
fun NewsScreen(
    newsList: List<NewsItem>,
    userRole: UserRole,
    onAddNews: (NewsItem, Boolean) -> Unit,
    onTogglePin: (String) -> Unit,
    onDeleteNews: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    // Sorted so pinned news always appear first
    val sortedNews = remember(newsList) {
        newsList.sortedByDescending { it.isPinned }
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
                    text = "الأخبار والإعلانات المدرسية",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تابع أحدث الجداول والمواعيد وتنبيهات الامتحانات أولاً بأول",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(sortedNews) { news ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("news_feed_card_${news.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (news.isPinned) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BrandDanger.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PushPin,
                                            contentDescription = null,
                                            tint = BrandDanger,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "خبر مثبت",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandDanger,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "إعلان رسمي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = news.publishedAt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (userRole == UserRole.ADMIN) {
                                    IconButton(onClick = { onTogglePin(news.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.PushPin,
                                            contentDescription = "تثبيت",
                                            tint = if (news.isPinned) BrandDanger else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteNews(news.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "حذف",
                                            tint = BrandDanger
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = news.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = news.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        // Admin Add News FAB
        if (userRole == UserRole.ADMIN) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("admin_add_news_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "نشر خبر")
            }
        }
    }

    if (showAddDialog) {
        AddNewsDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newNews, sendNotif ->
                onAddNews(newNews, sendNotif)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddNewsDialog(
    onDismiss: () -> Unit,
    onSave: (NewsItem, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var sendNotification by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نشر خبر أو إعلان جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الخبر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("تفاصيل الخبر والإعلان") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isPinned = !isPinned }
                ) {
                    Checkbox(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors = CheckboxDefaults.colors(checkedColor = BrandDanger)
                    )
                    Text("تثبيت الخبر في الأعلى (📌 Pinned)")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { sendNotification = !sendNotification }
                ) {
                    Checkbox(
                        checked = sendNotification,
                        onCheckedChange = { sendNotification = it },
                        colors = CheckboxDefaults.colors(checkedColor = BrandPrimary)
                    )
                    Text("إرسال إشعار فوري للطلاب بهذا الخبر")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = NewsItem(
                        title = title.ifBlank { "إعلان جديد" },
                        body = body.ifBlank { "تفاصيل الإعلان" },
                        isPinned = isPinned,
                        publishedAt = "الآن"
                    )
                    onSave(item, sendNotification)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("نشر الآن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
