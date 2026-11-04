package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteLink
import com.example.data.model.Tag
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class GraphNode(
    val id: String,
    val title: String,
    val folderId: String?,
    val degree: Int
)

@Composable
fun KnowledgeGraphScreen(
    notes: List<Note>,
    links: List<NoteLink>,
    folders: List<Folder>,
    tags: List<Tag>,
    noteTagsMap: Map<String, List<String>>,
    onSelectNote: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFolderFilter by remember { mutableStateOf<String?>(null) }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }

    // Active nodes excluding trash
    val activeNotes = remember(notes) { notes.filter { !it.inTrash } }
    val noteMap = remember(activeNotes) { activeNotes.associateBy { it.id } }

    // Calculate degrees for nodes
    val nodeDegrees = remember(activeNotes, links) {
        val degrees = mutableMapOf<String, Int>()
        links.forEach { link ->
            if (noteMap.containsKey(link.sourceNoteId) && noteMap.containsKey(link.targetNoteId)) {
                degrees[link.sourceNoteId] = (degrees[link.sourceNoteId] ?: 0) + 1
                degrees[link.targetNoteId] = (degrees[link.targetNoteId] ?: 0) + 1
            }
        }
        degrees
    }

    // Filter notes
    val filteredNotes = remember(activeNotes, searchQuery, selectedFolderFilter, selectedTagFilter, noteTagsMap) {
        activeNotes.filter { note ->
            val matchesSearch = searchQuery.isBlank() || note.title.contains(searchQuery, ignoreCase = true)
            val matchesFolder = selectedFolderFilter == null || note.folderId == selectedFolderFilter
            val matchesTag = selectedTagFilter == null || (noteTagsMap[note.id]?.contains(selectedTagFilter) == true)
            matchesSearch && matchesFolder && matchesTag
        }
    }

    val filteredNoteIds = remember(filteredNotes) { filteredNotes.map { it.id }.toSet() }

    // Filtered links
    val filteredLinks = remember(links, filteredNoteIds) {
        links.filter { it.sourceNoteId in filteredNoteIds && it.targetNoteId in filteredNoteIds }
    }

    // Node Positions map (noteId -> Offset)
    val nodePositions = remember { mutableStateMapOf<String, Offset>() }

    // Canvas Transformation
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Initial positioning: layout in a circular / grouped galaxy
    LaunchedEffect(filteredNotes) {
        val total = filteredNotes.size
        if (total == 0) return@LaunchedEffect

        val radiusStep = 180f
        var currentRadius = 250f
        var angle = 0.0

        filteredNotes.forEachIndexed { index, note ->
            if (!nodePositions.containsKey(note.id)) {
                val degree = nodeDegrees[note.id] ?: 0
                // Nodes with high degrees placed closer to center
                val r = if (degree >= 2) 180f + (index % 5) * 40f else 320f + (index * 35f) % 400f
                val theta = (index.toDouble() / total) * 2 * Math.PI
                val x = 500f + (r * cos(theta)).toFloat()
                val y = 700f + (r * sin(theta)).toFloat()
                nodePositions[note.id] = Offset(x, y)
            }
        }
    }

    val selectedNote = remember(selectedNodeId, noteMap) {
        selectedNodeId?.let { noteMap[it] }
    }

    val connectedToSelected = remember(selectedNodeId, filteredLinks) {
        if (selectedNodeId == null) emptySet()
        else {
            val s = mutableSetOf<String>()
            filteredLinks.forEach { link ->
                if (link.sourceNoteId == selectedNodeId) s.add(link.targetNoteId)
                if (link.targetNoteId == selectedNodeId) s.add(link.sourceNoteId)
            }
            s
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.knowledgeGraph,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = "${filteredNotes.size} nodes • ${filteredLinks.size} connections",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
            }

            IconButton(
                onClick = {
                    scale = 1.0f
                    offset = Offset.Zero
                }
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom", tint = colors.textSecondary)
            }
        }

        // Search Bar & Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                placeholder = { Text(strings.searchGraph, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.cardBackground,
                    unfocusedContainerColor = colors.cardBackground,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Folder & Tag Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            folders.forEach { folder ->
                val isSelected = selectedFolderFilter == folder.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedFolderFilter = if (isSelected) null else folder.id
                    },
                    label = { Text(folder.name, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                        selectedLabelColor = colors.accent,
                        containerColor = colors.cardBackground,
                        labelColor = colors.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) colors.accent else colors.border,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        // Interactive Graph Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.background)
        ) {
            var draggedNodeId by remember { mutableStateOf<String?>(null) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(filteredNotes) {
                        detectTapGestures { tapOffset ->
                            // Convert tap to canvas space
                            val canvasTap = (tapOffset - offset) / scale
                            // Check if tapped near any node
                            var hitNodeId: String? = null
                            for (note in filteredNotes) {
                                val pos = nodePositions[note.id] ?: continue
                                val dist = hypot(canvasTap.x - pos.x, canvasTap.y - pos.y)
                                if (dist <= 30f) {
                                    hitNodeId = note.id
                                    break
                                }
                            }
                            selectedNodeId = hitNodeId
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.3f, 3.5f)
                            offset += pan
                        }
                    }
                    .pointerInput(filteredNotes) {
                        detectDragGestures(
                            onDragStart = { startPos ->
                                val canvasPos = (startPos - offset) / scale
                                for (note in filteredNotes) {
                                    val pos = nodePositions[note.id] ?: continue
                                    if (hypot(canvasPos.x - pos.x, canvasPos.y - pos.y) <= 30f) {
                                        draggedNodeId = note.id
                                        selectedNodeId = note.id
                                        break
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val targetId = draggedNodeId
                                if (targetId != null) {
                                    val current = nodePositions[targetId] ?: Offset(500f, 500f)
                                    nodePositions[targetId] = current + (dragAmount / scale)
                                } else {
                                    offset += dragAmount
                                }
                            },
                            onDragEnd = { draggedNodeId = null },
                            onDragCancel = { draggedNodeId = null }
                        )
                    }
            ) {
                val accentColor = colors.accent
                val borderColor = colors.border
                val textPrimary = colors.textPrimary.toArgb()
                val textSecondary = colors.textSecondary.toArgb()

                // Draw Edges (Links)
                filteredLinks.forEach { link ->
                    val startPos = nodePositions[link.sourceNoteId]
                    val endPos = nodePositions[link.targetNoteId]

                    if (startPos != null && endPos != null) {
                        val isHighlighted = selectedNodeId != null &&
                                (link.sourceNoteId == selectedNodeId || link.targetNoteId == selectedNodeId)

                        val lineColor = if (isHighlighted) accentColor else borderColor.copy(alpha = 0.5f)
                        val strokeWidth = if (isHighlighted) 3.5f * scale else 1.5f * scale

                        val transformedStart = (startPos * scale) + offset
                        val transformedEnd = (endPos * scale) + offset

                        drawLine(
                            color = lineColor,
                            start = transformedStart,
                            end = transformedEnd,
                            strokeWidth = strokeWidth
                        )
                    }
                }

                // Draw Nodes
                val textPaint = Paint().apply {
                    color = textPrimary
                    textSize = 11f * density * scale.coerceIn(0.7f, 1.4f)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                filteredNotes.forEach { note ->
                    val pos = nodePositions[note.id] ?: return@forEach
                    val transformedPos = (pos * scale) + offset

                    val degree = nodeDegrees[note.id] ?: 0
                    val isSelected = note.id == selectedNodeId
                    val isConnected = selectedNodeId != null && note.id in connectedToSelected

                    // Node radius
                    val baseRadius = when {
                        degree >= 4 -> 16f
                        degree >= 2 -> 12f
                        degree >= 1 -> 9f
                        else -> 7f
                    }
                    val radius = baseRadius * scale

                    val nodeFill = when {
                        isSelected -> accentColor
                        isConnected -> accentColor.copy(alpha = 0.8f)
                        degree >= 3 -> Color(0xFFDCDDE0)
                        degree >= 1 -> Color(0xFF90939A)
                        else -> Color(0xFF484A50)
                    }

                    // Outer halo for selected or hub nodes
                    if (isSelected || degree >= 4) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.25f),
                            radius = radius + 6f * scale,
                            center = transformedPos
                        )
                    }

                    // Node body
                    drawCircle(
                        color = nodeFill,
                        radius = radius,
                        center = transformedPos
                    )

                    // Node border
                    drawCircle(
                        color = colors.background,
                        radius = radius,
                        center = transformedPos,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * scale)
                    )

                    // Node label
                    val label = note.title.ifBlank { "Untitled" }
                    val shortLabel = if (label.length > 18) label.take(16) + "…" else label

                    drawContext.canvas.nativeCanvas.drawText(
                        shortLabel,
                        transformedPos.x,
                        transformedPos.y + radius + (14f * scale),
                        textPaint
                    )
                }
            }

            // Bottom Floating Card for Selected Node
            if (selectedNote != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.accent.copy(alpha = 0.5f))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedNote.title.ifBlank { "Untitled Note" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${nodeDegrees[selectedNote.id] ?: 0} ${strings.connectionsCount}",
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.accent)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.accent,
                            modifier = Modifier
                                .clickable { onSelectNote(selectedNote.id) }
                                .padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Open Note",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = colors.background,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        IconButton(
                            onClick = { selectedNodeId = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
