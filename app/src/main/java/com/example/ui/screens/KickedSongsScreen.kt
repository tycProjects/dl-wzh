package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.KickedSong
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KickedSongsScreen(
    kickedSongs: List<KickedSong>,
    onBack: () -> Unit,
    onRestoreSong: (Long) -> Unit,
    onUpdateSong: (KickedSong) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JnmfTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Dialog states
    var songForInfo by remember { mutableStateOf<KickedSong?>(null) }
    var songForEdit by remember { mutableStateOf<KickedSong?>(null) }

    val filteredList = remember(kickedSongs, searchQuery) {
        if (searchQuery.isBlank()) {
            kickedSongs
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            kickedSongs.filter {
                it.title.lowercase(Locale.ROOT).contains(q) ||
                        it.artist.lowercase(Locale.ROOT).contains(q) ||
                        it.album.lowercase(Locale.ROOT).contains(q)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = JnmfDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kicked Songs",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.bright,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${kickedSongs.size} hidden from JNMF • Safe (Files kept on phone)",
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
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.testTag("kicked_search_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search Kicked Songs",
                            tint = if (isSearchActive) JnmfWarningYellow else colors.primary
                        )
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
            // Search Bar
            AnimatedVisibility(visible = isSearchActive) {
                Surface(
                    color = JnmfSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, colors.borderGlow, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by title, artist, or album...", fontSize = 13.sp, color = JnmfTextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = JnmfTextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kicked_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedTextColor = JnmfTextPrimary,
                            unfocusedTextColor = JnmfTextPrimary
                        )
                    )
                }
            }

            // Info Banner stating: Original music files remain intact, NO delete button here
            Surface(
                color = JnmfSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(0.5.dp, JnmfBorder, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Kicked songs are hidden from JNMF playback. Files remain intact on device storage. Permanent deletion is disabled here for safety.",
                        fontSize = 11.sp,
                        color = JnmfTextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = JnmfTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No kicked songs matching \"$searchQuery\"" else "No Kicked Songs",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = JnmfTextSecondary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "To hide unwanted songs from JNMF, tap 'Kick Song' in the Library song menu.",
                            color = JnmfTextMuted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(filteredList, key = { it.id }) { kickedSong ->
                        KickedSongItemCard(
                            kickedSong = kickedSong,
                            onRestore = { onRestoreSong(kickedSong.id) },
                            onEdit = { songForEdit = kickedSong },
                            onInfo = { songForInfo = kickedSong }
                        )
                    }
                }
            }
        }
    }

    // Song Info Modal
    songForInfo?.let { song ->
        KickedSongInfoDialog(
            kickedSong = song,
            onDismiss = { songForInfo = null }
        )
    }

    // Edit Info Modal
    songForEdit?.let { song ->
        KickedSongEditDialog(
            kickedSong = song,
            onDismiss = { songForEdit = null },
            onSave = { updated ->
                onUpdateSong(updated)
                songForEdit = null
            }
        )
    }
}

@Composable
private fun KickedSongItemCard(
    kickedSong: KickedSong,
    onRestore: () -> Unit,
    onEdit: () -> Unit,
    onInfo: () -> Unit
) {
    val colors = JnmfTheme.colors
    val kickedDateStr = remember(kickedSong.dateKicked) {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
        sdf.format(Date(kickedSong.dateKicked))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(0.5.dp, JnmfBorder, RoundedCornerShape(10.dp)),
        color = JnmfSurfaceVariant.copy(alpha = 0.75f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JnmfDarkBg)
                        .border(1.dp, JnmfBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Kicked",
                        tint = JnmfWarningYellow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = kickedSong.title,
                        fontWeight = FontWeight.Bold,
                        color = JnmfTextPrimary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${kickedSong.artist} • ${kickedSong.album}",
                        color = JnmfTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // BPM Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(JnmfDarkBg)
                        .border(0.5.dp, JnmfBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${kickedSong.bpm} BPM",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File metadata info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kicked: $kickedDateStr • ${kickedSong.formattedDuration} • ${kickedSong.formattedFileSize}",
                    fontSize = 11.sp,
                    color = JnmfTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!kickedSong.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: ${kickedSong.notes}",
                    fontSize = 11.sp,
                    color = JnmfWarningYellow,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Restore to JNMF, Edit information, Song information (NO DELETE FUNCTION)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info button
                OutlinedButton(
                    onClick = onInfo,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, JnmfBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JnmfTextSecondary)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Info", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Edit button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, JnmfBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JnmfTextSecondary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Restore to JNMF button
                Button(
                    onClick = onRestore,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = JnmfDarkBg
                    )
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore to JNMF", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun KickedSongInfoDialog(
    kickedSong: KickedSong,
    onDismiss: () -> Unit
) {
    val colors = JnmfTheme.colors
    val kickedDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(kickedSong.dateKicked))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = JnmfSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.borderGlow, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = colors.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SONG SPECIFICATION",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.bright,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                InfoRow("Title", kickedSong.title)
                InfoRow("Artist", kickedSong.artist)
                InfoRow("Album", kickedSong.album)
                InfoRow("Duration", kickedSong.formattedDuration)
                InfoRow("BPM", "${kickedSong.bpm} (${kickedSong.beatCategory})")
                InfoRow("File Size", kickedSong.formattedFileSize)
                InfoRow("Kicked At", kickedDateStr)
                InfoRow("Path", kickedSong.filePath ?: "MediaStore Content URI")
                if (!kickedSong.notes.isNullOrBlank()) {
                    InfoRow("Notes", kickedSong.notes)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JnmfSurface,
                        contentColor = colors.primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
                ) {
                    Text("Close", fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = JnmfTextMuted,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = JnmfTextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun KickedSongEditDialog(
    kickedSong: KickedSong,
    onDismiss: () -> Unit,
    onSave: (KickedSong) -> Unit
) {
    val colors = JnmfTheme.colors
    var title by remember { mutableStateOf(kickedSong.title) }
    var artist by remember { mutableStateOf(kickedSong.artist) }
    var album by remember { mutableStateOf(kickedSong.album) }
    var notes by remember { mutableStateOf(kickedSong.notes ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = JnmfSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.borderGlow, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "EDIT KICKED SONG INFO",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.bright,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = JnmfBorder,
                        focusedTextColor = JnmfTextPrimary,
                        unfocusedTextColor = JnmfTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = JnmfBorder,
                        focusedTextColor = JnmfTextPrimary,
                        unfocusedTextColor = JnmfTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = JnmfBorder,
                        focusedTextColor = JnmfTextPrimary,
                        unfocusedTextColor = JnmfTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reason", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = JnmfBorder,
                        focusedTextColor = JnmfTextPrimary,
                        unfocusedTextColor = JnmfTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                        onClick = {
                            val updated = kickedSong.copy(
                                title = title.trim().ifEmpty { kickedSong.title },
                                artist = artist.trim().ifEmpty { kickedSong.artist },
                                album = album.trim().ifEmpty { kickedSong.album },
                                notes = notes.trim().ifEmpty { null }
                            )
                            onSave(updated)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = JnmfDarkBg
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
