package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppSettings
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings

@Composable
fun SettingsDialog(
    isOpen: Boolean,
    settings: AppSettings,
    totalNotesCount: Int,
    totalFoldersCount: Int,
    onUpdateSetting: (key: String, value: String) -> Unit,
    onExportBackupJson: () -> String,
    onImportBackupJson: (jsonString: String, mode: String) -> Result<String>,
    onExportSaf: () -> Unit = {},
    onImportSaf: (mode: String) -> Unit = {},
    onClearAllData: () -> Unit,
    onClose: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    var selectedTab by remember { mutableStateOf("language") } // language, appearance, editor, backup, storage

    var importJsonText by remember { mutableStateOf("") }
    var importMode by remember { mutableStateOf("merge") } // merge, replace
    var showReplaceWarning by remember { mutableStateOf(false) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var importIsError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, colors.borderColor, RoundedCornerShape(14.dp)),
            color = colors.bgCard
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.settingsAndData,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "TUMBA NOTES v1.0.0 — Offline Personal Notes",
                            fontSize = 10.5.sp,
                            color = colors.textSecondary
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

                // Tab selector bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgSidebar)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsTabItem(strings.language, selectedTab == "language") { selectedTab = "language" }
                    SettingsTabItem(strings.appearance, selectedTab == "appearance") { selectedTab = "appearance" }
                    SettingsTabItem(strings.editor, selectedTab == "editor") { selectedTab = "editor" }
                    SettingsTabItem(strings.backup, selectedTab == "backup") { selectedTab = "backup" }
                    SettingsTabItem(strings.storage, selectedTab == "storage") { selectedTab = "storage" }
                }

                HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

                // Tab Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        "language" -> {
                            Text(
                                text = strings.interfaceLanguage,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textMuted
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LanguageOptionCard(
                                    code = "uk",
                                    nativeName = strings.ukrainian,
                                    subtitle = "Українська мова",
                                    flagEmoji = "🇺🇦",
                                    isSelected = settings.language == "uk",
                                    onClick = { onUpdateSetting("language", "uk") }
                                )
                                LanguageOptionCard(
                                    code = "ru",
                                    nativeName = strings.russian,
                                    subtitle = "Русский язык",
                                    flagEmoji = "🇷🇺",
                                    isSelected = settings.language == "ru",
                                    onClick = { onUpdateSetting("language", "ru") }
                                )
                                LanguageOptionCard(
                                    code = "en",
                                    nativeName = strings.english,
                                    subtitle = "English language",
                                    flagEmoji = "🇬🇧",
                                    isSelected = settings.language == "en",
                                    onClick = { onUpdateSetting("language", "en") }
                                )
                            }
                        }

                        "appearance" -> {
                            // Theme mode
                            Text("GRAPHITE THEME", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeCard("dark", strings.themeObsidian, settings.theme == "dark", modifier = Modifier.weight(1f)) {
                                    onUpdateSetting("theme", "dark")
                                }
                                ThemeCard("dim", strings.themeSlate, settings.theme == "dim", modifier = Modifier.weight(1f)) {
                                    onUpdateSetting("theme", "dim")
                                }
                                ThemeCard("light", strings.themePaper, settings.theme == "light", modifier = Modifier.weight(1f)) {
                                    onUpdateSetting("theme", "light")
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Accent Nuance
                            Text(strings.accentNuance, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "silver" to "Silver",
                                    "titanium" to "Titanium",
                                    "platinum" to "Platinum",
                                    "gold" to "Gold",
                                    "ice" to "Ice"
                                ).forEach { (accKey, accLabel) ->
                                    AccentChip(
                                        name = accLabel,
                                        key = accKey,
                                        isSelected = settings.accent.equals(accKey, ignoreCase = true),
                                        onClick = { onUpdateSetting("accent", accKey) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Compact card mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.bgElevated)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(strings.compactModeTitle, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    Text(strings.compactModeDesc, fontSize = 11.sp, color = colors.textSecondary)
                                }
                                Switch(
                                    checked = settings.compactMode,
                                    onCheckedChange = { onUpdateSetting("compactMode", it.toString()) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.textPrimary,
                                        checkedTrackColor = colors.accent,
                                        uncheckedThumbColor = colors.textSecondary,
                                        uncheckedTrackColor = colors.bgCard
                                    )
                                )
                            }
                        }

                        "editor" -> {
                            // Editor Typography style
                            Text(strings.editorTypography, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FontCard("sans", "System Sans", "Modern", settings.editorFontFamily == "sans", Modifier.weight(1f)) {
                                    onUpdateSetting("editorFontFamily", "sans")
                                }
                                FontCard("serif", "Editorial Serif", "Classic", settings.editorFontFamily == "serif", Modifier.weight(1f)) {
                                    onUpdateSetting("editorFontFamily", "serif")
                                }
                                FontCard("mono", "Developer Mono", "Code", settings.editorFontFamily == "mono", Modifier.weight(1f)) {
                                    onUpdateSetting("editorFontFamily", "mono")
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Font size
                            Text(strings.fontSize, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OptionCard("small", "Small (14sp)", settings.fontSize == "small", Modifier.weight(1f)) {
                                    onUpdateSetting("fontSize", "small")
                                }
                                OptionCard("medium", "Medium (16sp)", settings.fontSize == "medium", Modifier.weight(1f)) {
                                    onUpdateSetting("fontSize", "medium")
                                }
                                OptionCard("large", "Large (18sp)", settings.fontSize == "large", Modifier.weight(1f)) {
                                    onUpdateSetting("fontSize", "large")
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Autosave interval
                            Text(strings.autosaveInterval, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OptionCard("400", "400ms", settings.autosaveIntervalMs == 400L, Modifier.weight(1f)) {
                                    onUpdateSetting("autosaveIntervalMs", "400")
                                }
                                OptionCard("800", "800ms (Std)", settings.autosaveIntervalMs == 800L, Modifier.weight(1f)) {
                                    onUpdateSetting("autosaveIntervalMs", "800")
                                }
                                OptionCard("1500", "1.5 sec", settings.autosaveIntervalMs == 1500L, Modifier.weight(1f)) {
                                    onUpdateSetting("autosaveIntervalMs", "1500")
                                }
                            }
                        }

                        "backup" -> {
                            Text(strings.exportJsonBackup, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                            Text(
                                strings.exportDesc,
                                fontSize = 11.5.sp,
                                color = colors.textSecondary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        val json = onExportBackupJson()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("TumbaNotes_Backup", json))
                                        Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.bgElevated, contentColor = colors.textPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("copy_backup_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.copyJson, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val json = onExportBackupJson()
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_SUBJECT, "tumba-notes-backup.json")
                                            putExtra(Intent.EXTRA_TEXT, json)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Backup"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.bgMain),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("share_backup_button")
                                ) {
                                    Icon(Icons.Default.Share, null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.shareFile, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = onExportSaf,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.bgMain),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Download, null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.exportSafButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = colors.borderColor, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(strings.importRestoreBackup, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                            Text(
                                strings.importDesc,
                                fontSize = 11.5.sp,
                                color = colors.textSecondary
                            )

                            // SAF Import Button
                            Button(
                                onClick = {
                                    if (importMode == "replace") {
                                        showReplaceWarning = true
                                    } else {
                                        onImportSaf("merge")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent.copy(alpha = 0.2f), contentColor = colors.accent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Upload, null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.importSafButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (showReplaceWarning) {
                                AlertDialog(
                                    onDismissRequest = { showReplaceWarning = false },
                                    title = { Text("Replace Entire Database?", color = colors.textPrimary) },
                                    text = {
                                        Text(
                                            "Warning: Replace mode will permanently wipe all current notes, folders, tags, attachments, and settings before restoring the backup. Are you sure you want to proceed?",
                                            color = colors.textSecondary
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showReplaceWarning = false
                                                onImportSaf("replace")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = Color.White)
                                        ) {
                                            Text("Replace Everything")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showReplaceWarning = false }) {
                                            Text(strings.cancel, color = colors.textSecondary)
                                        }
                                    },
                                    containerColor = colors.bgElevated
                                )
                            }

                            OutlinedTextField(
                                value = importJsonText,
                                onValueChange = { importJsonText = it },
                                placeholder = { Text(strings.pasteJsonPlaceholder, fontSize = 12.sp, color = colors.textMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .testTag("import_json_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.bgElevated,
                                    unfocusedContainerColor = colors.bgElevated,
                                    focusedBorderColor = colors.accent,
                                    unfocusedBorderColor = colors.borderColor,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Conflict resolution
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { importMode = "merge" }
                                ) {
                                    RadioButton(
                                        selected = importMode == "merge",
                                        onClick = { importMode = "merge" },
                                        colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                                    )
                                    Text(strings.mergeWithExisting, fontSize = 11.5.sp, color = colors.textPrimary)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { importMode = "replace" }
                                ) {
                                    RadioButton(
                                        selected = importMode == "replace",
                                        onClick = { importMode = "replace" },
                                        colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                                    )
                                    Text(strings.replaceDatabase, fontSize = 11.5.sp, color = colors.textPrimary)
                                }
                            }

                            Button(
                                onClick = {
                                    if (importJsonText.isBlank()) {
                                        importMessage = "Please paste a JSON backup first."
                                        importIsError = true
                                        return@Button
                                    }
                                    val res = onImportBackupJson(importJsonText, importMode)
                                    res.fold(
                                        onSuccess = { msg ->
                                            importMessage = msg
                                            importIsError = false
                                            importJsonText = ""
                                        },
                                        onFailure = { err ->
                                            importMessage = "Import failed: ${err.message}"
                                            importIsError = true
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.bgElevated, contentColor = colors.textPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("execute_import_button")
                            ) {
                                Icon(Icons.Default.Upload, null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.restoreDatabase, fontSize = 12.sp)
                            }

                            if (importMessage != null) {
                                Text(
                                    text = importMessage!!,
                                    fontSize = 11.sp,
                                    color = if (importIsError) colors.danger else colors.accent
                                )
                            }
                        }

                        "storage" -> {
                            Text(strings.storageFootprint, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.bgElevated)
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(strings.totalNotesUpper, fontSize = 10.sp, color = colors.textMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("$totalNotesCount", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.bgElevated)
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(strings.totalFoldersUpper, fontSize = 10.sp, color = colors.textMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("$totalFoldersCount", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = colors.borderColor, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(strings.dangerZone, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.danger)
                            Text(
                                strings.clearAllDataDesc,
                                fontSize = 11.5.sp,
                                color = colors.textSecondary
                            )

                            Button(
                                onClick = onClearAllData,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.dangerBg, contentColor = colors.danger),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("wipe_all_data_button")
                            ) {
                                Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.clearAllData, fontSize = 12.sp)
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgSidebar)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "100% Offline SQLite Storage",
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.bgElevated, contentColor = colors.textPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(strings.done, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(
    code: String,
    nativeName: String,
    subtitle: String,
    flagEmoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalTumbaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.bgElevated else colors.bgCard)
            .border(if (isSelected) 1.dp else 0.8.dp, if (isSelected) colors.accent else colors.borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = flagEmoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = nativeName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = colors.bgMain, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun SettingsTabItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalTumbaColors.current
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) colors.textPrimary else colors.textMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .width(36.dp)
                    .background(if (isSelected) colors.accent else Color.Transparent)
            )
        }
    }
}

@Composable
private fun ThemeCard(key: String, title: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalTumbaColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.bgElevated else colors.bgCard)
            .border(if (isSelected) 1.dp else 0.8.dp, if (isSelected) colors.accent else colors.borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(Icons.Default.Check, null, tint = colors.accent, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun AccentChip(name: String, key: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalTumbaColors.current
    val accentColor = remember(key) {
        when (key.lowercase()) {
            "titanium" -> Color(0xFF9AA0A6)
            "platinum" -> Color(0xFFE5E5E7)
            "gold" -> Color(0xFFD4AF37)
            "ice" -> Color(0xFF93C5FD)
            else -> Color(0xFFB7B9BE)
        }
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.bgElevated else colors.bgCard)
            .border(if (isSelected) 1.dp else 0.8.dp, if (isSelected) colors.accent else colors.borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(name, fontSize = 11.5.sp, color = colors.textPrimary)
    }
}

@Composable
private fun FontCard(key: String, label: String, sample: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalTumbaColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.bgElevated else colors.bgCard)
            .border(if (isSelected) 1.dp else 0.8.dp, if (isSelected) colors.accent else colors.borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text(sample, fontSize = 9.sp, color = colors.textMuted)
        }
    }
}

@Composable
private fun OptionCard(key: String, label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalTumbaColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.bgElevated else colors.bgCard)
            .border(if (isSelected) 1.dp else 0.8.dp, if (isSelected) colors.accent else colors.borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = colors.textPrimary)
    }
}
