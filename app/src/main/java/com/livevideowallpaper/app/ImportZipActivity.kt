package com.livevideowallpaper.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import java.io.IOException

class ImportZipActivity : BaseActivity() {

    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var result: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnGallery: Button

    private val pickZip = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        btnSelect.isEnabled = false
        btnGallery.visibility = View.GONE
        result.text = ""
        progress.visibility = View.VISIBLE
        progress.isIndeterminate = true
        status.text = "Validating ZIP…"
        Bg.io.execute {
            try {
                val total = ZipImporter.scan(this, uri)
                if (total == 0) {
                    runOnUiThread { finishUi("No supported videos were found in this ZIP.", null) }
                    return@execute
                }
                runOnUiThread {
                    progress.isIndeterminate = false
                    progress.max = total
                    progress.progress = 0
                    status.text = "Found $total video(s). Extracting…"
                }
                val s = ZipImporter.extract(this, uri, total) { done, t, name ->
                    runOnUiThread { progress.progress = done; status.text = "Extracting ${done + 1} of $t: $name" }
                }
                val text = buildString {
                    append("Imported ${s.imported} video(s)")
                    if (s.skipped > 0) append(", skipped ${s.skipped}")
                    append('.')
                    s.messages.forEach { append("\n• ").append(it) }
                }
                runOnUiThread { finishUi(text, s) }
            } catch (e: IOException) {
                runOnUiThread { finishUi("This ZIP is damaged or unreadable (${e.message}).", null) }
            } catch (e: Exception) {
                runOnUiThread { finishUi("Import failed: ${e.message}", null) }
            }
        }
    }

    private fun finishUi(text: String, s: ZipImporter.Summary?) {
        if (isFinishing || isDestroyed) return
        progress.visibility = View.GONE
        status.text = ""
        result.text = text
        btnSelect.isEnabled = true
        if (s != null && s.imported > 0) btnGallery.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_zip)
        setupToolbar(R.string.import_zip_title)
        progress = findViewById(R.id.progress)
        status = findViewById(R.id.txtStatus)
        result = findViewById(R.id.txtResult)
        btnSelect = findViewById(R.id.btnSelectZip)
        btnGallery = findViewById(R.id.btnOpenGallery)
        btnSelect.setOnClickListener {
            pickZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
        }
        btnGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java)); finish()
        }
    }
}
