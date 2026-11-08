package com.akira.island.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.island.R
import com.akira.island.core.LangScope
import com.akira.island.core.tr
import com.akira.island.data.SettingsRepo
import com.akira.island.ui.screens.AnimScreen
import com.akira.island.ui.screens.AppInfoScreen
import com.akira.island.ui.screens.BannersScreen
import com.akira.island.ui.screens.BanksScreen
import com.akira.island.ui.screens.BehaviorScreen
import com.akira.island.ui.screens.CapsuleScreen
import com.akira.island.ui.screens.CardsScreen
import com.akira.island.ui.screens.ColorsScreen
import com.akira.island.ui.screens.DiagScreen
import com.akira.island.ui.screens.EffectsScreen
import com.akira.island.ui.screens.EventsScreen
import com.akira.island.ui.screens.FeaturesScreen
import com.akira.island.ui.screens.GesturesScreen
import com.akira.island.ui.screens.HomeScreen
import com.akira.island.ui.screens.IdeaScreen
import com.akira.island.ui.screens.InstructionScreen
import com.akira.island.ui.screens.KeepAliveScreen
import com.akira.island.ui.screens.LookScreen
import com.akira.island.ui.screens.LyricsScreen
import com.akira.island.ui.screens.NotificationsScreen
import com.akira.island.ui.screens.PayDiagScreen
import com.akira.island.ui.screens.PermissionsScreen
import com.akira.island.ui.screens.SizeScreen
import com.akira.island.ui.screens.TestsScreen
import com.akira.island.ui.LocalDylandGlassBackdrop
import com.akira.island.ui.rememberDylandGlassBackdrop
import com.akira.island.ui.DylandBackdropLayer
import com.akira.island.ui.dylandLiquidGlass
import com.akira.island.service.KeepAliveService
import com.akira.island.update.UpdateChecker
import com.styropyr0.prismal.components.PrismalGlassBottomTab
import com.styropyr0.prismal.components.PrismalGlassBottomTabs

enum class Screen {
    CARDS, EVENTS, GESTURES, LYRICS, SIZE, COLORS, INSTRUCTION, ABOUT, DIAG, LOOK, ANIM, EFFECTS, BANNERS,
    PERMISSIONS, TESTS, NOTIFICATIONS, BEHAVIOR, PAYDIAG, KEEPALIVE, BANKS,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsRepo.init(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { IslandTheme { AppRoot() } }
    }

    override fun onResume() {
        super.onResume()
        // «Не выгружать остров»: из фона Android может не запустить службу — запускаем, пока приложение открыто
        KeepAliveService.sync(this)
    }
}

@Composable
private fun AppRoot() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var stack by remember { mutableStateOf(listOf<Screen>()) }
    var forward by remember { mutableStateOf(true) }
    val open: (Screen) -> Unit = {
        forward = true
        stack = stack + it
    }
    val back: () -> Unit = {
        forward = false
        stack = stack.dropLast(1)
    }
    BackHandler(enabled = stack.isNotEmpty(), onBack = back)

    // Раз в 3 часа спрашиваем GitHub, не вышла ли новая версия; после обновления — «Что нового»
    val context = LocalContext.current
    LaunchedEffect(Unit) { UpdateChecker.check(context.applicationContext, force = false) }
    // Сменили язык — экраны рисуются заново на нём, а открытая вкладка и экран остаются
    LangScope {
        WhatsNewDialog()

        val glassBackdrop = rememberDylandGlassBackdrop()
        CompositionLocalProvider(LocalDylandGlassBackdrop provides glassBackdrop) {
        Box(Modifier.fillMaxSize()) {
            // Slowly moving blue-violet light makes the refraction visible without visual clutter.
            DylandBackdropLayer(glassBackdrop)
            val key = stack.lastOrNull()?.name ?: "tab$tab"
            AnimatedContent(
                targetState = key,
                transitionSpec = {
                    if (targetState.startsWith("tab") && initialState.startsWith("tab")) {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(120))
                    } else if (forward) {
                        (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 8 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 8 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
                    }
                },
                label = "nav",
            ) { k ->
                when (k) {
                    "tab0" -> HomeScreen(open)
                    "tab1" -> FeaturesScreen(open)
                    "tab2" -> CapsuleScreen(open)
                    "tab3" -> AppInfoScreen(open)
                    Screen.CARDS.name -> CardsScreen(back) { open(Screen.INSTRUCTION) }
                    Screen.EVENTS.name -> EventsScreen(back)
                    Screen.GESTURES.name -> GesturesScreen(back)
                    Screen.LYRICS.name -> LyricsScreen(back)
                    Screen.SIZE.name -> SizeScreen(back)
                    Screen.COLORS.name -> ColorsScreen(back)
                    Screen.INSTRUCTION.name -> InstructionScreen(back)
                    Screen.ABOUT.name -> IdeaScreen(back)
                    Screen.DIAG.name -> DiagScreen(back)
                    Screen.PAYDIAG.name -> PayDiagScreen(back)
                    Screen.KEEPALIVE.name -> KeepAliveScreen(back)
                    Screen.BANKS.name -> BanksScreen(back)
                    Screen.LOOK.name -> LookScreen(back)
                    Screen.ANIM.name -> AnimScreen(back)
                    Screen.EFFECTS.name -> EffectsScreen(back)
                    Screen.BANNERS.name -> BannersScreen(back)
                    Screen.PERMISSIONS.name -> PermissionsScreen(back, open)
                    Screen.TESTS.name -> TestsScreen(back)
                    Screen.NOTIFICATIONS.name -> NotificationsScreen(back)
                    Screen.BEHAVIOR.name -> BehaviorScreen(back)
                }
            }

            AnimatedVisibility(
                visible = stack.isEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                BottomBar(tab) { tab = it }
            }
        }
        }
    }
}

/** Вкладки: главная, что показывает остров, как он выглядит, и о приложении — отдельно от настроек. */
private fun tabTitles() = listOf(tr("Главная", "Home"), tr("Функции", "Features"), tr("Оформление", "Appearance"), tr("О приложении", "About"))

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val backdrop = LocalDylandGlassBackdrop.current
    val icons = listOf(
        ImageVector.vectorResource(R.drawable.ic_tab_island),
        Icons.Rounded.Widgets,
        Icons.Rounded.Palette,
        Icons.Rounded.Info,
    )
    val titles = tabTitles()
    if (backdrop != null) {
        PrismalGlassBottomTabs(
            selectedTabIndex = { selected },
            onTabSelected = onSelect,
            backdrop = backdrop,
            tabsCount = 4,
            tintDropletContent = false,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
        ) {
            titles.forEachIndexed { index, title ->
                PrismalGlassBottomTab(onClick = { onSelect(index) }) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = title,
                            tint = Ui.Text,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = title,
                            color = Ui.Text,
                            fontSize = 10.sp,
                            fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}
