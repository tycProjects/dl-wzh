package com.logalyze.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logalyze.app.LocalSettings
import com.logalyze.app.R
import com.logalyze.app.ui.theme.Accent
import com.logalyze.app.ui.theme.ThemeMode
import com.logalyze.app.ui.theme.resolveDark

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.background)
        }
    }
}

@Composable
private fun field(): androidx.compose.material3.TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary
)

@Composable
fun SettingsScreen() {
    val s = LocalSettings.current
    val line = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    val onBg = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val dark = resolveDark(s.themeMode)
    val languages = listOf("en" to "English", "ar" to "العربية")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(stringResource(R.string.settings_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = onBg)

        // ---- Language ----
        Stagger(0) {
            Card(colors = cardColors, border = line, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)) {
                        Text(stringResource(R.string.language), fontWeight = FontWeight.Bold, color = primary)
                        Text(stringResource(R.string.language_hint), fontSize = 12.sp, color = muted)
                    }
                    languages.forEachIndexed { index, (code, name) ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { if (code != s.language) s.updateLanguage(code) }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(name, color = onBg, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (code == s.language) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = primary)
                            }
                        }
                    }
                }
            }
        }

        // ---- Appearance ----
        Stagger(1) {
            Card(colors = cardColors, border = line, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.theme_title), fontWeight = FontWeight.Bold, color = primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(stringResource(R.string.theme_system), s.themeMode == ThemeMode.System) {
                            s.updateThemeMode(ThemeMode.System)
                        }
                        Pill(stringResource(R.string.theme_dark), s.themeMode == ThemeMode.Dark) {
                            s.updateThemeMode(ThemeMode.Dark)
                        }
                        Pill(stringResource(R.string.theme_light), s.themeMode == ThemeMode.Light) {
                            s.updateThemeMode(ThemeMode.Light)
                        }
                    }
                    Text(stringResource(R.string.accent_title), fontSize = 13.sp, color = muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Accent.entries.forEach { a ->
                            Swatch(if (dark) a.dark else a.light, s.accent == a) { s.updateAccent(a) }
                        }
                    }
                }
            }
        }

        // ---- About ----
        Stagger(3) {
            Card(colors = cardColors, border = line, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.about), fontWeight = FontWeight.Bold, color = primary)
                    Text(stringResource(R.string.about_text), color = onBg)
                    Text(stringResource(R.string.version_label), fontSize = 12.sp, color = muted)
                }
            }
        }
    }
}
