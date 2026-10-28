package com.schedule.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.schedule.app.MainViewModel
import com.schedule.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * مساعد نصي/صوتي بالعامية. يفهم أوامر محددة بالقواعد (إضافة محاضرة/واجب/ملاحظة/امتحان، إلغاء، استعلام عن اليوم، بحث، تلخيص، أسئلة)
 * ولا يستخدم نموذج ذكاء اصطناعي.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(vm: MainViewModel, onBack: () -> Unit) {
    val msgs = remember { mutableStateListOf(false to "اكتب أو قل أمرا، مثلا: ضيف محاضرة فيزياء يوم الأحد من 10 الى 12 قاعة 5") }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val voice = rememberVoiceInput { input = it }
    fun send(text: String) {
        if (text.isBlank()) return
        msgs += true to text.trim(); input = ""
        scope.launch {
            val r = try { vm.assistant(text) } catch (e: Exception) { "تعذّر تنفيذ الأمر." }
            msgs += false to r
            list.animateScrollToItem(msgs.lastIndex)
        }
    }
    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text("المساعد") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().imePadding()) {
            LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(16.dp, 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(msgs.size) { i ->
                    val (mine, t) = msgs[i]
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End) {
                        Box(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(12.dp)).background(if (mine) Crimson else Surface1).padding(12.dp)) {
                            Text(t, color = if (mine) Color.White else TextMain, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ايه محاضراتي بكرة", "المحاضرة الجاية", "ايه عندي النهارده").forEach { AssistChip({ send(it) }, { Text(it) }) }
            }
            MutedText("يعتمد على قواعد لغوية وليس نموذج ذكاء اصطناعي. البحث لفظي تقريبي، والتلخيص استخلاصي.", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("اكتب أمرك") }, maxLines = 3, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Crimson, unfocusedBorderColor = Border, focusedContainerColor = Surface2, unfocusedContainerColor = Surface2))
                IconButton(voice) { Icon(Icons.Filled.Mic, "إملاء صوتي", tint = Crimson) }
                IconButton({ send(input) }) { Icon(Icons.AutoMirrored.Filled.Send, "إرسال", tint = Crimson) }
            }
        }
    }
}
