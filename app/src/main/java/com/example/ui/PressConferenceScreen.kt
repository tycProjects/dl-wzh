package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Club
import com.example.model.PressConferenceSession
import com.example.model.PressOption
import com.example.model.PressTone
import com.example.ui.components.ClubBadge
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurfEmerald

@Composable
fun PressConferenceScreen(
    pressSession: PressConferenceSession,
    userClub: Club,
    onSelectOption: (optionId: String) -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    BackHandler {
        if (pressSession.isComplete) {
            onComplete()
        } else {
            onSkip()
        }
    }

    val currentMorale = userClub.startingXiPlayers().map { it.morale }.average().toInt()
    val currentRep = userClub.reputation
    val totalQuestions = pressSession.questions.size.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Media Room Top Header
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "POST-MATCH MEDIA ROOM",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                        )
                    }

                    if (!pressSession.isComplete) {
                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.testTag("skip_press_button"),
                        ) {
                            Text(
                                text = "Walk Past Mics",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ClubBadge(
                        club = userClub,
                        size = 44.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = BroadcastAmber.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = pressSession.contextBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pressSession.matchHeadline,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = pressSession.scoreSummary,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }

                // Live Morale & Reputation Status Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ImpactMetricChip(
                        label = "Squad Morale",
                        currentValue = currentMorale,
                        pendingDelta = pressSession.totalMoraleDelta,
                        accent = TurfEmerald,
                        modifier = Modifier.weight(1f),
                    )
                    ImpactMetricChip(
                        label = "Team Reputation",
                        currentValue = currentRep,
                        pendingDelta = pressSession.totalReputationDelta,
                        accent = ElectricCyan,
                        modifier = Modifier.weight(1f),
                    )
                }

                val progress = if (pressSession.isComplete) 1f else {
                    (pressSession.currentQuestionIndex.toFloat() / totalQuestions.toFloat()).coerceIn(0f, 1f)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = ElectricCyan,
                    trackColor = SlateCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }
        }

        // Body: Active Question OR Summary
        AnimatedContent(
            targetState = pressSession.isComplete,
            label = "press_stage",
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 640.dp),
        ) { complete ->
            if (!complete) {
                val qIdx = pressSession.currentQuestionIndex.coerceIn(0, pressSession.questions.lastIndex)
                val question = pressSession.questions[qIdx]

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Newspaper,
                                            contentDescription = null,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${question.journalistName} • ${question.outletName}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ElectricCyan,
                                        )
                                    }
                                    Text(
                                        text = "Q${qIdx + 1}/$totalQuestions",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                }

                                Text(
                                    text = "\"${question.question}\"",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Choose Your Response",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary,
                        )
                    }

                    items(question.options, key = { it.id }) { option ->
                        PressResponseCard(
                            option = option,
                            onClick = { onSelectOption(option.id) },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.5.dp, TurfEmerald.copy(alpha = 0.65f)),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = TurfEmerald,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "BACK PAGE HEADLINE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TurfEmerald,
                                        )
                                        Text(
                                            text = pressSession.lastHeadlineSnippet ?: pressSession.matchHeadline,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    SummaryDeltaCard(
                                        title = "Squad Morale",
                                        delta = pressSession.totalMoraleDelta,
                                        newValue = (currentMorale + pressSession.totalMoraleDelta).coerceIn(35, 100),
                                        modifier = Modifier.weight(1f),
                                    )
                                    SummaryDeltaCard(
                                        title = "Team Reputation",
                                        delta = pressSession.totalReputationDelta,
                                        newValue = (currentRep + pressSession.totalReputationDelta).coerceIn(10, 99),
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                Button(
                                    onClick = onComplete,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TurfEmerald,
                                        contentColor = PitchBlack,
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("complete_press_button"),
                                ) {
                                    Text(
                                        text = "Return to Club Hub",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
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
private fun PressResponseCard(
    option: PressOption,
    onClick: () -> Unit,
) {
    val toneColor = when (option.tone) {
        PressTone.CONFIDENT -> TurfEmerald
        PressTone.CALM -> ElectricCyan
        PressTone.FIERY -> CrimsonRed
        PressTone.PROTECTIVE -> BroadcastAmber
        PressTone.HUMBLE -> Color(0xFFA78BFA)
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SlateElevated),
        border = BorderStroke(1.dp, HairlineBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("press_option_${option.id}"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = toneColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = option.tone.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = toneColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeltaTag(label = "MOR", delta = option.moraleDelta)
                    DeltaTag(label = "REP", delta = option.reputationDelta)
                }
            }

            Text(
                text = option.quote,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun DeltaTag(label: String, delta: Int) {
    val color = when {
        delta > 0 -> TurfEmerald
        delta < 0 -> CrimsonRed
        else -> TextSecondary
    }
    val prefix = if (delta > 0) "+" else ""
    Text(
        text = "$label $prefix$delta",
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

@Composable
private fun ImpactMetricChip(
    label: String,
    currentValue: Int,
    pendingDelta: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SlateCard,
        border = BorderStroke(1.dp, HairlineBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currentValue",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
                if (pendingDelta != 0) {
                    val deltaColor = if (pendingDelta > 0) TurfEmerald else CrimsonRed
                    val sign = if (pendingDelta > 0) "+" else ""
                    Text(
                        text = " ($sign$pendingDelta)",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = deltaColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryDeltaCard(
    title: String,
    delta: Int,
    newValue: Int,
    modifier: Modifier = Modifier,
) {
    val color = when {
        delta > 0 -> TurfEmerald
        delta < 0 -> CrimsonRed
        else -> TextSecondary
    }
    val sign = if (delta > 0) "+" else ""

    Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, HairlineBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (delta >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$sign$delta  →  $newValue",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                )
            }
        }
    }
}
