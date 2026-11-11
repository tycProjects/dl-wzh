package com.logalyze.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.logalyze.app.ui.AppNav
import com.logalyze.app.ui.LogFile
import com.logalyze.app.ui.queryFile
import com.logalyze.app.ui.theme.LogalyzeTheme
import com.logalyze.app.ui.theme.resolveDark
import java.io.File

class MainActivity : ComponentActivity() {
    private val incoming = IncomingLog()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        val settings = SettingsState(applicationContext)
        setContent {
            val localized = remember(settings.language) {
                LocaleHelper.contextFor(this@MainActivity, settings.language)
            }
            val dark = resolveDark(settings.themeMode)
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration,
                LocalLayoutDirection provides
                    if (settings.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalSettings provides settings
            ) {
                LogalyzeTheme(settings.themeMode, settings.accent) {
                    AppNav(incoming)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** A launcher's "Share log" arrives here (as a file or as plain text). */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return

        val stream: Uri? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

        if (stream != null) {
            val file = queryFile(this, stream)
            when {
                file == null -> toast(R.string.err_read)
                file.size == 0L -> toast(R.string.err_empty)
                file.size > MAX_SHARED_SIZE -> toast(R.string.err_large)
                else -> incoming.file = file
            }
            return
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text.isNullOrBlank()) {
            toast(R.string.err_empty)
            return
        }
        if (text.length > MAX_SHARED_SIZE) {
            toast(R.string.err_large)
            return
        }
        try {
            val cached = File(cacheDir, "shared-log.txt")
            cached.writeText(text)
            incoming.file = LogFile(Uri.fromFile(cached), "shared-log.txt", cached.length())
        } catch (e: Exception) {
            toast(R.string.err_read)
        }
    }

    private fun toast(res: Int) {
        Toast.makeText(this, getString(res), Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val MAX_SHARED_SIZE = 5L * 1024 * 1024
    }
}
