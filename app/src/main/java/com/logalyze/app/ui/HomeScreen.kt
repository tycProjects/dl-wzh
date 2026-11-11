package com.logalyze.app.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logalyze.app.R
import com.logalyze.app.ui.theme.ErrorRed
import com.logalyze.app.ui.theme.Success
import java.util.Locale

data class LogFile(val uri: Uri, val name: String, val size: Long)

private const val MAX_SIZE = 5L * 1024 * 1024
private val ALLOWED_EXT = setOf("log", "txt")

fun queryFile(context: Context, uri: Uri): LogFile? {
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val si = c.getColumnIndex(OpenableColumns.SIZE)
            val name = if (ni >= 0 && !c.isNull(ni)) c.getString(ni) else "log"
            val size = if (si >= 0 && !c.isNull(si)) c.getLong(si) else -1L
            return LogFile(uri, name, size)
        }
    }
    return null
}

private fun formatSize(b: Long, unknown: String): String = when {
    b < 0 -> unknown
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", b / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", b / (1024.0 * 1024.0))
}

@Composable
fun HomeScreen(onOpenHistory: () -> Unit, analyzing: Boolean, onAnalyze: (LogFile) -> Unit) {
    val context = LocalContext.current
    var file by remember { mutableStateOf<LogFile?>(null) }
    var error by remember { mutableStateOf<Int?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val picked = queryFile(context, uri)
            val ext = picked?.name?.substringAfterLast('.', "")?.lowercase().orEmpty()
            when {
                picked == null -> { file = null; error = R.string.err_read }
                ext !in ALLOWED_EXT -> { file = null; error = R.string.err_ext }
                picked.size == 0L -> { file = null; error = R.string.err_empty }
                picked.size > MAX_SIZE -> { file = null; error = R.string.err_large }
                else -> { file = picked; error = null }
            }
        }
    }

    val line = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    val unknownSize = stringResource(R.string.unknown_size)
    val launchers = listOf("Zalith", "Pojav", "Prism", stringResource(R.string.launcher_more))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Wordmark(26)

        Stagger(0) {
            Card(colors = cardColors, border = line, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.home_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        stringResource(R.string.home_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Stagger(1) {
            Button(
                onClick = { picker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.upload_log), fontWeight = FontWeight.SemiBold)
            }
        }

        val f = file
        if (f != null) {
            Card(colors = cardColors, border = BorderStroke(1.dp, Success), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                f.name,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                formatSize(f.size, unknownSize),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { file = null }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_file))
                        }
                    }
                    Button(
                        onClick = { onAnalyze(f) },
                        enabled = !analyzing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(if (analyzing) R.string.analyzing else R.string.analyze_log))
                    }
                }
            }
        }

        val e = error
        if (e != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(e), color = ErrorRed, fontSize = 13.sp)
            }
        }

        Stagger(2) {
            OutlinedButton(
                onClick = onOpenHistory,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = line
            ) { Text(stringResource(R.string.view_history)) }
        }

        Stagger(3) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.supported_launchers),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    launchers.forEach { name ->
                        Card(
                            colors = cardColors,
                            border = line,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                name,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
