package com.lecturemanager.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lecturemanager.app.ui.*
import com.lecturemanager.app.ui.theme.AppTheme
import com.lecturemanager.app.ui.theme.Canvas
import com.lecturemanager.app.ui.theme.CardBg
import com.lecturemanager.app.ui.theme.NavyFill

/** FragmentActivity (وليس ComponentActivity فقط) لأن BiometricPrompt يحتاجها. */
class MainActivity : FragmentActivity() {
    private val vm: MainViewModel by viewModels()
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleScheduleIntent(intent)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            AppTheme(settings.themeMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val locked by vm.locked.collectAsStateWithLifecycle()
                    if (locked) LockScreen(vm) else AppRoot(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleScheduleIntent(intent)
    }

    /** استقبال محاضرة منقولة من تطبيق منظّم الجدول. */
    private fun handleScheduleIntent(i: android.content.Intent?) {
        if (i?.action == ACTION_IMPORT_LECTURES && !i.getBooleanExtra("handled", false)) {
            i.putExtra("handled", true)
            val json = i.getStringExtra("items_json") ?: return
            val uris = (0 until (i.clipData?.itemCount ?: 0)).mapNotNull { i.clipData?.getItemAt(it)?.uri }
            runCatching {
                val arr = org.json.JSONArray(json)
                val batch = (0 until arr.length()).map { k ->
                    val o = arr.getJSONObject(k)
                    MainViewModel.ScheduleItem(o.optString("subject", "مادة بدون اسم"), o.optString("instructor"), o.optString("lecture_title"),
                        o.optInt("week", 1), o.optString("notes"), o.optInt("pdf_index", -1).let { idx -> uris.getOrNull(idx) })
                }.filter { it.title.isNotBlank() }
                vm.importBatchFromSchedule(batch)
            }
            return
        }
        if (i?.action != ACTION_IMPORT_LECTURE || i.getBooleanExtra("handled", false)) return
        i.putExtra("handled", true) // يمنع التكرار عند إعادة إنشاء الواجهة
        val title = i.getStringExtra("lecture_title").orEmpty().ifBlank { return }
        vm.importFromSchedule(
            subject = i.getStringExtra("subject").orEmpty().ifBlank { "مادة بدون اسم" },
            instructor = i.getStringExtra("instructor").orEmpty(),
            week = i.getIntExtra("week", 1), title = title, notes = i.getStringExtra("notes").orEmpty(),
            pdf = i.data ?: i.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri,
        )
    }

    companion object {
        const val ACTION_IMPORT_LECTURE = "com.lecturemanager.app.action.IMPORT_LECTURE"
        const val ACTION_IMPORT_LECTURES = "com.lecturemanager.app.action.IMPORT_LECTURES"
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // القفل بعد غياب أكثر من 30 ثانية عن التطبيق
        if (stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > 30_000) vm.lock()
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    Tab("home", "المواد", Icons.Filled.MenuBook),
    Tab("cards", "البطاقات", Icons.Filled.Style),
    Tab("search", "البحث", Icons.Filled.Search),
    Tab("settings", "الإعدادات", Icons.Filled.Settings),
)

@Composable
fun AppRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snack = remember { SnackbarHostState() }
    val msg by vm.message.collectAsStateWithLifecycle()
    LaunchedEffect(msg) { msg?.let { snack.showSnackbar(it); vm.consumeMessage() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        containerColor = Canvas,
        floatingActionButton = {
            if (route == "home") FloatingActionButton(
                onClick = { nav.navigate("add") }, containerColor = NavyFill, contentColor = Color.White,
            ) { Icon(Icons.Filled.Add, "إضافة") }
        },
        bottomBar = {
            if (TABS.any { it.route == route }) NavigationBar(containerColor = CardBg) {
                TABS.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, null) }, label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm, onOpen = { nav.navigate("viewer/$it") }) }
            composable("cards") { CardsScreen(vm, onReview = { nav.navigate("review/$it") }) }
            composable("search") {
                SearchScreen(vm, onOpen = { id, page -> nav.navigate("viewer/$id?page=$page") })
            }
            composable("settings") { SettingsScreen(vm) }
            composable("add") { AddScreen(vm, onDone = { nav.popBackStack() }) }
            composable(
                "viewer/{id}?page={page}",
                listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("page") { type = NavType.IntType; defaultValue = -1 },
                ),
            ) { e ->
                ViewerScreen(
                    vm, e.arguments!!.getLong("id"), e.arguments!!.getInt("page"),
                    onBack = { nav.popBackStack() },
                    onOpenOther = { id -> nav.navigate("viewer/$id") { popUpTo("home") } },
                )
            }
            composable("review/{subjectId}", listOf(navArgument("subjectId") { type = NavType.LongType })) { e ->
                ReviewScreen(vm, e.arguments!!.getLong("subjectId"), onBack = { nav.popBackStack() })
            }
        }
    }
}
