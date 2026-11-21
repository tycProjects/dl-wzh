package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmPolicyTest {
    @Test fun sensitiveActionsNeedConfirmation() {
        assertTrue(ConfirmPolicy.needsConfirmation(AiAction.Call("08123")))
        assertTrue(ConfirmPolicy.needsConfirmation(AiAction.Sms("08123", "halo")))
        assertTrue(ConfirmPolicy.needsConfirmation(AiAction.ForceStop("WhatsApp")))
        assertTrue(ConfirmPolicy.needsConfirmation(AiAction.Shell("settings put system screen_brightness 80", "kecerahan")))
    }

    @Test fun readOnlyActionsDoNotNeedConfirmation() {
        assertFalse(ConfirmPolicy.needsConfirmation(AiAction.ReadScreen))
        assertFalse(ConfirmPolicy.needsConfirmation(AiAction.Shell("getprop ro.build.version.release", "versi")))
        assertFalse(ConfirmPolicy.needsConfirmation(AiAction.NotificationSummary))
    }

    @Test fun replyWordsAreRecognised() {
        assertTrue(ConfirmPolicy.isYes("Ya."))
        assertTrue(ConfirmPolicy.isNo("batal"))
        assertFalse(ConfirmPolicy.isYes("kirim sekarang"))
    }
}
