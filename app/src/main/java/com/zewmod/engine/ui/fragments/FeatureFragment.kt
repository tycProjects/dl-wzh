package com.zewmod.engine.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.zewmod.engine.R
import com.zewmod.engine.bridge.ZewModBridge
import com.zewmod.engine.databinding.FragmentFeatureBinding
import com.zewmod.engine.model.Feature
import com.zewmod.engine.model.FeatureCategory
import com.zewmod.engine.model.ZewModFeatures
import com.zewmod.engine.ui.adapter.FeatureAdapter

class FeatureFragment : Fragment(R.layout.fragment_feature) {

    private var _binding: FragmentFeatureBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FeatureAdapter
    private var currentCategory = FeatureCategory.COMBAT

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeatureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupRecycler()
        loadFeatures(currentCategory)
    }

    private fun setupTabs() {
        binding.featureTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentCategory = when (tab.position) {
                    0 -> FeatureCategory.COMBAT
                    1 -> FeatureCategory.VISION
                    2 -> FeatureCategory.UTILITY
                    else -> FeatureCategory.COMBAT
                }
                loadFeatures(currentCategory)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupRecycler() {
        adapter = FeatureAdapter(
            onToggle = { feature, enabled ->
                feature.enabled = enabled
                applyFeatures()
            },
            onSlider = { feature, value ->
                feature.sliderValue = value
                ZewModBridge.updateSlider(feature.id, value)
            }
        )
        binding.rvFeatures.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFeatures.adapter = adapter
    }

    private fun loadFeatures(category: FeatureCategory) {
        val features = ZewModFeatures.getByCategory(category)
        adapter.submitList(features)
        binding.tvFeatureCount.text = "${features.count { it.enabled }}/${features.size} đang bật"
    }

    private fun applyFeatures() {
        ZewModBridge.applyAllFeatures()
        // Refresh count
        val features = ZewModFeatures.getByCategory(currentCategory)
        binding.tvFeatureCount.text = "${features.count { it.enabled }}/${features.size} đang bật"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
