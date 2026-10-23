package com.schedule.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.ui.theme.*
import com.schedule.app.work.RecordService
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(vm: MainViewModel, slotId: Long, auto: Boolean, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val slots by vm.slots.collectAsStateWithLifecycle()
    val slot = slots.firstOrNull { it.id == slotId }
    val rec by RecordService.state.collectAsStateWithLifecycle()
    val mine = rec.active && rec.slotId == slotId
    var hasMic by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var autoDone by rememberSaveable { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0) }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        hasMic = ok
        if (ok && !rec.active) RecordService.start(ctx, slotId) else if (!ok) ctx.toast("لا يمكن التسجيل بدون إذن الميكروفون")
    }
    fun start() { if (hasMic) RecordService.start(ctx, slotId) else perm.launch(Manifest.permission.RECORD_AUDIO) }

    // التشغيل التلقائي عند القدوم من إشعار "هل تريد تسجيل المحاضرة؟"
    LaunchedEffect(Unit) { if (auto && !autoDone && !rec.active) { autoDone = true; start() } }
    // إبقاء الشاشة مضيئة أثناء وجودنا هنا
    DisposableEffect(mine) { view.keepScreenOn = mine; onDispose { view.keepScreenOn = false } }
    LaunchedEffect(mine, rec.startedAt) {
        while (mine) { elapsed = (android.os.SystemClock.elapsedRealtime() - rec.startedAt) / 1000; delay(500) }
    }

    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text("تسجيل المحاضرة") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(slot?.title ?: "", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(32.dp))
            Text("%02d:%02d".format(elapsed / 60, elapsed % 60), fontSize = 64.sp, color = if (mine) Crimson else Muted, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            MutedText(if (mine) "جارٍ التسجيل... يمكنك إغلاق الشاشة والتسجيل مستمر." else if (rec.active) "هناك تسجيل آخر قيد التشغيل." else "اضغط للبدء. يُحفظ التسجيل تلقائيا في مرفقات المحاضرة.")
            Spacer(Modifier.height(40.dp))
            if (mine) Button({ RecordService.stop(ctx); ctx.toast("تم حفظ التسجيل في المرفقات") }, Modifier.fillMaxWidth().heightIn(min = 56.dp), colors = ButtonDefaults.buttonColors(containerColor = Crimson)) {
                Icon(Icons.Filled.Stop, null); Spacer(Modifier.width(8.dp)); Text("إيقاف وحفظ")
            } else Button(::start, Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = !rec.active, colors = ButtonDefaults.buttonColors(containerColor = Crimson)) {
                Icon(Icons.Filled.Mic, null); Spacer(Modifier.width(8.dp)); Text("بدء التسجيل")
            }
        }
    }
}
