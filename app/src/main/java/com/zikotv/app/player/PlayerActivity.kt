package com.zikotv.app.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.zikotv.app.ui.ZikoTheme

class PlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        setContent {
            ZikoTheme {
                PlayerScreen(
                    url = url,
                    title = title,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_URL = "stream_url"
        private const val EXTRA_TITLE = "stream_title"

        fun createIntent(context: Context, url: String, title: String = ""): Intent =
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
            }
    }
}
