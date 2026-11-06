package com.android.island.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IslandBaselineProfile {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startupAndDashboard() = baselineProfileRule.collect(
        packageName = "com.android.island"
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg("com.android.island")), 5_000)
    }
}
