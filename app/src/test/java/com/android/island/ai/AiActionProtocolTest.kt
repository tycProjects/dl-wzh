package com.android.island.ai

import com.android.island.ai.DeviceActions.Setting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiActionProtocolTest {
    @Test
    fun envelopeGivesReplyAndValidActions() {
        val raw = """{"reply":"Oke, aku buka dan putar.","actions":[{"type":"open_app","query":"musik"},{"type":"media","op":"PLAY"}]}"""
        val env = AiActionProtocol.parse(raw)
        assertEquals("Oke, aku buka dan putar.", env.reply)
        assertEquals(
            listOf(AiAction.OpenApp("musik"), AiAction.Media(AiAction.Media.Op.PLAY)),
            env.actions
        )
    }

    @Test
    fun clickActionAcceptsLegacyQueryFieldWithoutBreakingTheEnvelope() {
        val env = AiActionProtocol.parse("""{"reply":"","actions":[{"type":"click","query":"Cari"}],"done":false}""")
        assertEquals(listOf(AiAction.Click("Cari")), env.actions)
        assertFalse(env.broken)
    }

    @Test
    fun playSongEnvelopeIsParsed() {
        val env = AiActionProtocol.parse("""{"reply":"Oke, aku putar.","actions":[{"type":"play_song","query":"teh hijau"}]}""")
        assertEquals(listOf(AiAction.PlaySong("teh hijau")), env.actions)
    }

    @Test
    fun richActionsAreParsedAndSanitized() {
        val raw = """{"reply":"ok","actions":[
            {"type":"timer","seconds":300},
            {"type":"system_key","key":"home"},
            {"type":"call","number":"+62 812-3456"},
            {"type":"volume_set","percent":250},
            {"type":"alarm","hour":25,"minute":0,"message":"x"}
        ]}"""
        val env = AiActionProtocol.parse(raw)
        assertEquals(
            listOf(
                AiAction.Timer(300),
                AiAction.SystemKey("HOME"),
                AiAction.Call("+628123456"),
                AiAction.VolumeSet(100)
            ),
            env.actions
        )
    }

    @Test
    fun standaloneToolCallWrappedInTagsIsParsed() {
        val raw = "<tool_call>\n{\"type\":\"api_find\",\"query\":\"news\"}\n</tool_call>"
        val env = AiActionProtocol.parse(raw)
        assertEquals(listOf(AiAction.ApiFind("news")), env.actions)
        assertFalse(env.broken)
    }

    @Test
    fun namedToolCallWithArgumentsIsParsed() {
        val raw = "<tool_call>{\"name\":\"shell\",\"arguments\":{\"command\":\"getprop ro.build.version.release\"}}</tool_call>"
        val env = AiActionProtocol.parse(raw)
        assertEquals(listOf(AiAction.Shell("getprop ro.build.version.release", "perintah shell")), env.actions)
    }

    @Test
    fun openAiFunctionToolCallWithStringArgumentsIsParsed() {
        val raw = "{\"type\":\"function\",\"function\":{\"name\":\"shell\",\"arguments\":\"{\\\"command\\\":\\\"getprop ro.build.version.release\\\"}\"}}"
        val env = AiActionProtocol.parse(raw)
        assertEquals(listOf(AiAction.Shell("getprop ro.build.version.release", "perintah shell")), env.actions)
    }


    @Test
    fun malformedToolWrapperAndResultTagDoesNotLeakAndStillParses() {
        val raw = """<tool_call>
{"type":"api_find","query":"news"}
</result>
</tool_call>"""
        val env = AiActionProtocol.parse(raw)
        assertEquals(listOf(AiAction.ApiFind("news")), env.actions)
        assertEquals("", env.reply)
        assertFalse(env.broken)
    }

    @Test
    fun preservesReadableExplanationAroundToolMarkup() {
        val raw = "Saya menemukan aplikasi yang dimaksud.\n<tool_call>{\"type\":\"open_app\",\"query\":\"TikTok\"}</tool_call>\nSaya akan memeriksa hasilnya."
        val env = AiActionProtocol.parse(raw)
        assertEquals(listOf(AiAction.OpenApp("TikTok")), env.actions)
        assertTrue(env.reply.contains("Saya menemukan aplikasi yang dimaksud."))
        assertTrue(env.reply.contains("Saya akan memeriksa hasilnya."))
    }

    @Test
    fun multipleStandaloneToolCallsAreParsedInOrder() {
        val raw = """<tool_call>{"type":"api_find","query":"weather"}</tool_call>
            <tool_call>{"name":"shell","arguments":{"command":"getprop ro.build.version.release"}}</tool_call>"""
        val env = AiActionProtocol.parse(raw)
        assertEquals(
            listOf(
                AiAction.ApiFind("weather"),
                AiAction.Shell("getprop ro.build.version.release", "perintah shell")
            ),
            env.actions
        )
    }

    @Test
    fun nativeOpenAiToolCallsAreNormalizedToSafeEnvelope() {
        val message = org.json.JSONObject().put("content", "").put(
            "tool_calls",
            org.json.JSONArray().put(
                org.json.JSONObject()
                    .put("type", "function")
                    .put("function", org.json.JSONObject()
                        .put("name", "api_find")
                        .put("arguments", "{\"query\":\"news\"}"))
            )
        )
        val normalized = AiActionProtocol.envelopeFromToolCalls(message)
        assertNotNull(normalized)
        assertEquals(listOf(AiAction.ApiFind("news")), AiActionProtocol.parse(normalized!!).actions)
    }


    @Test
    fun genericNativeAndroidActionToolCallPreservesActualActionType() {
        val message = org.json.JSONObject().put("content", "").put(
            "tool_calls",
            org.json.JSONArray().put(
                org.json.JSONObject().put("type", "function").put(
                    "function", org.json.JSONObject()
                        .put("name", "android_action")
                        .put("arguments", "{\"type\":\"search\",\"query\":\"berita terbaru\"}")
                )
            )
        )
        val normalized = AiActionProtocol.envelopeFromToolCalls(message)
        assertNotNull(normalized)
        assertEquals(listOf(AiAction.WebSearch("berita terbaru")), AiActionProtocol.parse(normalized!!).actions)
    }

    @Test
    fun malformedActionsFieldIsMarkedBroken() {
        val env = AiActionProtocol.parse("""{"reply":"ok","actions":"search"}""")
        assertTrue(env.broken)
        assertTrue(env.actions.isEmpty())
    }

    @Test
    fun unrecognizedToolWrapperIsNotReturnedAsRawMarkup() {
        val env = AiActionProtocol.parse("<tool_call>{\"name\":\"unknown_action\",\"arguments\":{}}</tool_call>")
        assertTrue(env.reply.contains("Format tindakan"))
        assertTrue(env.broken)
    }

    @Test
    fun plainTextIsShownWithoutActions() {
        val env = AiActionProtocol.parse("Halo, aku Gemini.")
        assertEquals("Halo, aku Gemini.", env.reply)
        assertTrue(env.actions.isEmpty())
    }

    @Test
    fun shellIsParsedButPolicyRefusesIt() {
        val raw = """{"reply":"x","actions":[{"type":"shell","command":"rm -rf /"},{"type":"toggle","setting":"WIFI","on":false}]}"""
        val env = AiActionProtocol.parse(raw)
        assertEquals(AiAction.Shell("rm -rf /", "perintah shell"), env.actions.first())
        assertNotNull(ShellPolicy.refusal("rm -rf /"))
    }

    @Test
    fun longTaskIsCappedPerRound() {
        val many = List(AiActionProtocol.MAX_ACTIONS_PER_ROUND + 8) { AiAction.Back }
        assertEquals(AiActionProtocol.MAX_ACTIONS_PER_ROUND, AiActionProtocol.limit(many).size)
    }

    @Test
    fun scrollTimesAndWaitStepsAreParsedInOrder() {
        val raw = """{"reply":"ok","actions":[
            {"type":"open_app","query":"tiktok"},
            {"type":"wait","ms":2000},
            {"type":"scroll","direction":"DOWN","times":2}
        ]}"""
        assertEquals(
            listOf(AiAction.OpenApp("tiktok"), AiAction.Wait(2000L), AiAction.Scroll(down = true, times = 2)),
            AiActionProtocol.parse(raw).actions
        )
    }

    @Test fun doneFlagIsParsed() {
        assertTrue(AiActionProtocol.parse("""{"reply":"ok","actions":[],"done":true}""").done)
    }

    @Test fun brokenJsonIsMarked() {
        assertTrue(AiActionProtocol.parse("bukan json").broken)
    }

    @Test fun typingIntoPasswordIsRefused() {
        assertFalse(AiCommandRouter.typeAllowed(focusedIsPassword = true))
    }
}
