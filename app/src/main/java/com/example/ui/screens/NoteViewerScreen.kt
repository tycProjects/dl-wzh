package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.NoteItem
import com.example.ui.theme.BrandLightBlue
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSuccess

@Composable
fun NoteViewerScreen(
    note: NoteItem,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(1) }
    var isDownloaded by remember { mutableStateOf(note.isDownloaded) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${note.subject} • ${note.fileSize} • صفحة $currentPage من 6",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "المفضلة",
                            tint = if (note.isFavorite) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            isDownloaded = true
                            onDownload()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDownloaded) BrandSuccess else BrandPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isDownloaded) "محفوظ" else "تنزيل")
                    }
                }
            }
        }

        // Reader Document Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandLightBlue
                    ) {
                        Text(
                            text = "الصفحة $currentPage: ملخص القواعد والمفاهيم الأساسية",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = note.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (currentPage) {
                            1 -> "📌 الباب الأول: القواعد الذهبية والملاحظات الاستنتاجية\n\n• قاعدة حاصل ضرب دالتين: الأولى في مشتقة الثانية + الثانية في مشتقة الأولى.\n• مشتقة خارج قسمة دالتين: (المقام × مشتقة البسط - البسط × مشتقة المقام) / (المقام)².\n• مشتقة دالة الدالة (قاعدة السلسلة): dy/dx = (dy/du) × (du/dx).\n\n💡 ملاحظة هامة للامتحان: تأكد دائماً من مجال تعريف الدالة قبل التعويض في المشتقة لتفادي القسمة على صفر."
                            2 -> "📌 الباب الثاني: تطبيقات القيم العظمى والصغرى ونقاط الانقلاب\n\n• النقاط الحرجة: f'(x) = 0 أو f'(x) غير معرّفة.\n• اختبار المشتقة الأولى: دراسة إشارة f'(x) حول النقطة الحرجة.\n• اختبار المشتقة الثانية: إذا كانت f''(c) < 0 فالنقطة عظمى محلية، وإذا كانت f''(c) > 0 فالنقطة صغرى محلية.\n• التحدب لأعلى وأسفل يتحدد بحسب إشارة المشتقة الثانية."
                            else -> "📌 الباب الثالث: التكامل غير المحدد والمحدد والمساحات والحجوم\n\n• المساحة بين منحنيين = التكامل من a إلى b للقيمة المطلقة لـ [f(x) - g(x)] dx.\n• حجم الجسم الدوراني حول محور السينات = π × التكامل من a إلى b لـ [y]² dx.\n• التكامل بالتعويض والتكامل بالتجزيء (قاعدة الجدول السريع)."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF1E293B),
                        lineHeight = 28.sp
                    )
                }
            }

            // Paging Navigation
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (currentPage > 1) currentPage-- },
                    enabled = currentPage > 1
                ) {
                    Text("الصفحة السابقة")
                }
                Text(
                    text = "$currentPage / 3",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { if (currentPage < 3) currentPage++ },
                    enabled = currentPage < 3
                ) {
                    Text("الصفحة التالية")
                }
            }
        }
    }
}
