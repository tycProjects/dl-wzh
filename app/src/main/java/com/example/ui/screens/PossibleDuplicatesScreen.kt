package com.example.ui.screens

import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DuplicateConfidence
import com.example.model.PossibleDuplicateGroup
import com.example.model.SimilarityLevel
import com.example.model.Song
import com.example.ui.theme.JnmfAlertRed
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfBorderGlow
import com.example.ui.theme.JnmfDarkBg
import com.example.ui.theme.JnmfSurface
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfSurfaceVariant
import com.example.ui.theme.JnmfTextMuted
import com.example.ui.theme.JnmfTextPrimary
import com.example.ui.theme.JnmfTextSecondary
import com.example.ui.theme.JnmfTheme
import com.example.ui.theme.JnmfWarningYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PossibleDuplicatesScreen(
    duplicates: List<PossibleDuplicateGroup>,
    isScanning: Boolean,
    onBack: () -> Unit,
    onRescan: () -> Unit,
    onPreviewSong: (Song) -> Unit,
    onDeleteSongPermanently: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JnmfTheme.colors
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = JnmfDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Possible Duplicates",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.bright,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${duplicates.size} candidate pairs found • Conservative analysis",
                            fontSize = 11.sp,
                            color = JnmfTextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRescan,
                        enabled = !isScanning,
                        modifier = Modifier.testTag("rescan_duplicates_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan",
                                tint = colors.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JnmfSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Safety Header Card
            Surface(
                color = JnmfSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .border(1.dp, JnmfBorderGlow, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = JnmfWarningYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SAFETY FIRST: NO AUTOMATIC DELETION",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = JnmfWarningYellow
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "JNMF compares Title, Beat/BPM, Duration, and Metadata. You must inspect both copies. Deleting a copy permanently removes that specific file from device storage.",
                        fontSize = 11.sp,
                        color = JnmfTextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            if (duplicates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Duplicates Found",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.bright,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your music library is clean. No multi-signal duplicate candidates detected.",
                            color = JnmfTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        OutlinedButton(
                            onClick = onRescan,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
                        ) {
                            Text("Run Analysis Again", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {
                    items(duplicates, key = { it.groupId + it.songA.id + it.songB.id }) { group ->
                        DuplicateGroupCard(
                            group = group,
                            onPreviewSong = onPreviewSong,
                            onRequestDelete = { songToDelete = it }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    // Permanent Deletion Safety Dialog
    songToDelete?.let { song ->
        PermanentDeleteConfirmationDialog(
            song = song,
            onDismiss = { songToDelete = null },
            onConfirmDelete = {
                onDeleteSongPermanently(song)
                songToDelete = null
            }
        )
    }
}

@Composable
private fun DuplicateGroupCard(
    group: PossibleDuplicateGroup,
    onPreviewSong: (Song) -> Unit,
    onRequestDelete: (Song) -> Unit
) {
    val colors = JnmfTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JnmfBorder, RoundedCornerShape(12.dp)),
        color = JnmfSurface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Group Name + Confidence Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.groupId,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    fontSize = 14.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(group.confidence.badgeColor.copy(alpha = 0.2f))
                        .border(1.dp, group.confidence.badgeColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = group.confidence.label.uppercase(Locale.ROOT),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = group.confidence.badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = group.detectedReason,
                fontSize = 11.sp,
                color = JnmfTextMuted,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Song A Card
            SongCandidateCard(
                label = "Song A",
                song = group.songA,
                onPreview = { onPreviewSong(group.songA) },
                onDelete = { onRequestDelete(group.songA) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Song B Card
            SongCandidateCard(
                label = "Song B",
                song = group.songB,
                onPreview = { onPreviewSong(group.songB) },
                onDelete = { onRequestDelete(group.songB) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Similarity Metrics Matrix
            Surface(
                color = JnmfDarkBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, JnmfBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "SIMILARITY BREAKDOWN:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = JnmfTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SimilarityChip("Name", group.nameSimilarity.label)
                        SimilarityChip("Beat/BPM", group.beatSimilarity.label)
                        SimilarityChip("Duration", group.durationSimilarity)
                        SimilarityChip("Metadata", group.metadataSimilarity.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun SongCandidateCard(
    label: String,
    song: Song,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = JnmfTheme.colors
    Surface(
        color = JnmfSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, JnmfBorder, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[$label]",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.bright,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    color = JnmfTextPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${song.artist} • ${song.formattedDuration} • ${song.bpm} BPM • ${song.formattedFileSize}",
                fontSize = 11.sp,
                color = JnmfTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!song.filePath.isNullOrEmpty()) {
                Text(
                    text = song.filePath,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JnmfTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions: Preview audio or Delete copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Listen", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onDelete,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JnmfAlertRed.copy(alpha = 0.85f),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SimilarityChip(label: String, value: String) {
    val colors = JnmfTheme.colors
    Column {
        Text(text = label, fontSize = 9.sp, color = JnmfTextMuted, fontFamily = FontFamily.Monospace)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primary)
    }
}

@Composable
private fun PermanentDeleteConfirmationDialog(
    song: Song,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val colors = JnmfTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = JnmfSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, JnmfAlertRed, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = JnmfAlertRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PERMANENT FILE DELETION",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = JnmfAlertRed,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "You are about to permanently delete this music file from your device:",
                    fontSize = 13.sp,
                    color = JnmfTextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = JnmfDarkBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, JnmfAlertRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = song.title,
                            fontWeight = FontWeight.Bold,
                            color = colors.bright,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${song.artist} • ${song.formattedDuration} • ${song.formattedFileSize}",
                            color = JnmfTextSecondary,
                            fontSize = 12.sp
                        )
                        if (!song.filePath.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Path: ${song.filePath}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = JnmfTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "⚠️ WARNING: This action CANNOT be undone. The selected file will be permanently removed from physical storage. The other duplicate copy will remain intact.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JnmfAlertRed,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JnmfTextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onConfirmDelete,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_permanent_delete_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JnmfAlertRed,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        Text("Delete File", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
