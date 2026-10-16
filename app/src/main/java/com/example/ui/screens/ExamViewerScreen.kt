package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.EduRepository
import com.example.data.ExamItem
import com.example.data.ExamType
import com.example.data.QuestionType
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandLightBlue
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSuccess
import kotlinx.coroutines.delay

@Composable
fun ExamViewerScreen(
    exam: ExamItem,
    onBack: () -> Unit,
    onSubmitAttempt: (score: Int, total: Int, correct: Int) -> Unit
) {
    when (exam.type) {
        ExamType.PDF -> PdfExamViewer(exam = exam, onBack = onBack)
        ExamType.HTML_CODE -> HtmlCodeExamViewer(exam = exam, onBack = onBack, onSubmit = onSubmitAttempt)
        else -> InteractiveExamRunner(exam = exam, onBack = onBack, onSubmit = onSubmitAttempt)
    }
}

@Composable
fun HtmlCodeExamViewer(
    exam: ExamItem,
    onBack: () -> Unit,
    onSubmit: (score: Int, total: Int, correct: Int) -> Unit
) {
    var isSubmitted by remember { mutableStateOf(exam.isSolved) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = exam.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "امتحان كود HTML معزول - منصة حمو التعليمية",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandPrimary
                        )
                    }
                }

                Button(
                    onClick = {
                        isSubmitted = true
                        onSubmit(100, 1, 1)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandSuccess),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isSubmitted) "تم التسليم بنجاح" else "تسليم الإجابات")
                }
            }
        }

        // Android WebView rendering custom HTML code
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    loadDataWithBaseURL(null, exam.htmlCode, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, exam.htmlCode, "text/html", "UTF-8", null)
            }
        )
    }
}

@Composable
fun PdfExamViewer(
    exam: ExamItem,
    onBack: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(1) }
    var isDownloaded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = exam.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "نموذج PDF - صفحة $currentPage من 3",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { isDownloaded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDownloaded) BrandSuccess else BrandPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isDownloaded) "تم التحميل" else "تحميل PDF")
                }
            }
        }

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
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "منصة حمو التعليمية - إعداد حمادة ابن ضيف",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = exam.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "السؤال الأول (صفحة $currentPage):\n\nأجب عن الأسئلة الآتية موضحاً خطوات الحل الرياضي بدقة:\n\n١. احسب قيمة التكامل المحدود من 0 إلى π/4 للدالة sec²(x) dx.\n\n٢. فسر انبعاث الإلكترونات الكهروضوئية عند سقوط ضوء ذو تردد أكبر من تردد العتبة.\n\n٣. اكتب الصيغة الجزيئية والبنائية للمركب الناتج من هيدرة الإيثاين بوجود حمض الكبريتيك وكبريتات الزئبقيك.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF1E293B),
                        lineHeight = 26.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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

            Text(
                text = EduRepository.FOOTER_CREDIT,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InteractiveExamRunner(
    exam: ExamItem,
    onBack: () -> Unit,
    onSubmit: (score: Int, total: Int, correct: Int) -> Unit
) {
    val questions = exam.questions
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    val essayAnswers = remember { mutableStateMapOf<Int, String>() }
    var isSubmitted by remember { mutableStateOf(exam.isSolved) }
    var remainingSeconds by remember { mutableIntStateOf(exam.durationMinutes * 60) }

    LaunchedEffect(isSubmitted) {
        if (!isSubmitted) {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }
            if (remainingSeconds == 0) {
                isSubmitted = true
            }
        }
    }

    fun calculateScore(): Pair<Int, Int> {
        var correct = 0
        questions.forEachIndexed { index, q ->
            if (q.type == QuestionType.ESSAY) {
                if (essayAnswers[index]?.isNotBlank() == true) correct++
            } else {
                if (userAnswers[index] == q.correctIndex) correct++
            }
        }
        val percentage = if (questions.isNotEmpty()) (correct * 100) / questions.size else 100
        return Pair(percentage, correct)
    }

    val (scorePercentage, correctCount) = calculateScore()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Exam Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                        Text(
                            text = exam.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    if (!isSubmitted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (remainingSeconds < 300) BrandDanger.copy(alpha = 0.15f) else BrandLightBlue)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = if (remainingSeconds < 300) BrandDanger else BrandPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val mins = remainingSeconds / 60
                            val secs = remainingSeconds % 60
                            Text(
                                text = "%02d:%02d".format(mins, secs),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingSeconds < 300) BrandDanger else BrandPrimary
                            )
                        }
                    }
                }

                if (!isSubmitted && questions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { ((currentQuestionIndex + 1).toFloat() / questions.size.toFloat()) },
                        color = BrandPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "السؤال ${currentQuestionIndex + 1} من ${questions.size} • إعداد حمادة ابن ضيف",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isSubmitted) {
            // Result View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(if (scorePercentage >= 50) BrandSuccess.copy(alpha = 0.15f) else BrandDanger.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (scorePercentage >= 50) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                contentDescription = null,
                                tint = if (scorePercentage >= 50) BrandSuccess else BrandDanger,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (scorePercentage >= 50) "أحسنت! اجتزت الامتحان بتفوق" else "فرصة أفضل في المراجعة القادمة",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "درجتك: $scorePercentage% ($correctCount من ${questions.size} إجابات صحيحة)",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (scorePercentage >= 50) BrandSuccess else BrandDanger,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("العودة إلى الامتحانات")
                }

                Text(
                    text = EduRepository.FOOTER_CREDIT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (questions.isNotEmpty()) {
            val currentQ = questions[currentQuestionIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "السؤال ${currentQuestionIndex + 1}:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BrandLightBlue
                                ) {
                                    Text(
                                        text = currentQ.type.labelAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentQ.questionText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // If Question Has Attached Image / Diagram
                            if (!currentQ.imageUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth().height(140.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Image,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = "مخطط الرسم البياني التوضيحي للسؤال",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentQ.type == QuestionType.ESSAY) {
                        val currentText = essayAnswers[currentQuestionIndex] ?: ""
                        OutlinedTextField(
                            value = currentText,
                            onValueChange = { essayAnswers[currentQuestionIndex] = it },
                            label = { Text("اكتب إجابتك النموذجية هنا:") },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth().testTag("essay_answer_input")
                        )
                    } else {
                        currentQ.options.forEachIndexed { optIndex, optionText ->
                            val isSelected = userAnswers[currentQuestionIndex] == optIndex
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) BrandLightBlue else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) BrandPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { userAnswers[currentQuestionIndex] = optIndex }
                                    .testTag("question_opt_$optIndex")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, if (isSelected) BrandPrimary else Color.Gray, CircleShape)
                                            .background(if (isSelected) BrandPrimary else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                        enabled = currentQuestionIndex > 0,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("السابق")
                    }

                    if (currentQuestionIndex < questions.size - 1) {
                        Button(
                            onClick = { currentQuestionIndex++ },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("السؤال التالي")
                        }
                    } else {
                        Button(
                            onClick = {
                                isSubmitted = true
                                val (score, correct) = calculateScore()
                                onSubmit(score, questions.size, correct)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandSuccess),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("submit_exam_btn")
                        ) {
                            Text("تسليم الامتحان والنتيجة")
                        }
                    }
                }
            }
        }
    }
}
