package com.android.island.ai

import com.android.island.ai.DeviceActions.Setting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntentResolverTest {
    @Test
    fun parsesToggleAndMediaFromModelJson() {
        val raw = """{"actions":[{"type":"toggle","setting":"wifi","on":false},{"type":"media","op":"next"}]}"""
        assertEquals(
            listOf(
                AiAction.ToggleSetting(Setting.WIFI, false),
                AiAction.Media(AiAction.Media.Op.NEXT)
            ),
            IntentResolver.parse(raw)
        )
    }

    @Test
    fun ignoresUnknownTypesAndKeepsShellForThePolicyToJudge() {
        val raw = """{"actions":[{"type":"shell","command":"rm -rf /"},{"type":"toggle","setting":"NUKE","on":true}]}"""
        assertEquals(listOf(AiAction.Shell("rm -rf /", "perintah shell")), IntentResolver.parse(raw))
    }

    @Test
    fun parsesPublicApiActionsAndRequiresHttps() {
        val raw = """{"actions":[{"type":"api_find","query":"weather"},{"type":"api_get","url":"https://api.open-meteo.com/v1/forecast"},{"type":"api_get","url":"http://x.com"},{"type":"api_find","query":" "}]}"""
        assertEquals(
            listOf(AiAction.ApiFind("weather"), AiAction.ApiGet("https://api.open-meteo.com/v1/forecast")),
            IntentResolver.parse(raw)
        )
    }

    @Test
    fun chatAnswerMeansNoActions() {
        assertNull(IntentResolver.parse("""{"actions":[]}"""))
    }

    @Test
    fun questionsAreNotSentToModel() {
        assertEquals(false, IntentResolver.looksLikeCommand("apa itu fotosintesis?"))
        assertEquals(true, IntentResolver.looksLikeCommand("nyalain senter"))
    }

    @Test
    fun greetingsWithoutDeviceWordsAreNotSentToModel() {
        assertEquals(false, IntentResolver.looksLikeCommand("Kamu teh saha"))
        assertEquals(false, IntentResolver.looksLikeCommand("halo apa kabar"))
    }

    @Test
    fun scrollTimesIsClampedFromModelJson() {
        val raw = """{"actions":[{"type":"scroll","direction":"DOWN","times":99}]}"""
        assertEquals(listOf(AiAction.Scroll(down = true, times = AiCommandRouter.MAX_SCROLL_TIMES)), IntentResolver.parse(raw))
    }

    @Test
    fun playInAppIsParsedFromModelJson() {
        val raw = """{"actions":[{"type":"play_in_app","app":"Auralys","query":"Hati-Hati di Jalan"}]}"""
        assertEquals(listOf(AiAction.PlayInApp("Auralys", "Hati-Hati di Jalan")), IntentResolver.parse(raw))
    }

    @Test
    fun playInAppWithoutQueryIsIgnored() {
        assertNull(IntentResolver.parse("""{"actions":[{"type":"play_in_app","app":"Auralys","query":""}]}"""))
    }

    @Test
    fun generateImageIsParsedFromModelJson() {
        val raw = """{"actions":[{"type":"generate_image","prompt":"kucing oranye di pantai"}]}"""
        assertEquals(listOf(AiAction.GenerateImage("kucing oranye di pantai")), IntentResolver.parse(raw))
    }

    @Test
    fun emptyImagePromptIsIgnored() {
        assertNull(IntentResolver.parse("""{"actions":[{"type":"generate_image","prompt":" "}]}"""))
    }
}
