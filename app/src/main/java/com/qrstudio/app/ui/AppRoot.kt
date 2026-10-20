package com.qrstudio.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qrstudio.app.QrApp
import com.qrstudio.app.ui.create.CreateScreen
import com.qrstudio.app.ui.create.CreateViewModel
import com.qrstudio.app.ui.scan.ScanScreen
import com.qrstudio.app.ui.scan.ScanViewModel
import com.qrstudio.app.ui.theme.Bg
import com.qrstudio.app.ui.theme.BorderColor
import com.qrstudio.app.ui.theme.CardBg
import com.qrstudio.app.ui.theme.Emerald
import com.qrstudio.app.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as QrApp
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val createVm: CreateViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CreateViewModel(app.settingsRepository) }
        },
    )
    val scanVm: ScanViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ScanViewModel(app, app.historyRepository, app.settingsRepository) }
        },
    )

    LaunchedEffect(Unit) {
        launch { createVm.events.collect { snackbarHostState.showSnackbar(it) } }
        launch { scanVm.events.collect { snackbarHostState.showSnackbar(it) } }
    }

    var showSettings by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "create"

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = Bg,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // AUTO-REPAIR: disabled -- unresolved reference 'safeDrawing' (was: contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing,)
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                AppHeader(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSettings = { showSettings = true },
                )
                NavHost(
                    navController = navController,
                    startDestination = "create",
                    modifier = Modifier.weight(1f),
                ) {
                    composable("create") { CreateScreen(createVm) }
                    composable("scan") {
                        ScanScreen(
                            vm = scanVm,
                            onCreateFrom = { raw ->
                                createVm.loadFromText(raw)
                                navController.navigate("create") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                }
            }
        }
        if (showSettings) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                containerColor = CardBg,
            ) {
                SettingsSheet(
                    settings = app.settingsRepository,
                    history = app.historyRepository,
                    onDone = { message ->
                        scope.launch {
                            showSettings = false
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AppHeader(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onSettings: () -> Unit,
) {
    Surface(color = CardBg, border = BorderStroke(1.dp, BorderColor)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Emerald),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.QrCode2,
                        contentDescription = null,
                        tint = Color(0xFF06281f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("QR Studio", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.weight(1f))
            Surface(color = Bg, shape = RoundedCornerShape(50)) {
                Row(Modifier.padding(3.dp)) {
                    HeaderTab("إنشاء", selected = currentRoute == "create") { onNavigate("create") }
                    HeaderTab("قراءة", selected = currentRoute == "scan") { onNavigate("scan") }
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "الإعدادات", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun HeaderTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Emerald else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color(0xFF06281f) else TextMuted,
        )
    }
}
