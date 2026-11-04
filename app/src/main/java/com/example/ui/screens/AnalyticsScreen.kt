package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Attachment
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteLink
import com.example.data.model.Tag
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings
import com.example.utils.TextFormatUtils
import java.util.Locale

@Composable
fun AnalyticsScreen(
    notes: List<Note>,
    folders: List<Folder>,
    tags: List<Tag>,
    links: List<NoteLink>,
    attachments: List<Attachment>,
    noteTagsMap: Map<String, List<String>>,
    onOpenDrawer: () -> Unit
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    val activeNotes = remember(notes) { notes.filter { !it.inTrash } }

    val totalWords = remember(activeNotes) {
        activeNotes.sumOf { TextFormatUtils.getWordCount(it.content) + TextFormatUtils.getWordCount(it.title) }
    }
    val totalChars = remember(activeNotes) {
        activeNotes.sumOf { it.content.length + it.title.length }
    }

    // Top folders by note count
    val folderCounts = remember(activeNotes, folders) {
        val counts = mutableMapOf<String?, Int>()
        activeNotes.forEach { note ->
            counts[note.folderId] = (counts[note.folderId] ?: 0) + 1
        }
        val folderNameMap = folders.associate { it.id to it.name }
        counts.map { (fId, count) ->
            val name = if (fId != null) folderNameMap[fId] ?: "Unknown" else "Root (No Folder)"
            Pair(name, count)
        }.sortedByDescending { it.second }.take(5)
    }

    // Top tags by note count
    val tagCounts = remember(noteTagsMap, tags) {
        val counts = mutableMapOf<String, Int>()
        noteTagsMap.values.flatten().forEach { tagId ->
            counts[tagId] = (counts[tagId] ?: 0) + 1
        }
        val tagNameMap = tags.associate { it.id to it.name }
        counts.mapNotNull { (tagId, count) ->
            val name = tagNameMap[tagId] ?: return@mapNotNull null
            Pair(name, count)
        }.sortedByDescending { it.second }.take(6)
    }

    // Most connected hub notes
    val mostLinkedNotes = remember(activeNotes, links) {
        val counts = mutableMapOf<String, Int>()
        links.forEach { link ->
            counts[link.sourceNoteId] = (counts[link.sourceNoteId] ?: 0) + 1
            counts[link.targetNoteId] = (counts[link.targetNoteId] ?: 0) + 1
        }
        val noteTitleMap = activeNotes.associate { it.id to it.title }
        counts.mapNotNull { (noteId, count) ->
            val title = noteTitleMap[noteId] ?: return@mapNotNull null
            Pair(title.ifBlank { "Untitled Note" }, count)
        }.sortedByDescending { it.second }.take(5)
    }

    // Notes created by day (last 7 days activity)
    val creationTimeline = remember(activeNotes) {
        val map = mutableMapOf<String, Int>()
        activeNotes.forEach { note ->
            val dateKey = TextFormatUtils.formatDateKey(note.createdAt)
            map[dateKey] = (map[dateKey] ?: 0) + 1
        }
        map.entries.sortedByDescending { it.key }.take(7).reversed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.analyticsTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = "Offline Knowledge Metrics & Topology",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Overview Grid (2x3 cards)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Notes",
                            value = "${activeNotes.size}",
                            icon = Icons.Default.Description
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Folders",
                            value = "${folders.size}",
                            icon = Icons.Default.Folder
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = strings.totalWords,
                            value = String.format(Locale.US, "%,d", totalWords),
                            icon = Icons.Default.Description
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = strings.totalCharacters,
                            value = String.format(Locale.US, "%,d", totalChars),
                            icon = Icons.Default.Description
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = strings.totalLinks,
                            value = "${links.size}",
                            icon = Icons.Default.Link
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = strings.totalAttachments,
                            value = "${attachments.size}",
                            icon = Icons.Default.AttachFile
                        )
                    }
                }
            }

            // Most active folders
            item {
                SectionCard(title = strings.activeFoldersRanking) {
                    if (folderCounts.isEmpty()) {
                        Text("No active folders", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    } else {
                        val maxCount = folderCounts.maxOfOrNull { it.second } ?: 1
                        folderCounts.forEach { (name, count) ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                    Text("$count notes", style = MaterialTheme.typography.labelMedium, color = colors.accent)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (count.toFloat() / maxCount).coerceIn(0.05f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = colors.accent,
                                    trackColor = colors.background,
                                )
                            }
                        }
                    }
                }
            }

            // Top Tags
            item {
                SectionCard(title = strings.popularTags) {
                    if (tagCounts.isEmpty()) {
                        Text("No tagged notes", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tagCounts.forEach { (name, count) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.background
                                ) {
                                    Text(
                                        text = "#$name ($count)",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Most Linked Notes (Hubs)
            item {
                SectionCard(title = strings.hubNotes) {
                    if (mostLinkedNotes.isEmpty()) {
                        Text("No inter-note links yet. Use [[Note Title]] in your writing.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    } else {
                        mostLinkedNotes.forEach { (title, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.background
                                ) {
                                    Text(
                                        text = "$count links",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.accent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Note Creation Timeline
            item {
                SectionCard(title = strings.creationTimeline) {
                    if (creationTimeline.isEmpty()) {
                        Text("No timeline entries recorded yet.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    } else {
                        val maxDay = creationTimeline.maxOfOrNull { it.value } ?: 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            creationTimeline.forEach { entry ->
                                val fraction = (entry.value.toFloat() / maxDay).coerceIn(0.1f, 1f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = "${entry.value}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.accent,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height((50 * fraction).dp)
                                            .background(colors.accent, RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = entry.key.takeLast(5),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.textSecondary,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector
) {
    val colors = LocalTumbaColors.current

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.border)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 20.sp
                )
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = LocalTumbaColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.border)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
