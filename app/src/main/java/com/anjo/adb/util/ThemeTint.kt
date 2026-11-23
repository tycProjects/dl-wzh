package com.anjo.adb.util

import android.graphics.PorterDuff
import android.view.View
import android.widget.CompoundButton
import com.anjo.adb.data.SettingsRepository
import com.google.android.material.tabs.TabLayout

object ThemeTint {
    fun corAtual(context: android.content.Context): Int = SettingsRepository(context).corPainel()

    fun aplicarEmBotaoPill(view: View) {
        view.background = view.background.constantState?.newDrawable()?.mutate() ?: view.background
        view.background.setColorFilter(corAtual(view.context), PorterDuff.Mode.SRC_IN)
    }

    fun aplicarEmTabs(tabLayout: TabLayout) {
        tabLayout.setSelectedTabIndicatorColor(corAtual(tabLayout.context))
    }

    fun aplicarEmSwitch(sw: CompoundButton) {
        // AUTO-REPAIR: disabled -- unresolved reference 'setThumbTintList' (was: sw.setThumbTintList(android.content.res.ColorStateList.valueOf(corAtual(sw.context))))
    }
}
