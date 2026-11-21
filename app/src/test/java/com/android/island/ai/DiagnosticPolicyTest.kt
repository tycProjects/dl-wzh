package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticPolicyTest {
    @Test
    fun meminfoNeedsAValidPackage() {
        assertEquals(
            "dumpsys meminfo com.whatsapp",
            DiagnosticPolicy.command(DiagnosticPolicy.Kind.MEMINFO, "com.whatsapp")
        )
        assertNull(DiagnosticPolicy.command(DiagnosticPolicy.Kind.MEMINFO, "com.x; rm -rf /"))
        assertNull(DiagnosticPolicy.command(DiagnosticPolicy.Kind.MEMINFO, null))
    }

    @Test
    fun forceStopNeedsTheUsersWords() {
        assertTrue(DiagnosticPolicy.forceStopAllowed("tolong paksa berhenti whatsapp"))
        assertFalse(DiagnosticPolicy.forceStopAllowed("buka whatsapp"))
    }

    @Test
    fun forceStopRejectsBadPackageNames() {
        assertEquals("am force-stop com.example.app", DiagnosticPolicy.forceStopCommand("com.example.app"))
        assertNull(DiagnosticPolicy.forceStopCommand("bad name; reboot"))
    }

    @Test
    fun meminfoSummaryKeepsOnlyTotalsAndHeapLines() {
        val raw = "Applications Memory Usage (in Kilobytes):\n  Native Heap  1200\n  Dalvik Heap  800\n  TOTAL PSS: 5000\nDetails: done"
        val summary = DiagnosticPolicy.summarizeMeminfo(raw)
        assertTrue(summary.contains("Native Heap"))
        assertTrue(summary.contains("TOTAL PSS"))
        assertFalse(summary.contains("Applications Memory"))
    }

    @Test
    fun meminfoSummaryFallsBackToRawTextWhenNothingMatches() {
        assertEquals("No process found", DiagnosticPolicy.summarizeMeminfo("No process found\n"))
    }
}
