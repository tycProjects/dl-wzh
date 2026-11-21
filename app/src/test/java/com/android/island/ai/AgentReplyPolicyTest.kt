package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentReplyPolicyTest {
    @Test fun genericCompletionDoesNotReplaceSubstantiveExplanation() {
        val detailed = ("Laporan penggunaan memori menunjukkan beberapa kategori alokasi. ").repeat(8)
        val generic = "Saya telah menjelaskan laporan penggunaan memori. Apakah ada hal lain yang ingin Anda ketahui?"
        assertEquals(detailed, AgentReplyPolicy.keepSubstantiveAnswer(detailed, generic))
    }

    @Test fun finalExplanationCanReplaceShortProgressMessage() {
        val progress = "Saya sedang memeriksa laporan memori."
        val final = "Berikut penjelasan lengkap dari hasil pemeriksaan memori yang tersedia."
        assertEquals(final, AgentReplyPolicy.keepSubstantiveAnswer(progress, final))
    }

    @Test fun blankFollowUpDoesNotEraseExistingAnswer() {
        val answer = "Penjelasan hasil pemeriksaan tersedia."
        assertEquals(answer, AgentReplyPolicy.keepSubstantiveAnswer(answer, "  "))
    }
}
