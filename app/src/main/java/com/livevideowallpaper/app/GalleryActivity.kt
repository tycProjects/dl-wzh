package com.livevideowallpaper.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : BaseActivity() {

    private lateinit var adapter: VideoAdapter
    private lateinit var empty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        setupToolbar(R.string.gallery)
        empty = findViewById(R.id.txtEmpty)
        adapter = VideoAdapter(null) {
            startActivity(Intent(this, PreviewActivity::class.java).putExtra(PreviewActivity.EXTRA_PATH, it.file.absolutePath))
        }
        findViewById<RecyclerView>(R.id.grid).apply {
            layoutManager = GridLayoutManager(this@GalleryActivity, 2)
            this.adapter = this@GalleryActivity.adapter
        }
    }

    override fun onResume() {
        super.onResume()
        Bg.io.execute {
            val list = VideoRepository.list(this)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                adapter.submit(list)
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}
