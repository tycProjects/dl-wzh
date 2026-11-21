package com.android.island.ai

import org.junit.Assert.*
import org.junit.Test

class AgentIntentClassifierTest {
    @Test fun searchNeedsAnAppTargetOrAChain() {
        assertTrue(AgentIntentClassifier.isDeviceCommand("Cari di google mediator markaz quran"))
        assertTrue(AgentIntentClassifier.isDeviceCommand("carikan kucing lucu di youtube"))
        assertTrue(AgentIntentClassifier.isDeviceCommand("cari preset lalu enter lalu klik video pertama"))
        assertFalse(AgentIntentClassifier.isDeviceCommand("cari resep nasi goreng"))
    }

    @Test fun politeWordsAreSkipped() {
        assertTrue(AgentIntentClassifier.isDeviceCommand("tolong buka youtube dong"))
        assertTrue(AgentIntentClassifier.isDeviceCommand("aku mau buka whatsapp"))
        assertTrue(AgentIntentClassifier.isDeviceCommand("matikan wifi"))
    }

    @Test fun writingAndCodeRequestsAreNeverDeviceCommands() {
        assertFalse(AgentIntentClassifier.isDeviceCommand("buka file script python itu"))
        assertFalse(AgentIntentClassifier.isDeviceCommand("tuliskan email untuk bos"))
    }

    @Test fun singleStepSearchHasNoFollowUp() {
        assertTrue(AgentIntentClassifier.isSingleStepSearch("cari di google mediator markaz quran"))
        assertFalse(AgentIntentClassifier.isSingleStepSearch("cari kucing di youtube lalu putar video pertama"))
        assertFalse(AgentIntentClassifier.isSingleStepSearch("buka youtube"))
    }
}
