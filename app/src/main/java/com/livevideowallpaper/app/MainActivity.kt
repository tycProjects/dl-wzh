package com.livevideowallpaper.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : BaseActivity() {

    private lateinit var adapter: VideoAdapter
    private lateinit var imgCurrent: ImageView
    private lateinit var txtCurrent: TextView
    private lateinit var txtEmpty: TextView

    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        Bg.io.execute {
            val f = VideoRepository.importVideo(this, uri)
            runOnUiThread {
                say(if (f != null) R.string.import_video_ok else R.string.import_video_fail)
                refresh()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgCurrent = findViewById(R.id.imgCurrent)
        txtCurrent = findViewById(R.id.txtCurrentName)
        txtEmpty = findViewById(R.id.txtEmpty)

        val width = (resources.displayMetrics.density * 150).toInt()
        adapter = VideoAdapter(width) { openPreview(it.file) }
        findViewById<RecyclerView>(R.id.recent).apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = this@MainActivity.adapter
        }

        findViewById<Button>(R.id.btnApply).setOnClickListener {
            val path = Prefs.selectedPath(this)
            if (path != null && File(path).isFile) openPreview(File(path))
            else say(R.string.none_selected)
        }
        findViewById<View>(R.id.cardCurrent).setOnClickListener { findViewById<Button>(R.id.btnApply).performClick() }
        findViewById<Button>(R.id.btnImportVideo).setOnClickListener { pickVideo.launch(arrayOf("video/*")) }
        findViewById<Button>(R.id.btnImportZip).setOnClickListener { startActivity(Intent(this, ImportZipActivity::class.java)) }
        findViewById<Button>(R.id.btnGallery).setOnClickListener { startActivity(Intent(this, GalleryActivity::class.java)) }
        findViewById<Button>(R.id.btnDialer).setOnClickListener { startActivity(Intent(this, DialerActivity::class.java)) }
        findViewById<Button>(R.id.btnCustomize).setOnClickListener { startActivity(Intent(this, CustomizeActivity::class.java)) }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<Button>(R.id.btnEnableKeyboard).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        findViewById<Button>(R.id.btnChooseKeyboard).setOnClickListener {
            val imm = getSystemService(android.view.inputmethod.InputMethodManager::class.java)
            imm.showInputMethodPicker()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun openPreview(file: File) {
        startActivity(Intent(this, PreviewActivity::class.java).putExtra(PreviewActivity.EXTRA_PATH, file.absolutePath))
    }

    private fun refresh() {
        Bg.io.execute {
            val list = VideoRepository.list(this)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                adapter.submit(list)
                txtEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                val path = Prefs.selectedPath(this)
                val f = path?.let { File(it) }
                if (f != null && f.isFile) {
                    txtCurrent.text = f.nameWithoutExtension
                    ThumbnailLoader.load(f, imgCurrent)
                } else {
                    txtCurrent.setText(R.string.none_selected)
                    imgCurrent.setImageResource(R.drawable.thumb_placeholder)
                }
            }
        }
    }
}
