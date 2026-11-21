package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitVerifierTest {
    private val suggestions = listOf("Cari", "Waktu", "preset am", "Tutup", "motion graphics termux", "preset", "250wpm", "preset unexpected", "Lihat lainnya")

    @Test fun sameSuggestionListMeansEnterDidNothing() {
        // The exported TikTok chat: after "Enter ditekan" the suggestion list was still there.
        assertEquals(SubmitVerifier.Verdict.UNCHANGED, SubmitVerifier.verdict(suggestions, suggestions + "12:01"))
    }

    @Test fun resultsReplacingSuggestionsMeansSubmitted() {
        val results = listOf("Cari", "preset", "Video", "Pengguna", "Suara", "Preset AM pack", "12K suka", "Tutup")
        assertEquals(SubmitVerifier.Verdict.SUBMITTED, SubmitVerifier.verdict(suggestions, results))
    }

    @Test fun clockAndBatteryNoiseIsIgnored() {
        assertEquals(SubmitVerifier.Verdict.UNCHANGED, SubmitVerifier.verdict(suggestions + "12:01" + "80%", suggestions + "12:02" + "79%"))
    }

    @Test fun emptyScreensProveNothing() {
        assertEquals(SubmitVerifier.Verdict.UNCHANGED, SubmitVerifier.verdict(emptyList(), emptyList()))
    }
}
