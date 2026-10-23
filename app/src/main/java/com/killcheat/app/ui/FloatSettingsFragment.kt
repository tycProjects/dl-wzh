package com.killcheat.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.killcheat.app.R
import com.killcheat.app.state.FloatState

class FloatSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_float_settings, container, false)

        val tvSize    = v.findViewById<TextView>(R.id.tv_size_value)
        val tvOpacity = v.findViewById<TextView>(R.id.tv_opacity_value)
        val seekSize  = v.findViewById<SeekBar>(R.id.seek_size)
        val seekOp    = v.findViewById<SeekBar>(R.id.seek_opacity)

        seekSize.progress  = FloatState.buttonSize
        seekOp.progress    = FloatState.buttonOpacity
        tvSize.text        = "${FloatState.buttonSize} dp"
        tvOpacity.text     = "${FloatState.buttonOpacity}%"

        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, user: Boolean) {
                FloatState.buttonSize = p
                tvSize.text = "$p dp"
                FloatState.notifyChange()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        seekOp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, user: Boolean) {
                FloatState.buttonOpacity = p
                tvOpacity.text = "$p%"
                FloatState.notifyChange()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        return v
    }
}
