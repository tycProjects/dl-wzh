package com.schedule.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.data.Exam
import com.schedule.app.ui.theme.*
import com.schedule.app.util.daysUntil
import java.time.LocalDate
import androidx.compose.runtime.saveable.rememberSaveable

/** الملف الشخصي: الاسم والصورة والامتحانات فقط؛ يُعدَّل من الزر العلوي. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val exams by vm.exams.collectAsStateWithLifecycle()
    var editMode by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable(settings.profileName) { mutableStateOf(settings.profileName) }
    var examDialog by remember { mutableStateOf<Exam?>(null) }
    var addingExam by remember { mutableStateOf(false) }
    val today = LocalDate.now().toEpochDay()
    val photo = remember(settings.photoPath) { settings.photoPath.takeIf { it.isNotEmpty() }?.let { runCatching { android.graphics.BitmapFactory.decodeFile(it) }.getOrNull() } }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::setPhoto) }

    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text("الملف الشخصي") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
            actions = { TextButton({ if (editMode) vm.setName(name); editMode = !editMode }) { Text(if (editMode) "حفظ" else "تعديل", color = Crimson) } })
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(104.dp).clip(CircleShape).background(Surface2).then(if (editMode) Modifier.clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } else Modifier), contentAlignment = Alignment.Center) {
                        if (photo != null) Image(photo.asImageBitmap(), "الصورة الشخصية", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Text(settings.profileName.firstOrNull()?.toString() ?: "؟", fontSize = 40.sp, color = Crimson)
                    }
                    if (editMode) {
                        OutlinedButton({ pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(if (photo == null) "إضافة صورة" else "تغيير الصورة") }
                        AppField(name, "الاسم") { name = it }
                    } else Text(settings.profileName.ifBlank { "اضغط تعديل لإضافة اسمك" }, style = MaterialTheme.typography.titleLarge)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { SectionTitle("مواعيد الامتحانات") }
                    if (editMode) TextButton({ addingExam = true }) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("إضافة") }
                }
                if (exams.isEmpty()) MutedText("لم تضف أي امتحان بعد.")
            }
            items(exams, key = { it.id }) { e ->
                val d = daysUntil(e.dateEpoch, today)
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.title, style = MaterialTheme.typography.titleMedium)
                            MutedText(formatDate(e.dateEpoch))
                        }
                        Text(when { d < 0 -> "انتهى"; d == 0L -> "اليوم"; d == 1L -> "غدا"; else -> "بعد $d يوم" }, color = if (d in 0..7) Crimson else TextMain, style = MaterialTheme.typography.titleSmall)
                        if (editMode) IconButton({ examDialog = e }) { Icon(Icons.Filled.Edit, "تعديل", tint = Crimson) }
                    }
                }
            }
        }
    }
    if (addingExam) ExamDialog(null, onSave = vm::saveExam, onDelete = null, onDismiss = { addingExam = false })
    examDialog?.let { e -> ExamDialog(e, onSave = vm::saveExam, onDelete = { vm.deleteExam(e.id) }, onDismiss = { examDialog = null }) }
}
