package com.example.gditroll

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private companion object {
        const val PREFS = "gdi_prefs"
        const val KEY_WARNING_OK = "photosensitivity_ok"
        const val REQ_NOTIFICATIONS = 7
        const val REQ_CAPTURE = 8
        const val DURATION_SEC = 0
    }

    private var pendingStart = false
    private lateinit var permissionButton: Button
    private lateinit var actionButton: Button
    private lateinit var status: TextView

    private val ticker = object : Runnable {
        override fun run() { refresh(); actionButton.postDelayed(this, 700) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Raiz: fundo cinza, tudo centralizado
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF9E9E9E.toInt())
            setPadding(dp(32), dp(32), dp(32), dp(32))
        }

        // Título
        root.addView(TextView(this).apply {
            text = "BOX"
            textSize = 36f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
        }, LinearLayout.LayoutParams(-1, -2))

        // Status
        status = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF212121.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(status, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = dp(12) })

        // Botão 1 — Permissão
        permissionButton = Button(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            isAllCaps = false
            setOnClickListener { openOverlaySettings() }
        }
        root.addView(permissionButton, LinearLayout.LayoutParams(-1, dp(60)).also { it.topMargin = dp(40) })

        // Botão 2 — Ativar / Parar  (sempre visível com cor contrastante)
        actionButton = Button(this).apply {
            textSize = 20f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            isAllCaps = false
            setOnClickListener { onActionTapped() }
        }
        root.addView(actionButton, LinearLayout.LayoutParams(-1, dp(72)).also { it.topMargin = dp(12) })

        // Define explicitamente que a raiz ocupa a tela inteira (retrato ou paisagem).
        // Sem isso, em alguns aparelhos o layout fica do tamanho só do conteúdo e
        // encolhido num canto, deixando o resto da tela com a cor padrão da janela.
        setContentView(root, android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        ))
        refresh()
    }

    override fun onResume() { super.onResume(); ticker.run() }
    override fun onPause() { actionButton.removeCallbacks(ticker); super.onPause() }

    private fun refresh() {
        val granted = Settings.canDrawOverlays(this)
        val running = GdiOverlayService.isRunning

        permissionButton.text  = if (granted) "✓  Sobreposição permitida" else "1 · Permitir sobreposição"
        permissionButton.isEnabled = !granted
        permissionButton.setBackgroundColor(if (granted) 0xFF757575.toInt() else 0xFF424242.toInt())

        actionButton.text = if (running) "■  PARAR" else "▶  ATIVAR"
        // Nunca usa a mesma cor do fundo (cinza 9E). Verde escuro = pronto, vermelho = rodando,
        // cinza bem escuro = aguardando permissão. Todos contrastam com o fundo cinza claro.
        actionButton.setBackgroundColor(when {
            running  -> 0xFFC62828.toInt()
            granted  -> 0xFF2E7D32.toInt()
            else     -> 0xFF546E7A.toInt() // azul-acinzentado escuro — visível sobre o fundo
        })

        status.text = when {
            running -> "● Efeitos ativos agora"
            granted -> "Pronto · toque em ATIVAR"
            else    -> "Primeiro permita a sobreposição"
        }
    }

    private fun openOverlaySettings() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Sobreposição já está permitida.", Toast.LENGTH_SHORT).show()
            return
        }
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
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permita a sobreposição primeiro.", Toast.LENGTH_LONG).show()
            openOverlaySettings(); return
        }
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_WARNING_OK, false)) {
            AlertDialog.Builder(this)
                .setTitle("Aviso de epilepsia e fotossensibilidade")
                .setMessage(
                    "Os efeitos usam cores e movimento na tela inteira. NÃO use se você, ou " +
                    "alguém perto de você, tem epilepsia fotossensível ou sensibilidade a luzes " +
                    "piscantes ou movimento. Você pode parar a qualquer momento pelo botão PARAR " +
                    "ou pela notificação."
                )
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIFICATIONS && pendingStart) {
            pendingStart = false
            if (grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Notificações negadas: use o botão PARAR para encerrar.", Toast.LENGTH_LONG).show()
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
        if (resultCode == RESULT_OK && data != null) startEffects(resultCode, data)
        else Toast.makeText(this, "Sem a permissão de gravar a tela os efeitos não iniciam.", Toast.LENGTH_LONG).show()
    }

    private fun startEffects(resultCode: Int, data: Intent) {
        startForegroundService(Intent(this, GdiOverlayService::class.java).apply {
            action = GdiOverlayService.ACTION_START
            putExtra(GdiOverlayService.EXTRA_EFFECT,     Effect.SCR_MEMZ_TOTAL.ordinal)
            putExtra(GdiOverlayService.EXTRA_INTENSITY,  Intensity.STRONG.ordinal)
            putExtra(GdiOverlayService.EXTRA_SOUND,      true)
            putExtra(GdiOverlayService.EXTRA_DURATION_SEC, DURATION_SEC)
            putExtra(GdiOverlayService.EXTRA_RESULT_CODE, resultCode)
            putExtra(GdiOverlayService.EXTRA_RESULT_DATA,  data)
        })
        status.postDelayed({ refresh() }, 400)
    }
}
