package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DictionaryEntry
import com.example.data.model.LanguageDirection
import com.example.ui.theme.CyberDarkBorder
import com.example.ui.theme.CyberDarkElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusNeedsVerificationAmber
import com.example.ui.theme.StatusVerifiedGreen
import com.example.ui.theme.TextLightGray
import com.example.ui.theme.TextMutedGray
import com.example.ui.theme.TextWhite

@Composable
fun WordResultCard(
    entry: DictionaryEntry,
    direction: LanguageDirection,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val originalWord = entry.getSourceWord(direction)
    val translationWord = entry.getTranslationWord(direction)
    val meaning = entry.getMeaning(direction)

    val isDark = MaterialTheme.colorScheme.background.value == 0xFF070B12UL

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("word_card_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) CyberDarkElevated else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) CyanNeon.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Word + Verification Badge + Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel(text = "WORD")
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = originalWord,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Verification Status Badge
                    VerificationBadge(isVerified = entry.isVerified, statusText = entry.verificationStatus)

                    // Favorite Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) CyberDarkSurface else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("favorite_button_${entry.id}")
                    ) {
                        Icon(
                            imageVector = if (entry.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (entry.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (entry.isFavorite) CyanNeon else (if (isDark) TextLightGray else MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                color = if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // TRANSLATION
            SectionLabel(text = "TRANSLATION (${direction.targetLanguage.uppercase()})")
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = translationWord.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanNeon
                )
            )

            // MEANING (if available)
            if (meaning.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionLabel(text = "MEANING")
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = meaning,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                )
            }

            // PART OF SPEECH (if available)
            if (entry.partOfSpeech.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionLabel(text = "PART OF SPEECH")
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) CyberDarkSurface else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = entry.partOfSpeech,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) TextLightGray else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // EXAMPLE SENTENCE (if available)
            if (entry.exampleSentence.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionLabel(text = "EXAMPLE")
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) CyberDarkSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "“${entry.exampleSentence}”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // ALTERNATIVE TRANSLATIONS (if available)
            if (entry.alternativeTranslations.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionLabel(text = "ALTERNATIVE TRANSLATIONS")
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.alternativeTranslations,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDark) TextLightGray else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // NOTES (if available)
            if (entry.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionLabel(text = "NOTES")
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) TextMutedGray else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            fontSize = 10.sp,
            color = CyanNeon
        ),
        modifier = modifier
    )
}

@Composable
fun VerificationBadge(isVerified: Boolean, statusText: String, modifier: Modifier = Modifier) {
    val bgColor = if (isVerified) StatusGreenBg else StatusAmberBg
    val textColor = if (isVerified) StatusVerifiedGreen else StatusNeedsVerificationAmber
    val icon = if (isVerified) Icons.Filled.CheckCircle else Icons.Filled.Info

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            )
        }
    }
}
