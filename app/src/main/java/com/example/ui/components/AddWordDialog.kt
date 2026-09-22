package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DictionaryEntry
import com.example.ui.theme.CyberDarkBorder
import com.example.ui.theme.CyberDarkElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.TextLightGray
import com.example.ui.theme.TextMutedGray
import com.example.ui.theme.TextWhite

@Composable
fun AddWordDialog(
    onDismiss: () -> Unit,
    onSave: (
        bisaya: String,
        subanen: String,
        bisayaMeaning: String,
        subanenMeaning: String,
        partOfSpeech: String,
        example: String,
        notes: String,
        alt: String,
        verificationStatus: String
    ) -> Unit
) {
    var bisayaWord by remember { mutableStateOf("") }
    var subanenWord by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var partOfSpeech by remember { mutableStateOf("Noun") }
    var exampleSentence by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var alternativeTranslations by remember { mutableStateOf("") }
    var verificationStatus by remember { mutableStateOf(DictionaryEntry.STATUS_NEEDS_VERIFICATION) }

    val isDark = MaterialTheme.colorScheme.background.value == 0xFF070B12UL
    val partsOfSpeechList = listOf("Noun", "Verb", "Adjective", "Adverb", "Greeting", "Phrase")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) CyberDarkElevated else MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Add Dictionary Word",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add a test or verified word to the local Room database:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) TextLightGray else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Bisaya Word
                OutlinedTextField(
                    value = bisayaWord,
                    onValueChange = { bisayaWord = it },
                    label = { Text("Bisaya Word *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bisaya_word"),
                    colors = customTextFieldColors(isDark)
                )

                // Subanen Word
                OutlinedTextField(
                    value = subanenWord,
                    onValueChange = { subanenWord = it },
                    label = { Text("Subanen Translation *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_subanen_word"),
                    colors = customTextFieldColors(isDark)
                )

                // Meaning
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text("Meaning / Definition") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors(isDark)
                )

                // Part of Speech Selection
                Text(
                    text = "Part of Speech:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    partsOfSpeechList.take(3).forEach { pos ->
                        FilterChip(
                            selected = partOfSpeech == pos,
                            onClick = { partOfSpeech = pos },
                            label = { Text(pos, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
                                selectedLabelColor = CyanNeon
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    partsOfSpeechList.drop(3).forEach { pos ->
                        FilterChip(
                            selected = partOfSpeech == pos,
                            onClick = { partOfSpeech = pos },
                            label = { Text(pos, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
                                selectedLabelColor = CyanNeon
                            )
                        )
                    }
                }

                // Example Sentence
                OutlinedTextField(
                    value = exampleSentence,
                    onValueChange = { exampleSentence = it },
                    label = { Text("Example Sentence") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors(isDark)
                )

                // Alternative Translations
                OutlinedTextField(
                    value = alternativeTranslations,
                    onValueChange = { alternativeTranslations = it },
                    label = { Text("Alternative Translations") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors(isDark)
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. dialect, origin)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors(isDark)
                )

                // Verification Status
                Text(
                    text = "Verification Status:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = verificationStatus == DictionaryEntry.STATUS_NEEDS_VERIFICATION,
                        onClick = { verificationStatus = DictionaryEntry.STATUS_NEEDS_VERIFICATION },
                        label = { Text("Needs verification", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
                            selectedLabelColor = CyanNeon
                        )
                    )
                    FilterChip(
                        selected = verificationStatus == DictionaryEntry.STATUS_VERIFIED,
                        onClick = { verificationStatus = DictionaryEntry.STATUS_VERIFIED },
                        label = { Text("Verified", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
                            selectedLabelColor = CyanNeon
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bisayaWord.isNotBlank() && subanenWord.isNotBlank()) {
                        onSave(
                            bisayaWord,
                            subanenWord,
                            meaning,
                            meaning,
                            partOfSpeech,
                            exampleSentence,
                            notes,
                            alternativeTranslations,
                            verificationStatus
                        )
                    }
                },
                enabled = bisayaWord.isNotBlank() && subanenWord.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanNeon,
                    contentColor = CyberDarkElevated
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_word_button")
            ) {
                Text("Save Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    "Cancel",
                    color = if (isDark) TextLightGray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun customTextFieldColors(isDark: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = if (isDark) CyberDarkSurface else MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = if (isDark) CyberDarkSurface else MaterialTheme.colorScheme.surface,
    focusedBorderColor = CyanNeon,
    unfocusedBorderColor = if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outline,
    focusedTextColor = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface,
    cursorColor = CyanNeon
)
