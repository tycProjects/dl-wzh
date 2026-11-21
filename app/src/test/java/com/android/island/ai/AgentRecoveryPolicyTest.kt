package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRecoveryPolicyTest {
    @Test fun failedActionReplayIsBlockedButNewActionIsAllowed() {
        assertTrue(AgentRecoveryPolicy.shouldBlockReplay("Click(text=Kirim)", setOf("Click(text=Kirim)")))
        assertFalse(AgentRecoveryPolicy.shouldBlockReplay("Back", setOf("Click(text=Kirim)")))
    }

    @Test fun completedActionReplayIsBlocked() {
        assertTrue(AgentRecoveryPolicy.shouldBlockCompletedReplay("OpenApp(query=TikTok)", setOf("OpenApp(query=TikTok)")))
        assertFalse(AgentRecoveryPolicy.shouldBlockCompletedReplay("TapFirstResult", setOf("OpenApp(query=TikTok)")))
    }

    @Test fun recoveryForbidsRepeatingFailedAction() {
        val prompt = AgentRecoveryPolicy.alternativePrompt("Click(text=Kirim)", 1, false)
        assertTrue(prompt.contains("Jangan mengeluarkan ulang aksi yang sama"))
        assertTrue(prompt.contains("Click(text=Kirim)"))
    }

    @Test fun recoveryDoesNotPretendShizukuIsAvailable() {
        val prompt = AgentRecoveryPolicy.alternativePrompt("Shell(command=x)", 1, false)
        assertTrue(prompt.contains("Shizuku tidak tersedia"))
        assertFalse(prompt.contains("Shizuku terdeteksi siap"))
    }

    @Test fun recoveryMentionsShizukuOnlyWhenReady() {
        val prompt = AgentRecoveryPolicy.alternativePrompt("Diagnose(kind=BATTERY)", 1, true)
        assertTrue(prompt.contains("Shizuku terdeteksi siap"))
    }

    @Test fun guardRejectionTellsModelWhyAndKeepsTaskAlive() {
        val prompt = AgentRecoveryPolicy.rejectedActionPrompt("TypeText(text=Ummi Faris)", "buka kontak ummi faris", "", 1)
        assertTrue(prompt.contains("AKSI DITOLAK"))
        assertTrue(prompt.contains("buka kontak ummi faris"))
        assertTrue(prompt.contains("app_home"))
        assertTrue(prompt.contains("read_screen"))
    }

    @Test fun persistenceLadderMentionsGoingBackToAppHome() {
        assertTrue(AgentRecoveryPolicy.persistencePrompt(1, "", false).contains("app_home"))
    }
}
