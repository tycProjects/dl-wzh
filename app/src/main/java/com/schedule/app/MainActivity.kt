package com.schedule.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.schedule.app.ui.*
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.ui.theme.Bg
import com.schedule.app.util.dayIndex
import com.schedule.app.util.weekNumber
import com.schedule.app.work.Notifier
import kotlinx.coroutines.delay
import java.time.LocalDate

/** FragmentActivity (وليس ComponentActivity فقط) لأن BiometricPrompt تحتاجها. */
class MainActivity : FragmentActivity() {
    private val vm: MainViewModel by viewModels()
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifier.ensureChannels(this)
        handleIntent(intent)
        vm.rescheduleAll() // يضمن بقاء المنبهات بعد أي تحديث أو تنظيف للنظام
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            AppTheme(settings.lightTheme) {
                val view = LocalView.current
                SideEffect {
                    val c = WindowCompat.getInsetsController(window, view)
                    c.isAppearanceLightStatusBars = settings.lightTheme; c.isAppearanceLightNavigationBars = settings.lightTheme
                }
                Surface(Modifier.fillMaxSize(), color = Bg) {
                    val locked by vm.locked.collectAsStateWithLifecycle()
                    if (locked) LockScreen(vm) else AppRoot(vm)
                }
            }
        }
    }

    override fun onStop() { super.onStop(); stoppedAt = SystemClock.elapsedRealtime() }

    override fun onStart() {
        super.onStart()
        // القفل بعد غياب أكثر من 30 ثانية عن التطبيق
        if (stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > 30_000) vm.lock()
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }

    private fun handleIntent(i: Intent?) {
        val rec = i?.getLongExtra(Notifier.EXTRA_RECORD_SLOT, 0) ?: 0
        val open = i?.getLongExtra(Notifier.EXTRA_OPEN_SLOT, 0) ?: 0
        if (rec > 0) vm.navRequest.value = MainViewModel.NavRequest(2, rec)
        else if (open > 0) vm.navRequest.value = MainViewModel.NavRequest(1, open)
        // نحذف الإضافات لتجنب تكرار التنقل عند إعادة إنشاء النشاط
        i?.removeExtra(Notifier.EXTRA_RECORD_SLOT); i?.removeExtra(Notifier.EXTRA_OPEN_SLOT)
    }
}

@Composable
private fun AppRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val req by vm.navRequest.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    val slots by vm.slots.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val files by vm.attachments.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var bulk by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { if (Build.VERSION.SDK_INT >= 33 && !Notifier.canPost(ctx)) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS) }
    // التنقل القادم من الإشعارات
    LaunchedEffect(req) {
        req?.let { r ->
            if (r.kind == 2 && r.slotId > 0) nav.navigate("record/${r.slotId}?auto=true")
            else if (r.slotId > 0) nav.navigate("slot/${r.slotId}")
            vm.navRequest.value = null
        }
    }
    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = "schedule") {
            composable("schedule") {
                ScheduleScreen(vm, onOpenSlot = { nav.navigate("slot/$it") }, onProfile = { nav.navigate("profile") }, onSettings = { nav.navigate("settings") },
                    onSearch = { nav.navigate("search") }, onTasks = { nav.navigate("tasks") }, onBulkTransfer = { bulk = true })
            }
            composable("slot/{id}", listOf(navArgument("id") { type = NavType.LongType })) { e ->
                SlotScreen(vm, e.arguments!!.getLong("id"), onBack = { nav.popBackStack() }, onRecord = { nav.navigate("record/$it") })
            }
            composable("record/{id}?auto={auto}", listOf(navArgument("id") { type = NavType.LongType }, navArgument("auto") { type = NavType.BoolType; defaultValue = false })) { e ->
                RecordScreen(vm, e.arguments!!.getLong("id"), e.arguments!!.getBoolean("auto"), onBack = { nav.popBackStack() })
            }
            composable("profile") { ProfileScreen(vm, onBack = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }, onHolidays = { nav.navigate("holidays") }) }
            composable("holidays") { HolidaysScreen(vm, onBack = { nav.popBackStack() }) }
            composable("search") { SearchScreen(vm, onBack = { nav.popBackStack() }, onOpenSlot = { nav.navigate("slot/$it") }) }
            composable("tasks") { TasksScreen(vm, onBack = { nav.popBackStack() }, onOpenSlot = { nav.navigate("slot/$it") }) }
        }
        msg?.let { m ->
            LaunchedEffect(m) { delay(2500); vm.consumeMessage() }
            Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp)) { androidx.compose.material3.Text(m) }
        }
    }
    if (bulk) BulkTransferDialog(
        slots, notes, files, defaultWeek = weekNumber(LocalDate.now(), LocalDate.ofEpochDay(settings.semesterStart)).coerceAtLeast(1),
        today = dayIndex(LocalDate.now()), onDismiss = { bulk = false },
    )
}
