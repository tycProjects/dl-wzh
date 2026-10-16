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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Category
import com.example.data.ContentStatus
import com.example.data.ExamItem
import com.example.data.ExamQuestion
import com.example.data.ExamType
import com.example.data.QuestionType
import com.example.data.UserRole
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.EduSearchBar
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandLightBlue
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSuccess

@Composable
fun ExamsScreen(
    exams: List<ExamItem>,
    categories: List<Category>,
    userRole: UserRole,
    onExamClick: (String) -> Unit,
    onAddExam: (ExamItem) -> Unit,
    onUpdateExam: (ExamItem) -> Unit,
    onDeleteExam: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("cat_all") }
    var filterSolvedOnly by remember { mutableStateOf<Boolean?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredExams = remember(exams, searchQuery, selectedSubject, filterSolvedOnly) {
        exams.filter { exam ->
            val matchesSubject = selectedSubject == "cat_all" || exam.subject.contains(
                categories.find { it.id == selectedSubject }?.name?.replace("جميع المواد", "") ?: "",
                true
            )
            val matchesSearch = searchQuery.isBlank() ||
                exam.title.contains(searchQuery, true) ||
                exam.description.contains(searchQuery, true)
            val matchesSolved = when (filterSolvedOnly) {
                true -> exam.isSolved
                false -> !exam.isSolved
                null -> true
            }
            matchesSubject && matchesSearch && matchesSolved
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
                    text = "قسم الامتحانات والاختبارات",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "امتحانات تفاعلية مع صور توضيحية، امتحانات بكود HTML، ونماذج PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                EduSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "ابحث في الامتحانات وبنوك الأسئلة..."
                )
            }

            // Quick Status Filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterSolvedOnly == null,
                        onClick = { filterSolvedOnly = null },
                        label = { Text("الكل (${exams.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filterSolvedOnly == false,
                        onClick = { filterSolvedOnly = false },
                        label = { Text("المتاحة للحل") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filterSolvedOnly == true,
                        onClick = { filterSolvedOnly = true },
                        label = { Text("المكتملة والدرجات") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandSuccess,
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

            if (filteredExams.isEmpty()) {
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
                                text = "لا توجد امتحانات مطابقة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                items(filteredExams) { exam ->
                    ExamDetailedCard(
                        exam = exam,
                        isAdmin = userRole == UserRole.ADMIN,
                        onClick = { onExamClick(exam.id) },
                        onDelete = { onDeleteExam(exam.id) }
                    )
                }
            }
        }

        // Admin Create Exam FAB
        if (userRole == UserRole.ADMIN) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("admin_add_exam_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "إنشاء امتحان جديد")
            }
        }
    }

    if (showAddDialog) {
        CreateAdvancedExamDialog(
            categories = categories.filter { it.id != "cat_all" },
            onDismiss = { showAddDialog = false },
            onSave = { newExam ->
                onAddExam(newExam)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExamDetailedCard(
    exam: ExamItem,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("exam_card_${exam.id}")
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
                            text = exam.subject,
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (exam.type) {
                            ExamType.PDF -> BrandDanger.copy(alpha = 0.12f)
                            ExamType.HTML_CODE -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                            else -> BrandSuccess.copy(alpha = 0.12f)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = when (exam.type) {
                                    ExamType.PDF -> Icons.Filled.PictureAsPdf
                                    ExamType.HTML_CODE -> Icons.Filled.Code
                                    else -> Icons.Filled.Quiz
                                },
                                contentDescription = null,
                                tint = when (exam.type) {
                                    ExamType.PDF -> BrandDanger
                                    ExamType.HTML_CODE -> Color(0xFF8B5CF6)
                                    else -> BrandSuccess
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = exam.type.labelAr,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (exam.type) {
                                    ExamType.PDF -> BrandDanger
                                    ExamType.HTML_CODE -> Color(0xFF8B5CF6)
                                    else -> BrandSuccess
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (exam.isSolved && exam.lastScore != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandSuccess.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "تم الحل (${exam.lastScore}/${exam.maxScore})",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandSuccess,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = exam.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exam.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏱ ${exam.durationMinutes} دقيقة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "• ${exam.startsAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    if (isAdmin) {
                        IconButton(onClick = onDelete) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف", tint = BrandDanger)
                        }
                    }
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (exam.isSolved) BrandSuccess else BrandPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (exam.isSolved) "عرض النتيجة والتقرير" else "بدء الامتحان",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateAdvancedExamDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (ExamItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(categories.firstOrNull()?.name ?: "الرياضيات") }
    var examType by remember { mutableStateOf(ExamType.HTML) }
    var durationMinutes by remember { mutableStateOf("45") }
    var htmlCodeInput by remember { mutableStateOf("") }

    // Question builder state
    var questionText by remember { mutableStateOf("") }
    var questionType by remember { mutableStateOf(QuestionType.MULTIPLE_CHOICE) }
    var questionImageUrl by remember { mutableStateOf("") }
    var questionOptions by remember { mutableStateOf("الخيار الأول\nالخيار الثاني (الصحيح)\nالخيار الثالث\nالخيار الرابع") }
    var correctIndex by remember { mutableIntStateOf(1) }
    var explanation by remember { mutableStateOf("") }

    val createdQuestions = remember { mutableStateListOf<ExamQuestion>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء وتصميم امتحان جديد") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الامتحان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف والإرشادات") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    label = { Text("مدة الامتحان بالدقائق") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اختر طريقة بناء الامتحان:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = examType == ExamType.HTML,
                        onClick = { examType = ExamType.HTML },
                        label = { Text("بناء أسئلة وصور") }
                    )
                    FilterChip(
                        selected = examType == ExamType.HTML_CODE,
                        onClick = { examType = ExamType.HTML_CODE },
                        label = { Text("كود HTML كامل") }
                    )
                }

                if (examType == ExamType.HTML_CODE) {
                    OutlinedTextField(
                        value = htmlCodeInput,
                        onValueChange = { htmlCodeInput = it },
                        label = { Text("كود HTML الخاص بالامتحان") },
                        placeholder = { Text("<!DOCTYPE html><html><body>...</body></html>") },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Question Builder Form
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("إضافة سؤال جديد (${createdQuestions.size} أسئلة مضافة):", fontWeight = FontWeight.Bold, color = BrandPrimary)

                            // Question Type Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuestionType.values().forEach { qType ->
                                    FilterChip(
                                        selected = questionType == qType,
                                        onClick = { questionType = qType },
                                        label = { Text(qType.labelAr, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = questionText,
                                onValueChange = { questionText = it },
                                label = { Text("نص السؤال") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Question Image
                            OutlinedTextField(
                                value = questionImageUrl,
                                onValueChange = { questionImageUrl = it },
                                label = { Text("رابط صورة للسؤال (اختياري)") },
                                placeholder = { Text("https://example.com/diagram.png") },
                                trailingIcon = {
                                    Icon(imageVector = Icons.Filled.Image, contentDescription = null, tint = BrandPrimary)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (questionType == QuestionType.MULTIPLE_CHOICE) {
                                OutlinedTextField(
                                    value = questionOptions,
                                    onValueChange = { questionOptions = it },
                                    label = { Text("الخيارات (كل خيار في سطر)") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            OutlinedTextField(
                                value = explanation,
                                onValueChange = { explanation = it },
                                label = { Text("التفسير النموذجي للحل") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (questionText.isNotBlank()) {
                                        val opts = if (questionType == QuestionType.TRUE_FALSE) {
                                            listOf("صح (صواب)", "خطأ")
                                        } else if (questionType == QuestionType.ESSAY) {
                                            listOf("إجابة مقالية يدونها الطالب")
                                        } else {
                                            questionOptions.lines().filter { it.isNotBlank() }
                                        }

                                        val newQ = ExamQuestion(
                                            id = createdQuestions.size + 1,
                                            questionText = questionText,
                                            type = questionType,
                                            imageUrl = questionImageUrl.ifBlank { null },
                                            options = opts,
                                            correctIndex = correctIndex.coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                                            explanation = explanation
                                        )
                                        createdQuestions.add(newQ)
                                        questionText = ""
                                        questionImageUrl = ""
                                        explanation = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة هذا السؤال للامتحان")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val defaultQs = if (createdQuestions.isEmpty()) {
                        listOf(
                            ExamQuestion(
                                id = 1,
                                questionText = "ما هو القانون الرياضي الأساسي في هذا الاختبار؟",
                                type = QuestionType.MULTIPLE_CHOICE,
                                options = listOf("الخيار الأول", "الخيار النموذجي", "الخيار الثالث"),
                                correctIndex = 1,
                                explanation = "الشرح النموذجي مع إعداد حمادة ابن ضيف."
                            )
                        )
                    } else createdQuestions.toList()

                    val newExam = ExamItem(
                        title = title.ifBlank { "امتحان تفاعلي جديد - منصة حمو" },
                        description = description.ifBlank { "اختبار تدريبي مع إعداد حمادة ابن ضيف" },
                        subject = selectedSubject,
                        type = examType,
                        htmlCode = htmlCodeInput.ifBlank {
                            "<!DOCTYPE html><html dir='rtl'><body style='font-family:sans-serif;padding:20px'><h2>امتحان كود HTML</h2><p>منصة حمو التعليمية - إعداد حمادة ابن ضيف</p></body></html>"
                        },
                        durationMinutes = durationMinutes.toIntOrNull() ?: 30,
                        questions = defaultQs
                    )
                    onSave(newExam)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("حفظ ونشر الامتحان")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
