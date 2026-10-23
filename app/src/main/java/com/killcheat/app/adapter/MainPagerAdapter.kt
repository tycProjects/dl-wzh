package com.killcheat.app.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.killcheat.app.ui.FeaturesFragment
import com.killcheat.app.ui.FloatSettingsFragment
import com.killcheat.app.ui.SettingsFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 3
    override fun createFragment(position: Int): Fragment = when (position) {
        1    -> FloatSettingsFragment()
        2    -> SettingsFragment()
        else -> FeaturesFragment()
    }
}
