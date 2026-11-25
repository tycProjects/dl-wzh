package com.livevideowallpaper.app

import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar

abstract class BaseActivity : AppCompatActivity() {

    protected fun setupToolbar(@StringRes title: Int) {
        val tb = findViewById<MaterialToolbar>(R.id.toolbar)
        tb.setTitle(title)
        tb.setNavigationOnClickListener { finish() }
    }

    protected fun say(msg: String) {
        if (isFinishing || isDestroyed) return
        Snackbar.make(findViewById<View>(android.R.id.content), msg, Snackbar.LENGTH_LONG).show()
    }

    protected fun say(@StringRes msg: Int) = say(getString(msg))

    protected fun sayAsync(msg: Int) = runOnUiThread { say(msg) }
}
