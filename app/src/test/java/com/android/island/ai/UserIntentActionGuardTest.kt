package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserIntentActionGuardTest {
    @Test fun aiCannotAddAnUnrequestedSystemToggle() {
        assertFalse(UserIntentActionGuard.allows(
            AiAction.ToggleSetting(DeviceActions.Setting.WIFI, false),
            "tolong buka YouTube"
        ))
        assertTrue(UserIntentActionGuard.allows(
            AiAction.ToggleSetting(DeviceActions.Setting.WIFI, false),
            "tolong matikan WiFi"
        ))
    }

    @Test fun sensitiveActionsNeedMatchingOriginalUserIntent() {
        assertFalse(UserIntentActionGuard.allows(AiAction.Call("081234567890"), "buka WhatsApp"))
        assertTrue(UserIntentActionGuard.allows(AiAction.Call("081234567890"), "telepon 0812 3456 7890"))
        assertFalse(UserIntentActionGuard.allows(AiAction.ForceStop("WhatsApp"), "buka WhatsApp"))
        assertTrue(UserIntentActionGuard.allows(AiAction.ForceStop("WhatsApp"), "paksa berhenti WhatsApp"))
    }

    @Test fun uiStepsCanContinueOnlyFromAnExplicitUserTask() {
        assertTrue(UserIntentActionGuard.allows(AiAction.Click("Cari"), "buka YouTube lalu cari lagu"))
        assertFalse(UserIntentActionGuard.allows(AiAction.Click("Cari"), "apa arti tombol cari?"))
    }

    @Test fun assistantProseIsNeverAnAuthorizationSource() {
        assertFalse(UserIntentActionGuard.allows(AiAction.OpenApp("YouTube"), "Jelaskan fungsi YouTube"))
        assertFalse(UserIntentActionGuard.allows(AiAction.ReadScreen, "Apa itu aksesibilitas?"))
    }

    @Test fun newUiPrimitivesFollowTheSameRuleAsClick() {
        val task = "buka tiktok lalu cari preset"
        assertTrue(UserIntentActionGuard.allows(AiAction.PressEnter, task))
        assertTrue(UserIntentActionGuard.allows(AiAction.TapXY(540, 210), task))
        assertTrue(UserIntentActionGuard.allows(AiAction.ClickIndex(3), task))
        assertFalse(UserIntentActionGuard.allows(AiAction.PressEnter, "apa fungsi tombol enter?"))
    }

    @Test fun typingIsAllowedForASearchTaskWithoutTheWordType() {
        assertTrue(UserIntentActionGuard.allows(AiAction.TypeText("preset"), "buka tiktok lalu cari preset"))
        assertFalse(UserIntentActionGuard.allows(AiAction.TypeText("preset"), "apa itu tiktok?"))
    }
}
