package com.everline.lifesim.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.everline.lifesim.data.*
import com.everline.lifesim.domain.FamilyGraph
import com.everline.lifesim.domain.FamilyLink
import com.everline.lifesim.domain.GameMonth
import com.everline.lifesim.domain.SimulationRules

@Composable fun Page(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable fun GameCard(title: String, body: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            body()
        }
    }
}
@Composable fun Notice(text: String) { Text(text, color = Mist, modifier = Modifier.padding(16.dp)) }
@Composable fun ValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = Mist, modifier = Modifier.weight(1f))
        Text(value, color = Cream, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1.15f))
    }
}
@Composable fun MenuButton(text: String, detail: String = "", enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text, fontWeight = FontWeight.Bold)
            if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall, color = Mist)
        }
    }
}
@Composable fun StatBar(name: String, value: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ValueRow(name, "$value / 100")
        LinearProgressIndicator(progress = { value.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth(), color = Gold, trackColor = Slate)
    }
}
fun characterAge(slot: SaveSlotEntity, p: CharacterEntity) = SimulationRules.age(GameMonth(slot.year, slot.month), p.birthYear, p.birthMonth)
fun familyLinks(state: GameUiState) = state.relationships.map { FamilyLink(it.fromId, it.toId, it.kind) }
fun canEditFamily(mode: String) = mode in listOf("ADVANCED_SANDBOX", "SANDBOX_DYNASTY")
fun canSwitchFamily(mode: String) = mode in listOf("DYNASTY", "SANDBOX_DYNASTY")
fun modeName(mode: String): String = when (mode) {
    "STANDARD" -> "Standard Life"; "BASIC_SANDBOX" -> "Basic Sandbox"; "ADVANCED_SANDBOX" -> "Advanced Sandbox"
    "DYNASTY" -> "Dynasty"; "SANDBOX_DYNASTY" -> "Sandbox-Dynasty"; else -> mode
}

@Composable fun MainMenuScreen(slots: List<SaveSlotEntity>, onNew: () -> Unit,
                               onContinue: (String) -> Unit, onSaves: () -> Unit, onImport: () -> Unit) {
    Page {
        item { Spacer(Modifier.height(18.dp)) }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("E", color = Gold, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                Text("EVERLINE", color = Cream, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Every life leaves a legacy.", color = Mist)
                Spacer(Modifier.height(12.dp))
                Text("PROTOTYPE v0.1 · ALL FEATURES FREE", color = Gold, style = MaterialTheme.typography.labelMedium)
            }
        }
        item { GameCard("Begin") {
            Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("New Life") }
            if (slots.isNotEmpty()) MenuButton("Continue ${slots[0].title}", "${slots[0].year}-${slots[0].month.toString().padStart(2, '0')} · ${modeName(slots[0].mode)}") {
                onContinue(slots[0].id)
            }
            MenuButton("Save slots", "Load, export, import or remove a life", onClick = onSaves)
            MenuButton("Import local backup", "Restore a JSON save into a separate slot", onClick = onImport)
        } }
        item { GameCard("About this build") {
            Text("Native offline Android life simulator. Monthly calculations, character and family history, careers, education, sandbox edits, and living-relative dynasty switching.")
            Text("World content and additional systems are expanding through later milestones. No advertising or billing SDK is included.", color = Mist)
        } }
    }
}

@Composable fun NewLifeScreen(busy: Boolean, onCreate: (NewLifeDraft) -> Unit) {
    var random by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf("Alex") }
    var last by remember { mutableStateOf("Rivera") }
    var gender by remember { mutableStateOf("Male") }
    var year by remember { mutableStateOf("2006") }
    var month by remember { mutableStateOf("6") }
    var day by remember { mutableStateOf("15") }
    var startYear by remember { mutableStateOf("2026") }
    var country by remember { mutableStateOf("PH") }
    var city by remember { mutableStateOf("Quezon City") }
    var mode by remember { mutableStateOf("SANDBOX_DYNASTY") }
    val modes = listOf("STANDARD", "BASIC_SANDBOX", "ADVANCED_SANDBOX", "DYNASTY", "SANDBOX_DYNASTY")
    Page {
        item { GameCard("Starting method") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = random, onClick = { random = true }, label = { Text("Random Life") })
                FilterChip(selected = !random, onClick = { random = false }, label = { Text("Custom Life") })
            }
        } }
        item { GameCard("Game mode · unlocked in testing") {
            modes.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { mode = option }) {
                    RadioButton(selected = mode == option, onClick = { mode = option })
                    Text(modeName(option))
                }
            }
            Text("Hardcore, Challenge and Historical Scenario rule sets are not implemented in v0.1.", color = Mist, style = MaterialTheme.typography.bodySmall)
        } }
        if (!random) item { GameCard("Character identity") {
            EditText("First name", first) { first = it }
            EditText("Last name", last) { last = it }
            EditText("Gender", gender) { gender = it }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) { EditText("Year", year) { year = it } }
                Box(Modifier.weight(1f)) { EditText("Month", month) { month = it } }
                Box(Modifier.weight(1f)) { EditText("Day", day) { day = it } }
            }
            Text("Full calendar dates are supported; life advances monthly.", style = MaterialTheme.typography.bodySmall, color = Mist)
        } }
        item { GameCard("World setup") {
            Text("Country (starter dataset)", color = Mist)
            WorldCatalog.countries.forEach { item ->
                Row(Modifier.fillMaxWidth().clickable { country = item.code; city = item.city }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = country == item.code, onClick = { country = item.code; city = item.city })
                    Text("${item.name} · ${item.currency}")
                }
            }
            if (!random) EditText("City", city) { city = it }
            EditText("Starting year (1600–2200)", startYear) { startYear = it }
            Text("Historical calendar support is present; historical world datasets and scenarios are not yet implemented.", color = Mist, style = MaterialTheme.typography.bodySmall)
        } }
        item { Button(
            onClick = { onCreate(NewLifeDraft(first, last, gender,
                year.toIntOrNull() ?: 0, month.toIntOrNull() ?: 0, day.toIntOrNull() ?: 0,
                country, city, startYear.toIntOrNull() ?: 0, 11, mode, random)) },
            enabled = !busy, modifier = Modifier.fillMaxWidth()
        ) { Text(if (random) "Generate Random Life" else "Create Custom Life") } }
    }
}

@Composable fun LifeScreen(state: GameUiState, busy: Boolean, advance: () -> Unit, onNavigate: (String) -> Unit) {
    val slot = state.slot
    val p = state.active
    if (slot == null || p == null) { Notice("Open a life from the main menu first."); return }
    val recent = state.events.filter { it.year == slot.year && it.month == slot.month }
    Page {
        item { GameCard("${slot.year} · Month ${slot.month}") {
            ValueRow("Mode", modeName(slot.mode))
            ValueRow("Character", "${p.firstName} ${p.lastName}")
            ValueRow("Age", "${characterAge(slot, p)} years")
            ValueRow("Status", if (p.isAlive) "Living" else "Deceased")
            ValueRow("Location", "${p.city}, ${WorldCatalog.country(p.countryCode).name}")
            ValueRow("Cash", WorldCatalog.money(p.countryCode, p.cashCents))
            Button(onClick = advance, enabled = !busy && p.isAlive, modifier = Modifier.fillMaxWidth()) {
                Text("Advance +1 Month")
            }
            Text("One tap = one atomic calendar month across the whole family.", color = Mist, style = MaterialTheme.typography.bodySmall)
        } }
        item { GameCard("Life attributes") {
            StatBar("Health", p.health)
            StatBar("Intelligence", p.intelligence)
            StatBar("Social ability", p.charisma)
            StatBar("Talent", p.talent)
            StatBar("Happiness", p.happiness)
        } }
        item { GameCard("Current life") {
            ValueRow("Education", p.degree.ifBlank { p.educationStage })
            ValueRow("Occupation", p.occupation)
            ValueRow("Employer", p.employer.ifBlank { "—" })
            ValueRow("Monthly salary", WorldCatalog.money(p.countryCode, p.monthlySalaryCents))
            ValueRow("Relatives", "${state.characters.size - 1} family records")
        } }
        item { GameCard("Monthly summary") {
            if (recent.isEmpty()) Text("No events logged for this month yet.", color = Mist)
            recent.take(15).forEach { item ->
                Text("• ${item.title} — ${item.description}", color = Cream, style = MaterialTheme.typography.bodyMedium)
            }
        } }
        item { GameCard("Gameplay") {
            listOf("Activities" to "activities", "Education" to "education", "Career" to "career",
                "Finances" to "finances", "Relationships" to "relationships", "Family Tree" to "family",
                "Monthly Timeline" to "timeline", "Achievements" to "achievements",
                "Save Management" to "saves", "Settings" to "settings"
            ).forEach { (label, path) -> MenuButton(label, onClick = { onNavigate(path) }) }
            if (slot.mode in listOf("BASIC_SANDBOX", "ADVANCED_SANDBOX", "SANDBOX_DYNASTY"))
                MenuButton("Basic Sandbox: edit yourself", onClick = { onNavigate("basic") })
            if (canSwitchFamily(slot.mode)) MenuButton("Switch living relative", "No death or calendar advance required", onClick = { onNavigate("dynasty") })
        } }
    }
}

@Composable fun BasicSandboxScreen(state: GameUiState, busy: Boolean,
    save: (Int, Int, Long) -> Unit, onError: (String) -> Unit) {
    val person = state.active ?: return Notice("No active character")
    var health by remember(person.id) { mutableStateOf(person.health.toString()) }
    var happiness by remember(person.id) { mutableStateOf(person.happiness.toString()) }
    var cash by remember(person.id) { mutableStateOf(java.math.BigDecimal.valueOf(person.cashCents, 2).toPlainString()) }
    Page { item { GameCard("Basic Sandbox · all free") {
        Text("Edit your active character's health, happiness and bank balance. Advanced Sandbox Family Editor allows editing relatives, careers, education and complete finances.", color = Mist)
        EditText("Health (0–100)", health) { health = it }
        EditText("Happiness (0–100)", happiness) { happiness = it }
        EditText("Cash in ${WorldCatalog.country(person.countryCode).currency}", cash) { cash = it }
        MenuButton("Apply Basic Sandbox changes", enabled = !busy, onClick = {
            val h = health.toIntOrNull()
            val joy = happiness.toIntOrNull()
            val money = runCatching { java.math.BigDecimal(cash).movePointRight(2).longValueExact() }.getOrNull()
            if (h == null || joy == null || money == null) onError("Enter valid stats and a money amount with two decimal places.")
            else save(h, joy, money)
        })
    } } }
}

@Composable fun ActivitiesScreen(state: GameUiState, busy: Boolean, action: (String) -> Unit) {
    Page { item { GameCard("Activities · once per activity per month") {
        listOf("Exercise", "Read", "Practice Skill", "Relax").forEach {
            MenuButton(it, enabled = !busy, onClick = { action(it) })
        }
        Text("Activities update the character's stored attributes; duplicate actions in the same month are rejected.", color = Mist)
    } } }
}

@Composable fun EducationScreen(state: GameUiState, busy: Boolean, enroll: (String) -> Unit) {
    val p = state.active ?: return Notice("No active character")
    Page {
        item { GameCard("Education profile") {
            ValueRow("Highest stage", p.educationStage)
            ValueRow("Degree", p.degree.ifBlank { "None" })
            ValueRow("University", p.university.ifBlank { "None" })
            ValueRow("Current enrollment", p.enrollmentTarget.ifBlank { "None" })
            if (p.enrollmentTarget.isNotEmpty()) ValueRow("Months completed", "${p.educationMonths}")
        } }
        item { GameCard("Apply to a program") {
            listOf("Secondary" to "48 months", "Bachelor" to "48 months", "Master" to "24 months", "Doctorate" to "36 months").forEach { (name, duration) ->
                MenuButton(name, duration, enabled = !busy && p.enrollmentTarget.isBlank(), onClick = { enroll(name) })
            }
            Text("Prerequisites and minimum ages are enforced. A completed program creates a permanent history record.", color = Mist)
        } }
    }
}

@Composable fun CareerScreen(state: GameUiState, busy: Boolean, apply: (String) -> Unit, resign: () -> Unit, retire: () -> Unit) {
    val p = state.active ?: return Notice("No active character")
    Page {
        item { GameCard("Employment") {
            ValueRow("Occupation", p.occupation)
            ValueRow("Employer", p.employer.ifBlank { "None" })
            ValueRow("Rank", p.professionalRank.ifBlank { "Unranked" })
            ValueRow("Salary / month", WorldCatalog.money(p.countryCode, p.monthlySalaryCents))
            ValueRow("Months employed", p.careerMonths.toString())
            if (p.occupation != "Unemployed") MenuButton("Resign", enabled = !busy, onClick = resign)
            if (!p.retired) MenuButton("Retire (age 55+)", enabled = !busy, onClick = retire)
        } }
        item { GameCard("Available employers · modeled test content") {
            WorldCatalog.careers.forEach { job ->
                MenuButton(job.title, "${job.employer} · ${WorldCatalog.money(p.countryCode, WorldCatalog.salary(p.countryCode, job))}/mo · ${job.minimumEducation}",
                    enabled = !busy && p.occupation == "Unemployed" && !p.retired, onClick = { apply(job.id) })
            }
            Text("Applications may be accepted or rejected; wages begin at the next monthly advance.", color = Mist)
        } }
    }
}

@Composable fun FinanceScreen(state: GameUiState, ledger: List<LedgerEntryEntity>) {
    val p = state.active ?: return Notice("No active character")
    Page {
        item { GameCard("Personal financial account") {
            ValueRow("Cash / bank", WorldCatalog.money(p.countryCode, p.cashCents))
            ValueRow("Property and assets", WorldCatalog.money(p.countryCode, p.assetsCents))
            ValueRow("Liabilities", WorldCatalog.money(p.countryCode, p.debtCents))
            ValueRow("Net worth", WorldCatalog.money(p.countryCode, p.cashCents + p.assetsCents - p.debtCents))
            ValueRow("Monthly salary", WorldCatalog.money(p.countryCode, p.monthlySalaryCents))
            ValueRow("Monthly living cost", WorldCatalog.money(p.countryCode, p.livingCostCents))
        } }
        item { Text("ACCOUNT JOURNAL", color = Gold, style = MaterialTheme.typography.titleMedium) }
        if (ledger.isEmpty()) item { Notice("No monthly transactions yet.") }
        items(ledger) { entry -> GameCard("${entry.year}-${entry.month.toString().padStart(2, '0')} · ${entry.kind}") {
            Text(entry.note)
            ValueRow("Cash change", WorldCatalog.money(p.countryCode, entry.cashDeltaCents))
            ValueRow("Debt change", WorldCatalog.money(p.countryCode, entry.debtDeltaCents))
        } }
    }
}

@Composable fun RelationshipScreen(state: GameUiState, busy: Boolean, socialize: (String) -> Unit) {
    val slot = state.slot ?: return Notice("No save")
    val person = state.active ?: return Notice("No character")
    val links = familyLinks(state)
    val kin = FamilyGraph.connected(person.id, links)
    Page {
        item { Text("FAMILY & SOCIAL RELATIONSHIPS", color = Gold) }
        items(state.characters.filter { it.id != person.id && it.id in kin }) { relative ->
            GameCard("${relative.firstName} ${relative.lastName}") {
                ValueRow("Relationship", FamilyGraph.label(person.id, relative.id, links))
                ValueRow("Age", "${characterAge(slot, relative)}")
                val edge = state.relationships.firstOrNull { (it.fromId == person.id && it.toId == relative.id) || (it.toId == person.id && it.fromId == relative.id) }
                if (edge != null) {
                    ValueRow("Relationship quality", "${edge.quality} / 100")
                    MenuButton("Spend time together", enabled = !busy && relative.isAlive, onClick = { socialize(relative.id) })
                }
            }
        }
    }
}

@Composable fun FamilyTreeScreen(state: GameUiState, onEdit: (String) -> Unit, onSwitch: () -> Unit) {
    val slot = state.slot ?: return Notice("No save")
    val person = state.active ?: return Notice("No active character")
    val links = familyLinks(state)
    val levels = FamilyGraph.generationOffsets(person.id, links)
    val groups = state.characters.filter { it.id in levels }
        .groupBy { levels[it.id] ?: 0 }.toSortedMap()
    fun generationName(level: Int): String = when (level) {
        -3 -> "Great-grandparent generation"; -2 -> "Grandparent generation"
        -1 -> "Parent generation"; 0 -> "Contemporaries and spouses"
        1 -> "Children's generation"; 2 -> "Grandchildren's generation"
        3 -> "Great-grandchildren's generation"
        else -> if (level < 0) "Ancestors · ${-level} generations" else "Descendants · $level generations"
    }
    Page {
        item { GameCard("Lineage explorer") {
            Text("${person.firstName} ${person.lastName}'s family across ${groups.size} generations. Select family members to edit; the displayed parent links are stored relationship records.", color = Mist)
            if (canSwitchFamily(slot.mode)) MenuButton("Switch controllable relative", onClick = onSwitch)
        } }
        groups.forEach { (level, members) ->
            item { Text(generationName(level).uppercase(), color = Gold, style = MaterialTheme.typography.titleSmall) }
            items(members.sortedBy { it.birthYear }) { relative ->
                val parents = state.relationships.filter { it.toId == relative.id && it.kind in listOf("PARENT", "ADOPTIVE_PARENT") }
                    .mapNotNull { edge -> state.characters.firstOrNull { it.id == edge.fromId }?.firstName }
                GameCard("${relative.firstName} ${relative.lastName}") {
                    ValueRow("Relation to active", FamilyGraph.label(person.id, relative.id, links))
                    ValueRow("Born", "${relative.birthYear}-${relative.birthMonth.toString().padStart(2, '0')}")
                    ValueRow("Age", characterAge(slot, relative).toString())
                    ValueRow("Status", if (relative.isAlive) "Living" else "Deceased")
                    if (parents.isNotEmpty()) ValueRow("Parent links", parents.joinToString(" · "))
                    if (canEditFamily(slot.mode)) MenuButton("Edit this family member", onClick = { onEdit(relative.id) })
                }
            }
        }
    }
}

@Composable fun DynastyScreen(state: GameUiState, busy: Boolean, switch: (String) -> Unit, onSettings: () -> Unit) {
    val slot = state.slot ?: return Notice("No save")
    val p = state.active ?: return Notice("No active character")
    val links = familyLinks(state)
    val related = FamilyGraph.connected(p.id, links)
    Page {
        item { GameCard("Shared calendar: ${slot.year}-${slot.month.toString().padStart(2, '0')}") {
            Text("Active: ${p.firstName} ${p.lastName}. Switching is instantaneous, does not advance time or move assets.")
            ValueRow("Minimum age", slot.minSwitchAge.toString())
            ValueRow("Guardian-managed younger relatives", if (slot.guardianManagedMinors) "Allowed" else "Not allowed")
            if (slot.mode == "SANDBOX_DYNASTY") MenuButton("Change eligibility policy", onClick = onSettings)
        } }
        items(state.characters.filter { it.id in related && it.id != p.id }) { relative ->
            val age = characterAge(slot, relative)
            val allowed = FamilyGraph.canSwitch(p.id, relative.id, links, relative.isAlive, age, slot.minSwitchAge, slot.guardianManagedMinors)
            GameCard("${relative.firstName} ${relative.lastName}") {
                ValueRow("Relation", FamilyGraph.label(p.id, relative.id, links))
                ValueRow("Age", "$age · ${if (relative.isAlive) "living" else "deceased"}")
                ValueRow("Cash", WorldCatalog.money(relative.countryCode, relative.cashCents))
                MenuButton(if (allowed) "Play as this relative" else "Not eligible under current policy",
                    enabled = !busy && allowed, onClick = { switch(relative.id) })
            }
        }
        item { GameCard("Switch history") {
            if (state.switches.isEmpty()) Text("No switches yet.", color = Mist)
            state.switches.take(12).forEach { log ->
                val from = state.characters.firstOrNull { it.id == log.fromCharacterId }?.firstName ?: "Unknown"
                val to = state.characters.firstOrNull { it.id == log.toCharacterId }?.firstName ?: "Unknown"
                Text("${log.year}-${log.month}: $from → $to")
            }
        } }
    }
}

@Composable fun TimelineScreen(state: GameUiState) {
    Page {
        item { Text("PERSISTENT LIFE HISTORY", color = Gold, style = MaterialTheme.typography.titleMedium) }
        if (state.events.isEmpty()) item { Notice("Nothing recorded yet.") }
        items(state.events) { e -> GameCard("${e.year}-${e.month.toString().padStart(2, '0')} · ${e.title}") {
            ValueRow("Category", e.category)
            Text(e.description)
        } }
        item { GameCard("Sandbox edit audit") {
            if (state.edits.isEmpty()) Text("No edits have been recorded.")
            state.edits.take(40).forEach { log ->
                Text("${log.field}: ${log.previousValue} → ${log.newValue}", style = MaterialTheme.typography.bodySmall)
            }
        } }
    }
}

@Composable fun AchievementsScreen(state: GameUiState) {
    Page { item { GameCard("Achievements: roadmap") {
        Text("Individual and dynasty achievements are planned but not implemented in prototype v0.1.")
        Text("Life events and permanent switch history are persisted and provide a foundation for later achievement calculations.", color = Mist)
    } } }
}

@Composable fun SettingsScreen(state: GameUiState, busy: Boolean, update: (Int, Boolean) -> Unit) {
    val slot = state.slot ?: return Notice("No save")
    var ageText by remember(slot.id) { mutableStateOf(slot.minSwitchAge.toString()) }
    var guardians by remember(slot.id) { mutableStateOf(slot.guardianManagedMinors) }
    Page {
        item { GameCard("Temporary testing access") {
            ValueRow("Billing", "DISABLED")
            ValueRow("Ads", "NONE")
            ValueRow("Online required", "NO")
            ValueRow("Mode", modeName(slot.mode))
        } }
        item { GameCard("Dynasty eligibility") {
            if (slot.mode == "SANDBOX_DYNASTY") {
                EditText("Minimum switch age (0–25)", ageText) { ageText = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = guardians, onCheckedChange = { guardians = it })
                    Text("Allow guardian-managed younger characters")
                }
                MenuButton("Save policy", enabled = !busy && ageText.toIntOrNull() in 0..25,
                    onClick = { update(ageText.toInt(), guardians) })
            } else Text("Eligibility overrides are available in Sandbox-Dynasty mode.", color = Mist)
        } }
        item { GameCard("Content provenance") {
            Text("World and wage datasets are fictional modeling estimates for test gameplay, not current official statistics.", color = Mist)
            Text("Language: English. Translation architecture and Filipino localization remain planned.")
        } }
    }
}

@Composable fun SavesScreen(slots: List<SaveSlotEntity>, currentId: String?, busy: Boolean, onManualSave: () -> Unit,
    onLoad: (String) -> Unit, onExport: (String) -> Unit, onImport: () -> Unit, onDelete: (String) -> Unit) {
    var pendingDeletion by remember { mutableStateOf<String?>(null) }
    Page {
        item { GameCard("Save management") {
            MenuButton("Import JSON backup", enabled = !busy, onClick = onImport)
            if (currentId != null) MenuButton("Validate / Save Now", enabled = !busy, onClick = onManualSave)
            Text("Changes are autosaved to Room. Import creates a separate slot and validates family references.", color = Mist)
        } }
        items(slots) { slot -> GameCard(slot.title) {
            ValueRow("Calendar", "${slot.year}-${slot.month.toString().padStart(2, '0')}")
            ValueRow("Mode", modeName(slot.mode))
            MenuButton(if (slot.id == currentId) "Open (current)" else "Load this life", enabled = !busy, onClick = { onLoad(slot.id) })
            MenuButton("Export JSON backup", enabled = !busy, onClick = { onExport(slot.id) })
            MenuButton("Delete this slot", enabled = !busy, onClick = { pendingDeletion = slot.id })
        } }
    }
    if (pendingDeletion != null) AlertDialog(onDismissRequest = { pendingDeletion = null },
        title = { Text("Delete this life?") }, text = { Text("This permanently deletes this local slot. Export a backup first if needed.") },
        confirmButton = { TextButton(onClick = { pendingDeletion?.let(onDelete); pendingDeletion = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { pendingDeletion = null }) { Text("Cancel") } })
}

@Composable fun EditText(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true)
}
