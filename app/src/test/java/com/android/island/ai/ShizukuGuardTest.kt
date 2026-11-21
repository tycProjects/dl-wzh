package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuGuardTest {
    @Test
    fun allowsToggleWhenUserNamedTheSetting() {
        assertTrue(ShizukuGuard.allowedToggle(DeviceActions.Setting.WIFI, "tolong matikan WiFi"))
        assertTrue(ShizukuGuard.allowedToggle(DeviceActions.Setting.FLASHLIGHT, "nyalakan senter"))
    }

    @Test
    fun refusesToggleWhenUserDidNotAskForIt() {
        assertFalse(ShizukuGuard.allowedToggle(DeviceActions.Setting.WIFI, "putar lagu jazz"))
        assertFalse(ShizukuGuard.allowedToggle(DeviceActions.Setting.BLUETOOTH, ""))
    }
}
