package com.zewmod.engine.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.zewmod.engine.R
import com.zewmod.engine.ZewModApp
import com.zewmod.engine.databinding.FragmentSettingsBinding
import com.zewmod.engine.service.FloatingService
import com.zewmod.engine.service.ZewModVpnService
import com.zewmod.engine.util.PermissionHelper

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = (requireActivity().application as ZewModApp).prefs

        // Menu opacity
        binding.sliderOpacity.value = prefs.menuOpacity * 100f
        binding.sliderOpacity.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefs.menuOpacity = value / 100f
                binding.tvOpacityVal.text = "${value.toInt()}%"
            }
        }
        binding.tvOpacityVal.text = "${(prefs.menuOpacity * 100).toInt()}%"

        // Menu size
        binding.sliderMenuSize.value = prefs.menuSize * 100f
        binding.sliderMenuSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefs.menuSize = value / 100f
                binding.tvMenuSizeVal.text = "${value.toInt()}%"
            }
        }
        binding.tvMenuSizeVal.text = "${(prefs.menuSize * 100).toInt()}%"

        // Exclude self team toggle
        val app = requireActivity().application as ZewModApp
        binding.switchExcludeTeam.isChecked = app.config.excludeSelfTeam
        binding.switchExcludeTeam.setOnCheckedChangeListener { _, checked ->
            app.updateConfig(app.config.copy(excludeSelfTeam = checked))
        }

        // Permission status
        refreshPermStatus()

        // Re-grant buttons
        binding.btnReGrantOverlay.setOnClickListener {
            PermissionHelper.requestOverlay(requireActivity(), 100)
        }
        binding.btnReGrantVpn.setOnClickListener {
            PermissionHelper.prepareVpn(requireActivity(), 101)
        }
        binding.btnReGrantBattery.setOnClickListener {
            PermissionHelper.requestIgnoreBattery(requireContext())
        }

        // Emergency stop
        binding.btnEmergencyStop.setOnClickListener {
            requireContext().startService(
                Intent(requireContext(), ZewModVpnService::class.java).apply {
                    action = ZewModVpnService.ACTION_STOP
                }
            )
            requireContext().startService(
                Intent(requireContext(), FloatingService::class.java).apply {
                    action = FloatingService.ACTION_STOP_FLOAT
                }
            )
        }

        // Version info
        binding.tvVersion.text = "ZewMod Engine v1.0"
        binding.tvBuildInfo.text = "Package: com.zewmod.engine\nMinSDK: 28 · TargetSDK: 34"
    }

    private fun refreshPermStatus() {
        val state = PermissionHelper.getState(requireContext())
        fun mark(tv: android.widget.TextView, granted: Boolean) {
            tv.text = if (granted) "✓ ĐÃ CẤP" else "✗ CHƯA CẤP"
            tv.setTextColor(requireContext().getColor(if (granted) R.color.zew_green else R.color.zew_red))
        }
        mark(binding.tvPermOverlay, state.overlay)
        mark(binding.tvPermVpn, state.vpn)
        mark(binding.tvPermBattery, state.battery)
    }

    override fun onResume() {
        super.onResume()
        refreshPermStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
