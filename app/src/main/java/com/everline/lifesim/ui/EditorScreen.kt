package com.everline.lifesim.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everline.lifesim.data.CharacterEntity
import com.everline.lifesim.data.ProfileRecordEntity
import com.everline.lifesim.data.WorldCatalog
import java.math.BigDecimal

/** This is an actual persisted model editor: the Save button applies a single Room transaction. */
@Composable
fun EditorScreen(
    state: GameUiState,
    person: CharacterEntity,
    records: List<ProfileRecordEntity>,
    busy: Boolean,
    onSave: (CharacterEntity, Boolean, () -> Unit) -> Unit,
    onAddRelative: (String, String, String, Int, Boolean, () -> Unit) -> Unit,
    onDone: () -> Unit,
    onQuality: (String, Int) -> Unit,
    onHistory: (String, String, String, String, Int, Int?, String) -> Unit,
    onError: (String) -> Unit
) {
    val original = person
    val values = remember(person.id) { mutableStateMapOf(
        "first" to person.firstName, "last" to person.lastName, "gender" to person.gender,
        "year" to person.birthYear.toString(), "month" to person.birthMonth.toString(), "day" to person.birthDay.toString(),
        "country" to person.countryCode, "nationality" to person.nationality, "city" to person.city,
        "appearance" to person.appearance, "health" to person.health.toString(),
        "intelligence" to person.intelligence.toString(), "charisma" to person.charisma.toString(),
        "talent" to person.talent.toString(), "happiness" to person.happiness.toString(),
        "personality" to person.personality, "skills" to person.skills, "interests" to person.interests,
        "school" to person.school, "university" to person.university, "degree" to person.degree,
        "major" to person.major, "educationStage" to person.educationStage,
        "occupation" to person.occupation, "employer" to person.employer,
        "rank" to person.professionalRank, "salary" to moneyInput(person.monthlySalaryCents),
        "branch" to person.militaryBranch, "militaryRank" to person.militaryRank,
        "cash" to moneyInput(person.cashCents), "assets" to moneyInput(person.assetsCents),
        "debt" to moneyInput(person.debtCents), "living" to moneyInput(person.livingCostCents)
    ) }
    fun v(key: String) = values[key].orEmpty()
    var allowOverride by remember(person.id) { mutableStateOf(false) }
    var newRelation by remember(person.id) { mutableStateOf("Child") }
    var newName by remember(person.id) { mutableStateOf("") }
    var newYear by remember(person.id) { mutableStateOf(minOf(person.birthYear + 24, state.slot?.year ?: 2026).toString()) }
    var overrideFamilyAge by remember(person.id) { mutableStateOf(false) }
    fun suggestedYear(relation: String): Int {
        val delta = when (relation) {
            "Parent", "Stepparent" -> -28; "Grandparent" -> -56; "Aunt / Uncle" -> -26
            "Child", "Adopted Child" -> 24; "Grandchild" -> 48; "Niece / Nephew" -> 25
            "Sibling", "Cousin", "Spouse", "Partner" -> 0; else -> 0
        }
        return (person.birthYear + delta).coerceAtMost(state.slot?.year ?: 2026)
    }
    var showRelationDropdown by remember { mutableStateOf(false) }
    val relationOptions = listOf("Parent", "Child", "Adopted Child", "Spouse", "Partner", "Stepparent", "Sibling",
        "Grandparent", "Grandchild", "Aunt / Uncle", "Niece / Nephew", "Cousin")
    var recordCategory by remember(person.id) { mutableStateOf("EDUCATION") }
    var recordTitle by remember(person.id) { mutableStateOf("") }
    var recordOrg by remember(person.id) { mutableStateOf("") }
    var recordStart by remember(person.id) { mutableStateOf(state.slot?.year?.toString() ?: "2026") }
    var recordEnd by remember(person.id) { mutableStateOf("") }
    var recordDetails by remember(person.id) { mutableStateOf("") }
    fun parsePerson(): CharacterEntity? = try {
        original.copy(
            firstName = v("first").trim(), lastName = v("last").trim(), gender = v("gender").trim(),
            birthYear = v("year").toInt(), birthMonth = v("month").toInt(), birthDay = v("day").toInt(),
            countryCode = v("country").trim().uppercase(), nationality = v("nationality"), city = v("city"),
            appearance = v("appearance"), health = v("health").toInt(), intelligence = v("intelligence").toInt(),
            charisma = v("charisma").toInt(), talent = v("talent").toInt(), happiness = v("happiness").toInt(),
            personality = v("personality"), skills = v("skills"), interests = v("interests"),
            school = v("school"), university = v("university"), degree = v("degree"), major = v("major"),
            educationStage = v("educationStage"), occupation = v("occupation"), employer = v("employer"),
            professionalRank = v("rank"), monthlySalaryCents = parseCents(v("salary")),
            militaryBranch = v("branch"), militaryRank = v("militaryRank"),
            cashCents = parseCents(v("cash")), assetsCents = parseCents(v("assets")),
            debtCents = parseCents(v("debt")), livingCostCents = parseCents(v("living"))
        )
    } catch (_: Exception) { null }
    @Composable fun edit(label: String, key: String) { EditText(label, v(key)) { values[key] = it } }
    Page {
        item { GameCard("Advanced Sandbox Family Editor") {
            Text("Editing ${person.firstName} ${person.lastName}. Changes are audited, saved locally, and do not rewrite their descendants.")
            Row(Modifier.fillMaxWidth()) {
                Checkbox(checked = allowOverride, onCheckedChange = { allowOverride = it })
                Text("Sandbox override of parent/child age eligibility", modifier = Modifier.padding(top = 12.dp))
            }
        } }
        item { GameCard("Identity & personal details") {
            edit("First name", "first"); edit("Last name", "last"); edit("Gender", "gender")
            edit("Birth year", "year"); edit("Birth month", "month"); edit("Birth day", "day")
            edit("Country code", "country"); edit("Nationality", "nationality")
            edit("City / birthplace", "city"); edit("Appearance", "appearance")
        } }
        item { GameCard("Personal development") {
            edit("Health (0–100)", "health"); edit("Intelligence (0–100)", "intelligence")
            edit("Social ability (0–100)", "charisma"); edit("Talent (0–100)", "talent")
            edit("Happiness (0–100)", "happiness")
            edit("Personality", "personality"); edit("Skills", "skills"); edit("Interests", "interests")
        } }
        item { GameCard("Education & qualifications") {
            edit("School", "school"); edit("University", "university")
            edit("Degree", "degree"); edit("Major", "major")
            edit("Education stage (None / Secondary / Bachelor / Master / Doctorate)", "educationStage")
        } }
        item { GameCard("Career & military service") {
            edit("Occupation", "occupation"); edit("Employer", "employer")
            edit("Professional rank / workplace position", "rank")
            edit("Monthly salary (${WorldCatalog.country(person.countryCode).currency})", "salary")
            edit("Military organization / branch", "branch"); edit("Military rank", "militaryRank")
        } }
        item { GameCard("Finances · amounts in main currency units") {
            edit("Cash / bank balance", "cash"); edit("Asset value", "assets")
            edit("Debt", "debt"); edit("Monthly living costs", "living")
            Text("Salary and expenses are stored as integer minor units for exact monthly accounting.", color = Mist)
        } }
        item { GameCard("Commit edits") {
            Button(enabled = !busy, onClick = {
                val edited = parsePerson()
                if (edited == null) onError("Please enter valid integer attributes and valid currency amounts (up to 2 decimal places).")
                else onSave(edited, allowOverride, onDone)
            }, modifier = Modifier.fillMaxWidth()) { Text("Save family member changes") }
        } }
        item { GameCard("Add another family member") {
            Box { OutlinedButton(onClick = { showRelationDropdown = true }, modifier = Modifier.fillMaxWidth()) { Text("Relationship: $newRelation") }
                DropdownMenu(expanded = showRelationDropdown, onDismissRequest = { showRelationDropdown = false }) {
                    relationOptions.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = {
                        newRelation = option; newYear = suggestedYear(option).toString(); showRelationDropdown = false
                    }) }
                }
            }
            EditText("Full name", newName) { newName = it }
            EditText("Birth year", newYear) { newYear = it }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = overrideFamilyAge, onCheckedChange = { overrideFamilyAge = it })
                Text("Allow deliberate family-age override")
            }
            MenuButton("Create and link relative", enabled = !busy && newName.isNotBlank() && newYear.toIntOrNull() != null,
                onClick = { onAddRelative(person.id, newRelation, newName, newYear.toInt(), overrideFamilyAge, onDone) })
            Text("Some extended relations require a connecting ancestor, parent, or sibling; the editor creates that relationship explicitly.", color = Mist)
        } }
        item { GameCard("Edit directly connected relationship quality") {
            val relatives = state.relationships.filter { it.fromId == person.id || it.toId == person.id }
            if (relatives.isEmpty()) Text("No direct relationship edges found.", color = Mist)
            relatives.forEach { link ->
                val other = state.characters.firstOrNull { it.id == (if (link.fromId == person.id) link.toId else link.fromId) }
                var quality by remember(link.id, link.quality) { mutableFloatStateOf(link.quality.toFloat()) }
                Text("${other?.firstName ?: "Unknown"} · ${link.kind}")
                Slider(value = quality, onValueChange = { quality = it }, valueRange = 0f..100f, steps = 99)
                MenuButton("Save relationship quality: ${quality.toInt()}", enabled = !busy,
                    onClick = { onQuality(link.id, quality.toInt()) })
            }
        } }
        item { GameCard("Add an education, career or military history record") {
            Text("Category")
            listOf("EDUCATION", "EMPLOYMENT", "MILITARY", "PERSONAL", "FINANCE").forEach { category ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = recordCategory == category, onClick = { recordCategory = category })
                    Text(category)
                }
            }
            EditText("History title (degree, rank, position, etc.)", recordTitle) { recordTitle = it }
            EditText("School, university, employer, service branch", recordOrg) { recordOrg = it }
            EditText("Start year", recordStart) { recordStart = it }
            EditText("End year (blank if current)", recordEnd) { recordEnd = it }
            EditText("Description / qualifications", recordDetails) { recordDetails = it }
            MenuButton("Add persistent history record", enabled = !busy && recordTitle.isNotBlank(), onClick = {
                val start = recordStart.toIntOrNull()
                val end = if (recordEnd.isBlank()) null else recordEnd.toIntOrNull()
                if (start == null || (recordEnd.isNotBlank() && end == null)) onError("Enter valid history years.")
                else onHistory(person.id, recordCategory, recordTitle, recordOrg, start, end, recordDetails)
            })
        } }
        item { GameCard("Previous education, career and military records") {
            if (records.isEmpty()) Text("No structured history records yet.", color = Mist)
            records.forEach { record ->
                Text("${record.category}: ${record.title} (${record.startYear}–${record.endYear ?: "present"})")
                Text("${record.organization} · ${record.details}", color = Mist, style = MaterialTheme.typography.bodySmall)
            }
        } }
    }
}

private fun moneyInput(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()
private fun parseCents(text: String): Long = BigDecimal(text.trim()).movePointRight(2).longValueExact()
