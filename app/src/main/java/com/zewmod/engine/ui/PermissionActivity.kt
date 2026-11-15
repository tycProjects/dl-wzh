package com.zewmod.engine.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.zewmod.engine.R
import com.zewmod.engine.databinding.ActivityPermissionBinding
import com.zewmod.engine.util.PermissionHelper

class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding

    companion object {
        const val RC_OVERLAY = 100
        const val RC_VPN     = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        refreshPermissionUI()
    }

    private fun setupUI() {
        // Header animation
        binding.permHeaderCard.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_down)
        )
        binding.permStepsCard.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_up)
        )

        // Grant Overlay
        binding.btnGrantOverlay.setOnClickListener {
            PermissionHelper.requestOverlay(this, RC_OVERLAY)
        }

        // Grant VPN
        binding.btnGrantVpn.setOnClickListener {
            PermissionHelper.prepareVpn(this, RC_VPN)
        }

        // Battery optimization
        binding.btnGrantBattery.setOnClickListener {
            PermissionHelper.requestIgnoreBattery(this)
        }

        // Open App — only enabled when all granted
        binding.btnOpenApp.setOnClickListener {
            if (PermissionHelper.allGranted(this)) {
                goToMain()
            }
        }
    }

    private fun refreshPermissionUI() {
        val state = PermissionHelper.getState(this)

        // Overlay row
        setPermRow(
            binding.rowOverlay,
            binding.tvOverlayStatus,
            binding.btnGrantOverlay,
            state.overlay,
            "OVERLAY"
        )

        // VPN row
        setPermRow(
            binding.rowVpn,
            binding.tvVpnStatus,
            binding.btnGrantVpn,
            state.vpn,
            "VPN"
        )

        // Battery row
        setPermRow(
            binding.rowBattery,
            binding.tvBatteryStatus,
            binding.btnGrantBattery,
            state.battery,
            "PIN"
        )

        // Unlock main button
        binding.btnOpenApp.isEnabled = state.allOk
        binding.btnOpenApp.alpha = if (state.allOk) 1f else 0.4f

        if (state.allOk) {
            binding.permStatusText.text = "Cấp đủ 3 quyền — SẴN SÀNG"
            binding.permStatusText.setTextColor(getColor(R.color.zew_green))
            binding.btnOpenApp.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.pulse)
            )
        } else {
            val count = listOf(state.overlay, state.vpn, state.battery).count { it }
            binding.permStatusText.text = "Cấp đủ 3 quyền để mở khóa ($count/3)"
            binding.permStatusText.setTextColor(getColor(R.color.zew_yellow))
        }
    }

    private fun setPermRow(
        row: View,
        statusTv: android.widget.TextView,
        grantBtn: com.google.android.material.button.MaterialButton,
        granted: Boolean,
        label: String
    ) {
        if (granted) {
            statusTv.text = "ĐÃ CẤP"
            statusTv.setTextColor(getColor(R.color.zew_green))
            grantBtn.isEnabled = false
            grantBtn.text = "✓ ĐÃ CẤP"
            row.alpha = 0.7f
        } else {
            statusTv.text = "CHƯA CẤP"
            statusTv.setTextColor(getColor(R.color.zew_red))
            grantBtn.isEnabled = true
            grantBtn.text = "CẤP QUYỀN $label"
            row.alpha = 1f
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionUI()
        // Auto-proceed if all permissions already granted
        if (PermissionHelper.allGranted(this)) {
            goToMain()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        refreshPermissionUI()
    }

    private fun goToMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}
