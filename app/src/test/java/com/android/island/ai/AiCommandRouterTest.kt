package com.android.island.ai

import com.android.island.ai.DeviceActions.Setting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCommandRouterTest {
    @Test
    fun fillerAndPunctuationDoNotChangeIntent() {
        val polite = AiCommandRouter.parseAll("tolong matiin wifi dong!")
        val plain = AiCommandRouter.parseAll("matiin wifi")
        assertEquals(plain, polite)
    }

    @Test
    fun softAskStillTurnsSettingOn() {
        val actions = AiCommandRouter.parseAll("senter dong")
        assertEquals(listOf(AiAction.ToggleSetting(Setting.FLASHLIGHT, true)), actions)
    }

    @Test
    fun shellPrefixNoLongerBypassesThePolicy() {
        val actions = AiCommandRouter.parseAll("shell: settings put global x 1.5 ya")
        assertTrue(actions.none { it is AiAction.Shell })
    }

    @Test
    fun cleanRemovesTrailingPunctuationOnly() {
        assertTrue(AiCommandRouter.clean("buka youtube?").endsWith("youtube"))
    }

    @Test
    fun opensRequestedAppDirectly() {
        assertEquals(listOf(AiAction.OpenApp("WhatsApp")), AiCommandRouter.parseAll("buka WhatsApp"))
    }

    @Test
    fun openAppThenScrollTwiceIsOneOrderedSequence() {
        val actions = AiCommandRouter.parseAll("buka tiktok lalu scroll 2 kali")
        assertEquals(listOf(AiAction.OpenApp("tiktok"), AiAction.Scroll(down = true, times = 2)), actions)
    }

    @Test
    fun swipeIsNotParsedAsScroll() {
        assertEquals(listOf(AiAction.Swipe(AiAction.Swipe.Direction.LEFT)), AiCommandRouter.parseAll("geser ke kiri"))
        assertEquals(listOf(AiAction.Swipe(AiAction.Swipe.Direction.RIGHT)), AiCommandRouter.parseAll("swipe right"))
    }

    @Test
    fun scrollUpWithoutCountIsOnce() {
        assertEquals(listOf(AiAction.Scroll(down = false, times = 1)), AiCommandRouter.parseAll("scroll ke atas"))
    }

    @Test
    fun indonesianCountWordsAndGulirAreUnderstood() {
        assertEquals(
            listOf(AiAction.Scroll(down = true, times = 3)),
            AiCommandRouter.parseAll("gulir ke bawah tiga kali")
        )
    }

    @Test
    fun scrollCountIsCappedToSafeMaximum() {
        val actions = AiCommandRouter.parseAll("scroll 500 kali")
        assertEquals(listOf(AiAction.Scroll(down = true, times = AiCommandRouter.MAX_SCROLL_TIMES)), actions)
    }

    @Test
    fun waitIsParsedInSeconds() {
        assertEquals(listOf(AiAction.Wait(3000L)), AiCommandRouter.parseAll("tunggu 3 detik"))
    }

    @Test
    fun waitIsCappedSoAnAssistantCannotStallForMinutes() {
        assertEquals(listOf(AiAction.Wait(AiCommandRouter.MAX_WAIT_MS)), AiCommandRouter.parseAll("tunggu 5 menit"))
    }
}
