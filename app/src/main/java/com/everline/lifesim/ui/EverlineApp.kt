package com.everline.lifesim.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EverlineApp(vm: GameViewModel = viewModel()) {
    val nav = rememberNavController()
    val slots by vm.slots.collectAsStateWithLifecycle()
    val state by vm.game.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val currentEntry by nav.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route ?: "menu"
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { vm.exportTo(it) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        vm.importFrom(uri) { nav.navigate("game") { popUpTo("menu") } }
    }
    val titles = mapOf(
        "menu" to "EVERLINE", "new" to "New life", "game" to "Character overview", "basic" to "Basic Sandbox",
        "activities" to "Activities", "education" to "Education", "career" to "Career",
        "finances" to "Finances", "relationships" to "Relationships", "family" to "Family tree",
        "dynasty" to "Dynasty switching", "timeline" to "Life timeline", "saves" to "Save management",
        "settings" to "Settings", "achievements" to "Achievements", "editor/{id}" to "Family editor"
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titles[route] ?: "Everline", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    if (route != "menu") IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    if (route != "menu" && route != "game") TextButton(onClick = { nav.navigate("game") }) { Text("Life") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Midnight, titleContentColor = Gold)
            )
        },
        containerColor = Midnight
    ) { padding ->
        NavHost(navController = nav, startDestination = "menu", modifier = Modifier.padding(padding)) {
            composable("menu") {
                MainMenuScreen(slots,
                    onNew = { nav.navigate("new") },
                    onContinue = { id -> vm.selectSave(id); nav.navigate("game") },
                    onSaves = { nav.navigate("saves") },
                    onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
            }
            composable("new") { NewLifeScreen(busy, onCreate = { draft -> vm.createLife(draft) {
                nav.navigate("game") { popUpTo("menu") }
            } }) }
            composable("game") { LifeScreen(state, busy, vm::advanceMonth, onNavigate = { nav.navigate(it) }) }
            composable("activities") { ActivitiesScreen(state, busy, vm::doActivity) }
            composable("basic") { BasicSandboxScreen(state, busy, vm::editBasicSandbox, vm::showError) }
            composable("education") { EducationScreen(state, busy, vm::enroll) }
            composable("career") { CareerScreen(state, busy, vm::applyJob, vm::resign, vm::retire) }
            composable("finances") { state.active?.let { person ->
                val entries by vm.repo.observeLedger(person.id).collectAsStateWithLifecycle(initialValue = emptyList())
                FinanceScreen(state, entries)
            } }
            composable("relationships") { RelationshipScreen(state, busy, vm::socialize) }
            composable("family") { FamilyTreeScreen(state, onEdit = { nav.navigate("editor/$it") },
                onSwitch = { nav.navigate("dynasty") }) }
            composable("dynasty") { DynastyScreen(state, busy, vm::switchCharacter, onSettings = { nav.navigate("settings") }) }
            composable("timeline") { TimelineScreen(state) }
            composable("achievements") { AchievementsScreen(state) }
            composable("settings") { SettingsScreen(state, busy, vm::switchPolicy) }
            composable("saves") { SavesScreen(slots, state.slot?.id, busy, vm::manualSave,
                onLoad = { vm.selectSave(it); nav.navigate("game") },
                onExport = { id -> vm.prepareExport(id); exportLauncher.launch("everline-${id.take(8)}.json") },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                onDelete = { id -> vm.deleteSave(id) {} }) }
            composable("editor/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id")
                val person = state.characters.firstOrNull { it.id == id }
                if (person != null) {
                    val records by vm.repo.observeRecords(person.id).collectAsStateWithLifecycle(initialValue = emptyList())
                    EditorScreen(state, person, records, busy, vm::saveEditedCharacter, vm::addRelative,
                        onDone = { nav.popBackStack() }, onQuality = vm::setQuality,
                        onHistory = vm::addProfileRecord, onError = vm::showError)
                }
                else Notice("Character no longer available in this save.")
            }
        }
    }
    if (message != null) AlertDialog(onDismissRequest = vm::clearMessage,
        title = { Text("Everline") }, text = { Text(message.orEmpty()) },
        confirmButton = { TextButton(onClick = vm::clearMessage) { Text("OK") } })
}
