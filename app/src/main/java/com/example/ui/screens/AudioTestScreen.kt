package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.model.AudioTestStatus
import com.example.model.AudioTestType
import com.example.ui.theme.JnmfAlertRed
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfDarkBg
import com.example.ui.theme.JnmfSurface
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfTextPrimary
import com.example.ui.theme.JnmfTextSecondary
import com.example.ui.theme.JnmfTheme
import com.example.ui.theme.JnmfWarningYellow

@Composable
fun AudioTestScreen(
    testResults: Map<AudioTestType, AudioTestStatus>,
    onRunTest: (AudioTestType) -> Unit,
    onRunAllTests: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JnmfTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JnmfDarkBg)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("audio_test_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.bright
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "AUDIO TEST",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.bright
                )
                Text(
                    text = "Diagnostic verification of DSP & effect pipelines",
                    fontSize = 11.sp,
                    color = JnmfTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Run All Tests Action
        Button(
            onClick = onRunAllTests,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("run_all_audio_tests_button"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = JnmfDarkBg
            )
        ) {
            Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RUN FULL AUDIO TEST SUITE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "//TEST_RESULTS_PIPELINE",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Tests List
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AudioTestType.entries.forEach { testType ->
                val result = testResults[testType]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_item_${testType.name.lowercase()}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = JnmfSurface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (result) {
                            AudioTestStatus.PASS -> colors.borderGlow
                            AudioTestStatus.FALLBACK_ACTIVE -> JnmfWarningYellow.copy(alpha = 0.5f)
                            AudioTestStatus.UNSUPPORTED -> JnmfAlertRed.copy(alpha = 0.5f)
                            null -> JnmfBorder
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = testType.title,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = JnmfTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = testType.description,
                                fontSize = 11.sp,
                                color = JnmfTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (result != null) {
                                TestResultBadge(result, colors.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            OutlinedButton(
                                onClick = { onRunTest(testType) },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("btn_run_${testType.name.lowercase()}"),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = colors.bright
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary)
                            ) {
                                Text(
                                    text = if (result == null) "TEST" else "RE-TEST",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Diagnostic Legend
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "STATUS CRITERIA:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.bright
                )
                Spacer(modifier = Modifier.height(6.dp))
                LegendRow(label = "PASS", desc = "Native hardware processing actively rendering without errors.", color = colors.primary)
                Spacer(modifier = Modifier.height(4.dp))
                LegendRow(label = "FALLBACK ACTIVE", desc = "JNMF Internal DSP processing substituted cleanly to prevent audio dropout.", color = JnmfWarningYellow)
                Spacer(modifier = Modifier.height(4.dp))
                LegendRow(label = "UNSUPPORTED", desc = "Device OS/Hardware lacks audio feature support.", color = JnmfAlertRed)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TestResultBadge(status: AudioTestStatus, passColor: Color) {
    val (bgColor, textColor) = when (status) {
        AudioTestStatus.PASS -> Pair(passColor.copy(alpha = 0.15f), passColor)
        AudioTestStatus.FALLBACK_ACTIVE -> Pair(JnmfWarningYellow.copy(alpha = 0.15f), JnmfWarningYellow)
        AudioTestStatus.UNSUPPORTED -> Pair(JnmfAlertRed.copy(alpha = 0.15f), JnmfAlertRed)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, textColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun LegendRow(label: String, desc: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: ",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = desc,
            fontSize = 10.sp,
            color = JnmfTextSecondary
        )
    }
}
