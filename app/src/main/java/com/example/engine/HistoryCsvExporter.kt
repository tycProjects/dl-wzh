package com.example.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.CalculationItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HistoryCsvExporter {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun generateCsv(history: List<CalculationItem>): String {
        val sb = StringBuilder()
        // Header
        sb.append("Timestamp,Date,Time,Category,Expression,Result\n")

        for (item in history) {
            val date = Date(item.timestamp)
            val dStr = dateFormat.format(date)
            val tStr = timeFormat.format(date)

            sb.append(item.timestamp).append(",")
            sb.append(escapeCsv(dStr)).append(",")
            sb.append(escapeCsv(tStr)).append(",")
            sb.append(escapeCsv(item.category)).append(",")
            sb.append(escapeCsv(item.expression)).append(",")
            sb.append(escapeCsv(item.result))
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(text: String): String {
        val needsQuotes = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")
        val escaped = text.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    fun shareCsv(context: Context, history: List<CalculationItem>) {
        if (history.isEmpty()) return

        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val fileName = "calculator_history_${System.currentTimeMillis()}.csv"
        val csvFile = File(exportDir, fileName)

        FileOutputStream(csvFile).use { fos ->
            fos.write(generateCsv(history).toByteArray(Charsets.UTF_8))
        }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Sharp Calculator History Export")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, "Here is the exported calculation history tape from Sharp Calculator (${history.size} entries).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Calculation History CSV")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun saveToUri(context: Context, uri: Uri, history: List<CalculationItem>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(generateCsv(history).toByteArray(Charsets.UTF_8))
                os.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
