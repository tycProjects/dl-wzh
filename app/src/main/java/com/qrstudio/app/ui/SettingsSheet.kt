package com.qrstudio.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qrstudio.app.data.ScanHistoryRepository
import com.qrstudio.app.data.SettingsRepository
import com.qrstudio.app.ui.components.ChipsRow
import com.qrstudio.app.ui.components.MutedLabel
import com.qrstudio.app.ui.theme.Danger
import com.qrstudio.app.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun SettingsSheet(
    settings: SettingsRepository,
    history: ScanHistoryRepository,
    onDone: (String) -> Unit,
) {
    var size by remember { mutableStateOf(settings.defaultSize) }
    var ec by remember { mutableStateOf(settings.errorCorrection) }
    var sound by remember { mutableStateOf(settings.sound) }
    var vibrate by remember { mutableStateOf(settings.vibrate) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("الإعدادات", style = MaterialTheme.typography.titleLarge)

        MutedLabel("دقة التصدير الافتراضية", modifier = Modifier.padding(top = 0.dp))
        ChipsRow(
            items = listOf("300" to "300px", "600" to "600px", "1200" to "1200px", "2000" to "2000px"),
            selectedKey = size.toString(),
        ) { key ->
            size = key.toInt()
            settings.defaultSize = size
        }

        MutedLabel("مستوى تصحيح الخطأ الافتراضي")
        ChipsRow(
            items = listOf("L" to "L · 7%", "M" to "M · 15%", "Q" to "Q · 25%", "H" to "H · 30%"),
            selectedKey = ec,
        ) { key ->
            ec = key
            settings.errorCorrection = ec
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("صوت عند المسح الناجح", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = sound, onCheckedChange = { sound = it; settings.sound = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("اهتزاز عند المسح الناجح", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = vibrate, onCheckedChange = { vibrate = it; settings.vibrate = it })
        }

        Button(
            onClick = {
                scope.launch {
                    history.clear()
                    settings.reset()
                    onDone("تم مسح جميع البيانات والإعدادات")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Danger),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("مسح جميع البيانات (السجل + الإعدادات)") }

        Text(
            "تُحفظ التفضيلات محلياً على جهازك. التطبيق يعمل دون إنترنت بالكامل — الإنترنت مطلوب فقط عند جلب شعار موقع.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
    }
}
