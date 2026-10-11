package dev.tajim.jarvis.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object ExternalLinks {
    /**
     * Opens an https link with a plain ACTION_VIEW intent, so Android picks the right app or browser.
     * Returns false if the link is not https or nothing can handle it.
     */
    fun open(context: Context, url: String): Boolean {
        val uri = Uri.parse(url)
        if (uri.scheme != "https") return false
        val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
