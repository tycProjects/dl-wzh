package com.zewmod.engine.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zewmod.engine.R
import com.zewmod.engine.databinding.ItemFeatureBinding
import com.zewmod.engine.model.Feature

class FeatureAdapter(
    private val onToggle: (Feature, Boolean) -> Unit,
    private val onSlider: (Feature, Float) -> Unit
) : ListAdapter<Feature, FeatureAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Feature>() {
            override fun areItemsTheSame(a: Feature, b: Feature) = a.id == b.id
            override fun areContentsTheSame(a: Feature, b: Feature) =
                a.enabled == b.enabled && a.sliderValue == b.sliderValue
        }
    }

    inner class VH(val binding: ItemFeatureBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemFeatureBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val feature = getItem(position)
        val ctx = holder.itemView.context

        with(holder.binding) {
            tvFeatureName.text = feature.nameVi
            tvFeatureDesc.text = feature.descVi

            // Category badge color
            val catColor = when (feature.category.name) {
                "COMBAT"   -> ctx.getColor(R.color.zew_red)
                "VISION"   -> ctx.getColor(R.color.zew_cyan)
                "UTILITY"  -> ctx.getColor(R.color.zew_yellow)
                else       -> ctx.getColor(R.color.zew_accent)
            }
            viewCategoryStripe.setBackgroundColor(catColor)

            // Toggle
            switchFeature.setOnCheckedChangeListener(null)
            switchFeature.isChecked = feature.enabled
            switchFeature.setOnCheckedChangeListener { _, checked ->
                onToggle(feature, checked)
                // Show/hide slider
                if (feature.hasSlider) {
                    sliderContainer.visibility = if (checked) View.VISIBLE else View.GONE
                }
            }

            // Slider
            if (feature.hasSlider) {
                sliderContainer.visibility = if (feature.enabled) View.VISIBLE else View.GONE
                tvSliderLabel.text = feature.sliderLabel
                sliderFeature.valueFrom = feature.sliderMin
                sliderFeature.valueTo   = feature.sliderMax
                sliderFeature.value     = feature.sliderValue.coerceIn(feature.sliderMin, feature.sliderMax)
                tvSliderVal.text = formatSliderVal(feature)
                sliderFeature.addOnChangeListener { _, value, fromUser ->
                    if (fromUser) {
                        feature.sliderValue = value
                        tvSliderVal.text = formatSliderVal(feature)
                        onSlider(feature, value)
                    }
                }
            } else {
                sliderContainer.visibility = View.GONE
            }

            // Card press ripple toggle
            root.setOnClickListener {
                switchFeature.isChecked = !switchFeature.isChecked
            }
        }
    }

    private fun formatSliderVal(feature: Feature): String {
        return when {
            feature.sliderMax <= 1f   -> "%.2f".format(feature.sliderValue)
            feature.sliderMax <= 2f   -> "%.1fs".format(feature.sliderValue)
            else                       -> "${feature.sliderValue.toInt()}"
        }
    }
}
