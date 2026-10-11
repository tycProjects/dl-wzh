package dev.tajim.jarvis.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.tajim.jarvis.AppContainer
import dev.tajim.jarvis.data.Memory
import dev.tajim.jarvis.ui.components.GlassCard
import dev.tajim.jarvis.ui.components.HoloButton
import dev.tajim.jarvis.ui.settings.SectionCard
import dev.tajim.jarvis.ui.settings.SectionLabel
import dev.tajim.jarvis.ui.settings.SettingsPage
import dev.tajim.jarvis.ui.settings.ToggleRow
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(private val container: AppContainer) : ViewModel() {
    val items = container.memoryRepository.items
    val useMemories = container.settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), dev.tajim.jarvis.data.UserSettings())
    fun add(t: String) { viewModelScope.launch { container.memoryRepository.add(t) } }
    fun edit(id: Long, t: String) { viewModelScope.launch { container.memoryRepository.edit(id, t) } }
    fun delete(id: Long) { viewModelScope.launch { container.memoryRepository.delete(id) } }
    fun clear() { viewModelScope.launch { container.memoryRepository.clear() } }
    fun setUse(v: Boolean) { viewModelScope.launch { container.settingsRepository.setUseMemories(v) } }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory { initializer { MemoryViewModel(c) } }
    }
}

/** Memory Bank: only what you add here is stored. Nothing is saved silently. Plain text list with simple search. */
@Composable
fun MemoryScreen(vm: MemoryViewModel, onBack: () -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    val s by vm.useMemories.collectAsStateWithLifecycle()
    val c = LocalJarvisColors.current
    var query by remember { mutableStateOf("") }
    var newText by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Memory?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val shown = remember(items, query) { if (query.isBlank()) items else items.filter { it.text.contains(query, ignoreCase = true) } }

    SettingsPage("Memory Bank", onBack) {
        SectionCard {
            ToggleRow("Use memories in AI replies", "Sends your saved notes to the AI so it can use them.", s.useMemories, vm::setUse)
        }
        SectionLabel("Add a memory")
        SectionCard {
            OutlinedTextField(newText, { newText = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Something JARVIS should remember") })
            HoloButton("Add", { vm.add(newText); newText = "" }, enabled = newText.isNotBlank())
        }
        SectionLabel("Saved (${items.size})")
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search memories") }, singleLine = true)
        shown.forEach { m ->
            GlassCard(Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
                Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(m.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                    TextButton(onClick = { editing = m }) { Text("Edit") }
                    TextButton(onClick = { vm.delete(m.id) }) { Text("Delete") }
                }
            }
        }
        if (items.isNotEmpty()) {
            HoloButton("Clear all memories", { confirmClear = true }, Modifier.padding(top = Spacing.lg))
        }
    }

    editing?.let { m ->
        var t by remember(m.id) { mutableStateOf(m.text) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit memory") },
            text = { OutlinedTextField(t, { t = it.take(500) }, Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { vm.edit(m.id, t); editing = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all memories?") },
            text = { Text("This cannot be undone. Chat history is not affected.") },
            confirmButton = { TextButton(onClick = { vm.clear(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}
