package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentPersistenceTest {
    @Test fun noActionAndNotDoneAfterWorkIsChallenged() {
        assertTrue(AgentLoopPolicy.shouldChallengeStop(2, 0, modelSaysDone = false, hasActions = false, lastStepFailed = false, reply = ""))
    }

    @Test fun handingTheTaskBackToTheUserIsChallengedEvenWhenDone() {
        assertTrue(AgentLoopPolicy.shouldChallengeStop(2, 0, true, false, false, "Saya tidak bisa menekan Enter, silakan tekan sendiri."))
        assertTrue(AgentLoopPolicy.shouldChallengeStop(0, 0, false, false, false, "I cannot press the search key."))
    }

    @Test fun failedLastStepIsChallengedEvenWhenModelSaysDone() {
        assertTrue(AgentLoopPolicy.shouldChallengeStop(3, 1, true, false, true, "Selesai."))
    }

    @Test fun provenCompletionIsNotChallenged() {
        assertFalse(AgentLoopPolicy.shouldChallengeStop(3, 0, true, false, false, "Video pertama sudah diputar."))
    }

    @Test fun normalStopSentenceAfterReachingTheDestinationIsAccepted() {
        // The model stopped with a plain sentence but forgot done=true: the goal is reached, do not push it on.
        assertFalse(AgentLoopPolicy.shouldChallengeStop(3, 0, false, false, false, "Sudah di percakapan Ummi Faris."))
    }

    @Test fun plainChatBeforeAnyActionIsNotChallenged() {
        assertFalse(AgentLoopPolicy.shouldChallengeStop(0, 0, false, false, false, "Halo! Ada yang bisa dibantu?"))
    }

    @Test fun challengesAreBoundedAndNeverBlockRealActions() {
        assertFalse(AgentLoopPolicy.shouldChallengeStop(2, AgentLoopPolicy.MAX_CHALLENGES, false, false, true, "tidak bisa"))
        assertFalse(AgentLoopPolicy.shouldChallengeStop(2, 0, false, true, true, "tidak bisa"))
    }

    @Test fun decideStillStopsWithoutActions() {
        assertTrue(AgentLoopPolicy.decide(1, 1_000, false, false, 0) is AgentLoopPolicy.Decision.Stop)
    }

    @Test fun persistencePromptListsTheNewRoutesAndCarriesTheScreen() {
        val p = AgentRecoveryPolicy.persistencePrompt(2, "[1] Cari (bisa diketuk)", false)
        assertTrue(p.contains("press_enter"))
        assertTrue(p.contains("click_index"))
        assertTrue(p.contains("tap dengan x,y"))
        assertTrue(p.contains("[1] Cari"))
        assertTrue(p.contains("ke-2 dari ${AgentLoopPolicy.MAX_CHALLENGES}"))
        assertFalse(p.contains("Shizuku siap"))
        assertTrue(AgentRecoveryPolicy.persistencePrompt(1, "", true).contains("read_screen"))
    }
}

class ScreenElementsTest {
    private val search = ScreenElements.Element(1, "Cari", true, true, 100, 100, 900, 200)
    private val icon = ScreenElements.Element(2, "Search button", false, false, 800, 110, 880, 190)

    @Test fun formatShowsIndexLabelKindAndCenter() {
        val text = ScreenElements.format(listOf(search, icon))
        assertTrue(text.contains("[1] Cari (kolom teks, sedang fokus, tengah 500,150)"))
        assertTrue(text.contains("[2] Search button (bisa diketuk, tengah 840,150)"))
    }

    @Test fun lookupByIndexAndSmallestContainingLabel() {
        val all = listOf(search, icon)
        assertEquals("Search button", ScreenElements.byIndex(all, 2)?.label)
        assertNull(ScreenElements.byIndex(all, 9))
        assertEquals("Search button", ScreenElements.labelAt(all, 840, 150))
        assertEquals("Cari", ScreenElements.labelAt(all, 200, 150))
        assertNull(ScreenElements.labelAt(all, 5, 5))
    }
}
