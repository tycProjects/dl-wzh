package com.livevideowallpaper.app

import android.os.Bundle
import android.widget.RadioButton
import com.google.android.material.materialswitch.MaterialSwitch

/** Settings for the status bar shown on the video lock screen, with a live preview. */
class StatusStyleActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status_style)
        setupToolbar(R.string.status_style)

        val preview = findViewById<StatusBarView>(R.id.statusPreview)
        preview.style = Prefs.battStyle(this)
        preview.notifAnim = Prefs.notifAnim(this)

        val swStatus = findViewById<MaterialSwitch>(R.id.swStatus)
        swStatus.isChecked = Prefs.statusOn(this)
        swStatus.setOnCheckedChangeListener { _, on -> Prefs.setStatusOn(this, on) }

        val swNotif = findViewById<MaterialSwitch>(R.id.swNotif)
        swNotif.isChecked = Prefs.notifAnim(this)
        swNotif.setOnCheckedChangeListener { _, on ->
            Prefs.setNotifAnim(this, on)
            preview.notifAnim = on
        }

        val swCharge = findViewById<MaterialSwitch>(R.id.swCharge)
        swCharge.setOnCheckedChangeListener { _, on ->
            preview.previewCharging = if (on) true else null
        }

        val rbClassic = findViewById<RadioButton>(R.id.rbClassic)
        val rbPill = findViewById<RadioButton>(R.id.rbPill)
        if (Prefs.battStyle(this) == 1) rbPill.isChecked = true else rbClassic.isChecked = true
        rbClassic.setOnClickListener {
            Prefs.setBattStyle(this, 0)
            preview.style = 0
        }
        rbPill.setOnClickListener {
            Prefs.setBattStyle(this, 1)
            preview.style = 1
        }
    }
}
