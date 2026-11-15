package com.zewmod.engine.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.zewmod.engine.R
import com.zewmod.engine.ZewModApp
import com.zewmod.engine.bridge.ZewModBridge
import com.zewmod.engine.databinding.FragmentHomeBinding
import com.zewmod.engine.model.GameTarget
import com.zewmod.engine.service.FloatingService
import com.zewmod.engine.service.ZewModVpnService
import com.zewmod.engine.util.PermissionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Available games
    private val games = listOf(
        GameTarget("ff",     "Free Fire",     "com.dts.freefireth",    R.drawable.ic_ff),
        GameTarget("ff_max", "Free Fire MAX", "com.dts.freefiremax",   R.drawable.ic_ff_max),
        GameTarget("ff_th",  "Free Fire TH",  "com.dts.freefireth_th", R.drawable.ic_ff)
    )
    private var selectedGameIdx = 0

    // Modes
    private val modes = listOf("CHỌN SẴN", "CHUYÊN SÂU", "LEGIT", "ỔN ĐỊNH")
    private var selectedModeIdx = 0

    // Launch modes
    private val launchModes = listOf("CARD", "CHIP", "CHUYÊN SÂU")
    private var selectedLaunchIdx = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGameSelector()
        setupModeSelector()
        setupLaunchMode()
        setupStatusCard()
        setupLaunchButton()
        animateEntrance()
    }

    private fun animateEntrance() {
        binding.homeHeaderCard.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down))
        binding.homeGameCard.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_delay1))
        binding.homeModeCard.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_delay2))
        binding.homeLaunchCard.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_delay3))
    }

    private fun setupGameSelector() {
        val prefs = (requireActivity().application as ZewModApp).prefs
        selectedGameIdx = games.indexOfFirst { it.id == prefs.selectedGame }.coerceAtLeast(0)
        updateGameUI()

        binding.btnGamePrev.setOnClickListener {
            selectedGameIdx = (selectedGameIdx - 1 + games.size) % games.size
            updateGameUI()
            prefs.selectedGame = games[selectedGameIdx].id
        }
        binding.btnGameNext.setOnClickListener {
            selectedGameIdx = (selectedGameIdx + 1) % games.size
            updateGameUI()
            prefs.selectedGame = games[selectedGameIdx].id
        }
    }

    private fun updateGameUI() {
        val game = games[selectedGameIdx]
        binding.tvSelectedGame.text = game.displayName
        binding.ivGameIcon.setImageResource(game.iconRes)
        binding.gameIndicator.text = "${selectedGameIdx + 1}/${games.size}"

        // Check if game installed
        val installed = isGameInstalled(game.packageName)
        binding.tvGameInstall.text = if (installed) "ĐÃ CÀI" else "CHƯA CÀI"
        binding.tvGameInstall.setTextColor(
            requireContext().getColor(if (installed) R.color.zew_green else R.color.zew_yellow)
        )
    }

    private fun isGameInstalled(pkg: String): Boolean {
        return try {
            requireContext().packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: Exception) { false }
    }

    private fun setupModeSelector() {
        val prefs = (requireActivity().application as ZewModApp).prefs
        selectedModeIdx = modes.indexOf(prefs.selectedMode).coerceAtLeast(0)
        updateModeUI()

        binding.btnModePrev.setOnClickListener {
            selectedModeIdx = (selectedModeIdx - 1 + modes.size) % modes.size
            updateModeUI()
            prefs.selectedMode = modes[selectedModeIdx]
        }
        binding.btnModeNext.setOnClickListener {
            selectedModeIdx = (selectedModeIdx + 1) % modes.size
            updateModeUI()
            prefs.selectedMode = modes[selectedModeIdx]
        }
    }

    private fun updateModeUI() {
        val mode = modes[selectedModeIdx]
        binding.tvSelectedMode.text = mode
        binding.modeIndicator.text = "${selectedModeIdx + 1}/${modes.size}"

        // Auto-apply mode features
        when (mode) {
            "CHỌN SẴN"   -> applyModePreset("track,head,scope")
            "CHUYÊN SÂU" -> applyModePreset("track,head,legit,freeze,scope,aim_assist")
            "LEGIT"       -> applyModePreset("legit,head")
            "ỔN ĐỊNH"    -> applyModePreset("track,scope")
        }
    }

    private fun applyModePreset(features: String) {
        (requireActivity().application as ZewModApp).prefs.lastFeatures = features
        if (ZewModBridge.engineRunning.value) {
            ZewModBridge.updateFeatures(features)
        }
    }

    private fun setupLaunchMode() {
        val prefs = (requireActivity().application as ZewModApp).prefs
        selectedLaunchIdx = launchModes.indexOf(prefs.launchMode).coerceAtLeast(0)
        updateLaunchModeUI()

        binding.btnLaunchModePrev.setOnClickListener {
            selectedLaunchIdx = (selectedLaunchIdx - 1 + launchModes.size) % launchModes.size
            updateLaunchModeUI()
            prefs.launchMode = launchModes[selectedLaunchIdx]
        }
        binding.btnLaunchModeNext.setOnClickListener {
            selectedLaunchIdx = (selectedLaunchIdx + 1) % launchModes.size
            updateLaunchModeUI()
            prefs.launchMode = launchModes[selectedLaunchIdx]
        }
    }

    private fun updateLaunchModeUI() {
        binding.tvSelectedLaunchMode.text = launchModes[selectedLaunchIdx]
    }

    private fun setupStatusCard() {
        lifecycleScope.launch {
            ZewModBridge.status.collectLatest { status ->
                binding.tvEngineStatus.text = status
                val (color, bg) = when {
                    status.contains("ACTIVE") ->
                        Pair(R.color.zew_green, R.color.zew_green_bg)
                    status.contains("LỖI") ->
                        Pair(R.color.zew_red, R.color.zew_red_bg)
                    else ->
                        Pair(R.color.zew_yellow, R.color.zew_yellow_bg)
                }
                binding.tvEngineStatus.setTextColor(requireContext().getColor(color))
                binding.statusDot.backgroundTintList = requireContext().getColorStateList(color)
            }
        }

        lifecycleScope.launch {
            ZewModBridge.engineRunning.collectLatest { running ->
                binding.btnLaunchMain.text = if (running) "ĐÓNG ENGINE" else "KHỞI CHẠY"
                binding.btnLaunchMain.backgroundTintList = requireContext().getColorStateList(
                    if (running) R.color.zew_red else R.color.zew_accent
                )
            }
        }
    }

    private fun setupLaunchButton() {
        binding.btnLaunchMain.setOnClickListener {
            if (ZewModBridge.engineRunning.value) {
                stopEngine()
            } else {
                startEngine()
            }
        }
    }

    private fun startEngine() {
        binding.btnLaunchMain.isEnabled = false
        binding.btnLaunchMain.text = "Đang khởi chạy..."

        val ctx = requireContext()
        ctx.startForegroundService(Intent(ctx, ZewModVpnService::class.java))
        ctx.startForegroundService(Intent(ctx, FloatingService::class.java))

        lifecycleScope.launch {
            kotlinx.coroutines.delay(1500)
            binding.btnLaunchMain.isEnabled = true
        }
    }

    private fun stopEngine() {
        val ctx = requireContext()
        ctx.startService(Intent(ctx, ZewModVpnService::class.java).apply {
            action = ZewModVpnService.ACTION_STOP
        })
        ctx.startService(Intent(ctx, FloatingService::class.java).apply {
            action = FloatingService.ACTION_STOP_FLOAT
        })
        ZewModBridge.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
