package com.tyce.enhancer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object Links {
    /** Tries the native app first, then falls back to the browser. */
    fun open(context: Context, url: String, pkg: String?) {
        if (url.isBlank()) return
        val uri = Uri.parse(url)
        val base = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            if (pkg != null) {
                try { context.startActivity(Intent(base).setPackage(pkg)); return } catch (_: Throwable) {}
            }
            context.startActivity(base)
        } catch (t: Throwable) {
            Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
        }
    }
}
