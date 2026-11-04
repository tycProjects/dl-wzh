package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JournalEntry
import com.example.data.model.Note
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings
import com.example.utils.TextFormatUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    notes: List<Note>,
    journalEntries: List<JournalEntry>,
    onSelectNote: (String) -> Unit,
    onOpenDailyJournal: (String) -> Unit,
    onCreateNoteForDate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    val calendar = remember { Calendar.getInstance() }
    var displayedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) } // 0-based
    var selectedDateKey by remember { mutableStateOf(TextFormatUtils.formatDateKey(System.currentTimeMillis())) }
    var isWeekView by remember { mutableStateOf(false) }

    val todayDateKey = remember { TextFormatUtils.formatDateKey(System.currentTimeMillis()) }

    // Map notes to dateKey based on updatedAt / createdAt
    val notesByDate = remember(notes) {
        val map = mutableMapOf<String, MutableList<Note>>()
        notes.filter { !it.inTrash }.forEach { note ->
            val createdKey = TextFormatUtils.formatDateKey(note.createdAt)
            map.getOrPut(createdKey) { mutableListOf() }.add(note)
            val updatedKey = TextFormatUtils.formatDateKey(note.updatedAt)
            if (updatedKey != createdKey) {
                map.getOrPut(updatedKey) { mutableListOf() }.add(note)
            }
        }
        map
    }

    val selectedDayNotes = remember(selectedDateKey, notesByDate) {
        notesByDate[selectedDateKey]?.distinctBy { it.id } ?: emptyList()
    }

    val hasJournalEntry = remember(selectedDateKey, journalEntries) {
        journalEntries.any { it.dateKey == selectedDateKey }
    }

    val monthName = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Header
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
                    text = strings.calendar,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
            }

            // Return to Today button
            IconButton(
                onClick = {
                    val cal = Calendar.getInstance()
                    displayedYear = cal.get(Calendar.YEAR)
                    displayedMonth = cal.get(Calendar.MONTH)
                    selectedDateKey = todayDateKey
                }
            ) {
                Icon(
                    Icons.Default.Today,
                    contentDescription = strings.today,
                    tint = if (selectedDateKey == todayDateKey) colors.accent else colors.textSecondary
                )
            }

            // View toggle (Month / Week)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardBackground,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { isWeekView = !isWeekView }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isWeekView) strings.weekView else strings.monthView,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // Month Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (displayedMonth == 0) {
                        displayedMonth = 11
                        displayedYear--
                    } else {
                        displayedMonth--
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", tint = colors.textSecondary)
            }

            Text(
                text = monthName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            )

            IconButton(
                onClick = {
                    if (displayedMonth == 11) {
                        displayedMonth = 0
                        displayedYear++
                    } else {
                        displayedMonth++
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = colors.textSecondary)
            }
        }

        // Days of week header (Mon - Sun)
        val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            dayHeaders.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Calendar Grid
        CalendarMonthGrid(
            year = displayedYear,
            month = displayedMonth,
            isWeekOnly = isWeekView,
            selectedDateKey = selectedDateKey,
            todayDateKey = todayDateKey,
            notesByDate = notesByDate,
            onSelectDate = { dateKey -> selectedDateKey = dateKey }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Date Details & Notes List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Daily Journal Hero Card for Selected Date
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDailyJournal(selectedDateKey) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasJournalEntry) colors.panelBackground else colors.cardBackground
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (hasJournalEntry) colors.accent.copy(alpha = 0.5f) else colors.border
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (hasJournalEntry) colors.accent.copy(alpha = 0.2f) else colors.background,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                tint = if (hasJournalEntry) colors.accent else colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.dailyJournal,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = if (hasJournalEntry) "Recorded journal entry available" else strings.noJournalForDate,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (hasJournalEntry) colors.accent else colors.textSecondary
                                )
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.background,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = if (hasJournalEntry) "View" else "Create",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.accent,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Notes header for selected date
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.notesOnDate} (${selectedDayNotes.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    IconButton(
                        onClick = { onCreateNoteForDate(selectedDateKey) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New note for date",
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (selectedDayNotes.isEmpty()) {
                item {
                    Text(
                        text = strings.noNotesOnDate,
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(selectedDayNotes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectNote(note.id) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(colors.border)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = note.title.ifBlank { strings.untitledNote },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (note.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = note.content.take(120).replace("\n", " "),
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = TextFormatUtils.formatDate(note.updatedAt),
                                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary.copy(alpha = 0.7f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarMonthGrid(
    year: Int,
    month: Int,
    isWeekOnly: Boolean,
    selectedDateKey: String,
    todayDateKey: String,
    notesByDate: Map<String, List<Note>>,
    onSelectDate: (String) -> Unit
) {
    val colors = LocalTumbaColors.current

    val cal = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    // Days calculation
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Sunday is 1, Monday is 2 in Java Calendar. We map Monday to index 0, Sunday to 6
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

    val totalCells = if (isWeekOnly) 7 else 35.coerceAtLeast(((firstDayOfWeek + maxDays + 6) / 7) * 7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val rows = totalCells / 7
        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val dayNum = cellIndex - firstDayOfWeek + 1

                    if (dayNum in 1..maxDays) {
                        val dayKey = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNum)
                        val isSelected = dayKey == selectedDateKey
                        val isToday = dayKey == todayDateKey
                        val notesCount = notesByDate[dayKey]?.size ?: 0

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> colors.accent
                                        isToday -> colors.cardBackground
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday && !isSelected) 1.dp else 0.dp,
                                    color = if (isToday && !isSelected) colors.accent.copy(alpha = 0.6f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectDate(dayKey) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$dayNum",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.background else colors.textPrimary,
                                        fontSize = 13.sp
                                    )
                                )
                                if (notesCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(
                                                if (isSelected) colors.background else colors.accent,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty padding cell
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}
