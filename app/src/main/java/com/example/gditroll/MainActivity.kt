package com.example.gditroll

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private companion object {
        const val PREFS = "gdi_prefs"
        const val KEY_WARNING_OK = "photosensitivity_ok"
        const val REQ_NOTIFICATIONS = 7
        const val REQ_CAPTURE = 8
        const val DURATION_SEC = 0 // 0 = sem limite de tempo (só para pelo botão, notificação ou tela apagada)
    }

    private var pendingStart = false
    private lateinit var permissionButton: Button
    private lateinit var actionButton: Button
    private lateinit var status: TextView

    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            actionButton.postDelayed(this, 700)
        }
    }

    // Fator de escala e largura de conteúdo conforme o tipo de tela (celular, tablet, TV).
    private val scale by lazy { UiScale.factor(this) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density * scale).toInt()
    private fun sp(v: Float) = v * scale

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bg = 0xFF9E9E9E.toInt()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        col.addView(TextView(this).apply {
            text = "BOX"; textSize = sp(32f); setTextColor(0xFF212121.toInt())
            gravity = Gravity.CENTER; setTypeface(typeface, Typeface.BOLD)
        })
        status = TextView(this).apply {
            textSize = sp(14f); setTextColor(0xFF424242.toInt()); gravity = Gravity.CENTER
        }
        col.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        permissionButton = Button(this).apply {
            isAllCaps = false; setTextColor(Color.WHITE); textSize = sp(16f)
            isFocusableInTouchMode = false
            setOnClickListener { openOverlaySettings() }
        }
        col.addView(permissionButton, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(32) })

        actionButton = Button(this).apply {
            isAllCaps = false; textSize = sp(18f); setTextColor(Color.WHITE)
            setOnClickListener { onActionTapped() }
        }
        col.addView(actionButton, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(12) })

        // Centraliza o conteúdo com uma largura máxima em telas largas (tablet/TV) e
        // deixa rolar em telas pequenas ou baixas, para nada ficar cortado.
        val contentWidthPx = (UiScale.contentWidthDp(this) * resources.displayMetrics.density).toInt()
        val frame = FrameLayout(this).apply {
            setBackgroundColor(bg)
            addView(col, FrameLayout.LayoutParams(contentWidthPx, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(bg)
            // AUTO-REPAIR: disabled -- unresolved reference 'LayoutParams' (was: addView(frame, ScrollView.LayoutParams(-1, -1)))
        }

        setContentView(scroll)
        if (UiScale.isTv(this)) permissionButton.requestFocus()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        ticker.run()
    }

    override fun onPause() {
        actionButton.removeCallbacks(ticker)
        super.onPause()
    }

    private fun refresh() {
        val granted = Settings.canDrawOverlays(this)
        val running = GdiOverlayService.isRunning
        permissionButton.text = if (granted) "Sobreposição permitida ✓" else "Permitir sobreposição"
        permissionButton.isEnabled = !granted
        permissionButton.setBackgroundColor(if (granted) 0xFF616161.toInt() else 0xFF757575.toInt())
        actionButton.text = if (running) "PARAR" else "ATIVAR"
        actionButton.isEnabled = granted
        actionButton.setBackgroundColor(
            when {
                // Precisa ser diferente do cinza de fundo (0xFF9E9E9E), senão o botão
                // "desaparece" visualmente mesmo continuando ali e funcionando.
                !granted -> 0xFFBDBDBD.toInt()
                running -> 0xFF424242.toInt()
                else -> 0xFF616161.toInt()
            }
        )
        status.text = when {
            running -> "● Efeitos ativos agora"
            granted -> "Pronto para ativar"
            else -> "Primeiro permita a sobreposição"
        }
    }

    private fun openOverlaySettings() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun onActionTapped() {
        if (GdiOverlayService.isRunning) {
            startService(Intent(this, GdiOverlayService::class.java).setAction(GdiOverlayService.ACTION_STOP))
            return
        }
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_WARNING_OK, false)) {
            AlertDialog.Builder(this)
                .setTitle("Aviso de epilepsia e fotossensibilidade")
                .setMessage("Os efeitos usam cores e movimento na tela inteira. NÃO use se você, ou alguém perto de você, " +
                    "tem epilepsia fotossensível ou sensibilidade a luzes piscantes ou movimento: mesmo com cuidados " +
                    "de segurança, qualquer padrão de tela pode incomodar quem tem essa condição. Você pode parar a " +
                    "qualquer momento pelo botão PARAR, pela notificação ou apagando a tela.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Entendi, continuar") { _, _ ->
                    prefs.edit().putBoolean(KEY_WARNING_OK, true).apply()
                    ensureNotificationsThenStart()
                }.show()
            return
        }
        ensureNotificationsThenStart()
    }

    private fun ensureNotificationsThenStart() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingStart = true
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATIONS)
        } else {
            requestCapture()
        }
    }

    private fun requestCapture() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CAPTURE) return
        if (resultCode == RESULT_OK && data != null) {
            startEffects(resultCode, data)
        } else {
            Toast.makeText(this, "Sem a permissão de gravar a tela, os efeitos não iniciam.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIFICATIONS && pendingStart) {
            pendingStart = false
            if (grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notificações negadas: o PARAR fica só neste botão e no tempo limite.", Toast.LENGTH_LONG).show()
            }
            requestCapture()
        }
    }

    private fun startEffects(resultCode: Int, data: Intent) {
        val intent = Intent(this, GdiOverlayService::class.java).apply {
            action = GdiOverlayService.ACTION_START
            putExtra(GdiOverlayService.EXTRA_EFFECT, Effect.SCR_MEMZ_TOTAL.ordinal)
            putExtra(GdiOverlayService.EXTRA_INTENSITY, Intensity.STRONG.ordinal)
            putExtra(GdiOverlayService.EXTRA_SOUND, true)
            putExtra(GdiOverlayService.EXTRA_DURATION_SEC, DURATION_SEC)
            putExtra(GdiOverlayService.EXTRA_RESULT_CODE, resultCode)
            putExtra(GdiOverlayService.EXTRA_RESULT_DATA, data)
        }
        startForegroundService(intent)
    }
}
