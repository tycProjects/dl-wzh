package com.killcheat.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.killcheat.app.R
import com.killcheat.app.network.KillAuthApi
import com.killcheat.app.util.LanguageManager

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME  = "killcheat_auth"
        private const val KEY_SAVED   = "saved_key"
        private const val APP_ID      = "killcheat"
        private const val APP_VERSION = "1.0.0"
        private const val GETKEY_URL  = "https://killcheat.onrender.com/getkey"
    }

    private lateinit var etKey        : EditText
    private lateinit var tvError      : TextView
    private lateinit var btnLogin     : TextView
    private lateinit var btnGetKey    : TextView
    private lateinit var flLoading    : FrameLayout
    private lateinit var llNotif      : LinearLayout
    private lateinit var tvNotifTitle : TextView
    private lateinit var tvNotifBody  : TextView
    private lateinit var llUpdate     : LinearLayout
    private lateinit var tvUpdateVer  : TextView
    private lateinit var btnUpdateNow : TextView
    private lateinit var tvVersion    : TextView

    override fun attachBaseContext(newBase: Context) {
        val lang = LanguageManager.getSavedLanguage(newBase)
        super.attachBaseContext(LanguageManager.applyLanguage(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etKey        = findViewById(R.id.et_key)
        tvError      = findViewById(R.id.tv_error)
        btnLogin     = findViewById(R.id.btn_login)
        btnGetKey    = findViewById(R.id.btn_getkey)
        flLoading    = findViewById(R.id.fl_loading)
        llNotif      = findViewById(R.id.ll_notification_banner)
        tvNotifTitle = findViewById(R.id.tv_notif_title)
        tvNotifBody  = findViewById(R.id.tv_notif_body)
        llUpdate     = findViewById(R.id.ll_update_banner)
        tvUpdateVer  = findViewById(R.id.tv_update_version)
        btnUpdateNow = findViewById(R.id.btn_update_now)
        tvVersion    = findViewById(R.id.tv_version_badge)

        tvVersion.text = "v$APP_VERSION"

        // Check for saved key — auto-login silently
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedKey = prefs.getString(KEY_SAVED, "")
        if (!savedKey.isNullOrBlank()) {
            etKey.setText(savedKey)
            performLogin(savedKey, autoLogin = true)
            return
        }

        // Fetch server notifications + update check without key
        checkServerStatus()

        btnLogin.setOnClickListener {
            val input = etKey.text.toString().trim()
            if (input.isEmpty()) {
                showError("Key cannot be empty")
                return@setOnClickListener
            }
            performLogin(input, autoLogin = false)
        }

        etKey.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnLogin.performClick()
                true
            } else false
        }

        btnGetKey.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GETKEY_URL)))
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Pre-login: check server for updates / notifications
    // ─────────────────────────────────────────────────────────
    private fun checkServerStatus() {
        Thread {
            try {
                val result = KillAuthApi.checkUpdate(APP_ID, APP_VERSION)
                runOnUiThread {
                    // Announcement / notification banner
                    val announcement = result.optString("announcement", "")
                    if (announcement.isNotBlank()) {
                        tvNotifTitle.text = "ANNOUNCEMENT"
                        tvNotifBody.text  = announcement
                        llNotif.visibility = View.VISIBLE
                    }
                    // Update banner
                    if (result.optBoolean("update_available", false)) {
                        val newVer = result.optString("current_version", "")
                        tvUpdateVer.text = "New version: $newVer available"
                        llUpdate.visibility = View.VISIBLE
                        val updateUrl = result.optString("update_url", "")
                        btnUpdateNow.setOnClickListener {
                            if (updateUrl.isNotBlank())
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
                        }
                    }
                }
            } catch (_: Exception) { /* network unavailable — silent */ }
        }.start()
    }

    // ─────────────────────────────────────────────────────────
    //  Verify key against KillAuth server
    // ─────────────────────────────────────────────────────────
    private fun performLogin(key: String, autoLogin: Boolean) {
        showLoading(true)
        hideError()

        Thread {
            try {
                val deviceId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: ""

                val response = KillAuthApi.verify(
                    appId    = APP_ID,
                    key      = key,
                    version  = APP_VERSION,
                    deviceId = deviceId
                )

                runOnUiThread {
                    showLoading(false)

                    if (response.optBoolean("success", false)) {
                        // Save key for next launch
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putString(KEY_SAVED, key)
                            .apply()

                        // Handle server notification shown at launch
                        val notifications = response.optJSONArray("notifications")
                        if (notifications != null && notifications.length() > 0) {
                            val first = notifications.getJSONObject(0)
                            tvNotifTitle.text  = first.optString("title", "NOTICE")
                            tvNotifBody.text   = first.optString("message", "")
                            llNotif.visibility = View.VISIBLE
                        }

                        // Check update returned in verify response
                        val app = response.optJSONObject("app")
                        // proceed to main
                        goToMain()

                    } else {
                        val status = response.optString("status", "")
                        when (status) {
                            "update_required" -> {
                                val newVer   = response.optString("new_version", "")
                                val updateUrl = response.optString("update_url", "")
                                tvUpdateVer.text   = "Required: v$newVer"
                                llUpdate.visibility = View.VISIBLE
                                btnUpdateNow.setOnClickListener {
                                    if (updateUrl.isNotBlank())
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
                                }
                                showError("Update required to continue.")
                            }
                            "paused"       -> showError("App is currently paused.")
                            "maintenance"  -> showError("Server under maintenance.")
                            else           -> {
                                val err = response.optString("error", "Invalid key")
                                showError(err)
                                // If auto-login fails, clear saved key
                                if (autoLogin) {
                                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                        .edit().remove(KEY_SAVED).apply()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    showError("Connection failed: ${e.message}")
                }
            }
        }.start()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun showLoading(show: Boolean) {
        flLoading.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled   = !show
        btnGetKey.isEnabled  = !show
    }

    private fun showError(msg: String) {
        tvError.text       = msg
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}
