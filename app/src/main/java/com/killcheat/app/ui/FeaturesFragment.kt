package com.killcheat.app.ui

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.killcheat.app.R
import com.killcheat.app.service.CheatService
import com.killcheat.app.state.CheatState

class FeaturesFragment : Fragment() {

    private lateinit var swTeleport: Switch
    private lateinit var swFreeze: Switch
    private lateinit var swGhost: Switch
    private lateinit var btnFF: Button
    private lateinit var btnFFMax: Button
    private var toneGen: ToneGenerator? = null

    private var isFFSelected = false
    private var isFFMaxSelected = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_features, container, false)

        toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

        swTeleport = v.findViewById(R.id.sw_teleport)
        swFreeze   = v.findViewById(R.id.sw_freeze)
        swGhost    = v.findViewById(R.id.sw_ghost)
        btnFF      = v.findViewById(R.id.btn_card_ff)
        btnFFMax   = v.findViewById(R.id.btn_card_ffmax)

        val btnInject   = v.findViewById<Button>(R.id.btn_inject)
        val tvCustomize = v.findViewById<TextView>(R.id.tv_customize_float)

        // Game card selection
        btnFF.setOnClickListener {
            isFFSelected = true; isFFMaxSelected = false
            updateCardState()
            beep(ToneGenerator.TONE_PROP_BEEP)
        }
        btnFFMax.setOnClickListener {
            isFFMaxSelected = true; isFFSelected = false
            updateCardState()
            beep(ToneGenerator.TONE_PROP_BEEP)
        }

        // Feature switches with beep
        swTeleport.setOnCheckedChangeListener { _, checked ->
            CheatState.isTeleportEnabled = checked
            beep(if (checked) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_BEEP2)
        }
        swFreeze.setOnCheckedChangeListener { _, checked ->
            CheatState.isFreezeEnabled = checked
            beep(if (checked) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_BEEP2)
        }
        swGhost.setOnCheckedChangeListener { _, checked ->
            CheatState.isGhostEnabled = checked
            beep(if (checked) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_BEEP2)
        }

        // Inject
        btnInject.setOnClickListener {
            if (!isFFSelected && !isFFMaxSelected) {
                Toast.makeText(context, getString(R.string.select_game_hint), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!CheatState.isTeleportEnabled && !CheatState.isFreezeEnabled && !CheatState.isGhostEnabled) {
                Toast.makeText(context, getString(R.string.enable_one_hint), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CheatState.targetGame = if (isFFSelected) "ff" else "ffmax"
            val svc = Intent(context, CheatService::class.java)
            svc.putExtra("target", CheatState.targetGame)
            requireContext().startForegroundService(svc)
            (activity as? MainActivity)?.requestOverlayPermission()
            beep(ToneGenerator.TONE_CDMA_ANSWER)
            Toast.makeText(context, getString(R.string.inject_success), Toast.LENGTH_SHORT).show()
        }

        // Navigate to float tab on tap
        tvCustomize.setOnClickListener {
            (activity as? MainActivity)?.navigateToFloatTab()
        }

        return v
    }

    private fun beep(tone: Int) = toneGen?.startTone(tone, 120)

    private fun updateCardState() {
        btnFF.alpha    = if (isFFSelected) 1f else 0.45f
        btnFFMax.alpha = if (isFFMaxSelected) 1f else 0.45f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toneGen?.release(); toneGen = null
    }
}
