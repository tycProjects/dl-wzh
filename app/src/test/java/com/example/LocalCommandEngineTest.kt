package com.example

import com.example.data.CommandAliasEntity
import com.example.device.LocalCommandEngine
import com.example.device.LocalIntentAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCommandEngineTest {

    @Test
    fun `parses bengali flashlight and volume commands locally`() {
        val flashOn = LocalCommandEngine.parse("ফ্ল্যাশলাইট চালু করো")
        assertEquals(LocalIntentAction.Flashlight(true), flashOn)

        val flashOff = LocalCommandEngine.parse("টর্চ বন্ধ করো")
        assertEquals(LocalIntentAction.Flashlight(false), flashOff)

        val vol50 = LocalCommandEngine.parse("ভলিউম ৫০ পার্সেন্ট করো")
        assertEquals(LocalIntentAction.VolumeChange(targetPercent = 50), vol50)
    }

    @Test
    fun `parses bengali app launch and custom aliases`() {
        val fb = LocalCommandEngine.parse("মায়া ফেসবুক খোলো।")
        assertTrue(fb is LocalIntentAction.LaunchApp)

        val alias = CommandAliasEntity(
            id = 1,
            triggerPhrase = "ভিডিও দেখাও",
            targetPackageName = "com.google.android.youtube",
            targetAppLabel = "YouTube",
            enabled = true
        )
        val matched = LocalCommandEngine.parse("ভিডিও দেখাও", listOf(alias))
        assertEquals(
            LocalIntentAction.LaunchApp("YouTube", "com.google.android.youtube"),
            matched
        )
    }

    @Test
    fun `parses multi step automation and flags sensitive actions`() {
        val seq = LocalCommandEngine.parse("ইউটিউব খোলো তারপর ভলিউম বাড়াও")
        assertTrue(seq is LocalIntentAction.AutomationSequence)
        assertEquals(2, (seq as LocalIntentAction.AutomationSequence).steps.size)

        val sensitive = LocalCommandEngine.parse("আমার সব ফাইল মুছে ফেলো")
        assertTrue(sensitive is LocalIntentAction.SensitiveConfirmationRequired)
    }
}
