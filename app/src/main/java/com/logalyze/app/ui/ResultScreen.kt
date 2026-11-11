package com.logalyze.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logalyze.app.R
import com.logalyze.app.analysis.LogAnalyzer
import com.logalyze.app.ui.theme.ErrorRed
import com.logalyze.app.ui.theme.Success

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun infoLabel(key: String): String = stringResource(
    when (key) {
        "device" -> R.string.info_device
        "android" -> R.string.info_android
        "cpu" -> R.string.info_cpu
        "arch" -> R.string.info_arch
        "ram" -> R.string.info_ram
        "java" -> R.string.info_java
        "renderer" -> R.string.info_renderer
        else -> R.string.info_gpu
    }
)

@Composable
fun ResultScreen(
    result: AnalysisResult,
    fileName: String?,
    resolved: Boolean,
    onResolvedChange: (Boolean) -> Unit,
    onAskAi: () -> Unit,
    onBack: () -> Unit
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val onBg = MaterialTheme.colorScheme.onBackground
    val noError = LogAnalyzer.isNoError(result.errorType)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.result_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onBg)
        }

        Stagger(0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val tagColor = if (noError) Success else ErrorRed
                    Text(
                        stringResource(if (noError) R.string.tag_none else R.string.tag_crash),
                        color = tagColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .border(1.dp, tagColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Text(result.errorType, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = onBg)
                    Text(result.summary, color = muted)
                    if (fileName != null) {
                        Text(stringResource(R.string.log_label, fileName), fontSize = 12.sp, color = muted)
                    }
                    result.launcher?.let {
                        Text(stringResource(R.string.launcher_label, it), fontSize = 12.sp, color = muted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.confidence), color = muted, modifier = Modifier.weight(1f))
                        Text(
                            "${result.confidence}%",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { result.confidence / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        if (result.deviceInfo.isNotEmpty()) {
            Stagger(1) {
                SectionCard(stringResource(R.string.device_info)) {
                    result.deviceInfo.forEach { (key, value) ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                infoLabel(key),
                                color = muted,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(0.38f)
                            )
                            Text(value, color = onBg, fontSize = 13.sp, modifier = Modifier.weight(0.62f))
                        }
                    }
                }
            }
        }

        Stagger(2) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionCard(stringResource(R.string.root_cause)) {
                    Text(result.rootCause, color = onBg)
                }
                if (result.mods.isNotEmpty()) {
                    SectionCard(stringResource(R.string.mods_title)) {
                        result.mods.forEach { mod ->
                            Text("• $mod", color = onBg, fontWeight = FontWeight.SemiBold)
                        }
                        Text(stringResource(R.string.mods_hint), fontSize = 12.sp, color = muted)
                    }
                }
            }
        }

        if (result.evidence.isNotEmpty()) {
            Stagger(3) {
                SectionCard(stringResource(R.string.evidence)) {
                    result.evidence.forEach { line ->
                        Text(
                            line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = onBg,
                            textAlign = TextAlign.Left,
                            style = TextStyle(textDirection = TextDirection.Ltr),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(10.dp)
                        )
                    }
                }
            }
        }

        Stagger(4) {
            SectionCard(stringResource(R.string.fixes)) {
                result.fixes.forEach { fix ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(fix, color = onBg)
                    }
                }
            }
        }

        if (!noError) {
            Stagger(5) {
                SectionCard(stringResource(R.string.feedback_question)) {
                    if (resolved) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.feedback_resolved),
                                color = onBg,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onResolvedChange(false) }) {
                                Text(stringResource(R.string.feedback_undo))
                            }
                        }
                    } else {
                        Button(
                            onClick = { onResolvedChange(true) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(stringResource(R.string.feedback_yes)) }
                    }
                }
            }
        }

        if (!noError) {
            Stagger(6) {
                OutlinedButton(
                    onClick = onAskAi,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) { Text(stringResource(R.string.ask_ai)) }
            }
        }

        Stagger(7) {
            Text(
                stringResource(R.string.kb_note),
                fontSize = 11.sp,
                color = muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }
    }
}
