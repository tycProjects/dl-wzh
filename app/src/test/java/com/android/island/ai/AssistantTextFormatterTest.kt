package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantTextFormatterTest {
    @Test fun separatesHeadingsAndListsWithoutExcessiveBlankLines() {
        val input = "Ringkasan\nIni penjelasan.\nLangkah berikutnya:\n1. Buka aplikasi\n2. Cari preset\nKesimpulan\nSelesai."
        val expected = "Ringkasan\n\nIni penjelasan.\n\nLangkah berikutnya:\n\n1. Buka aplikasi\n2. Cari preset\n\nKesimpulan\n\nSelesai."
        assertEquals(expected, AssistantTextFormatter.format(input))
    }

    @Test fun preservesCodeBlockContentsAndNormalizesLineEndings() {
        val input = "Contoh:\r\n```kotlin\r\nval x=1\r\nprintln(x)\r\n```\r\nSelesai."
        val expected = "Contoh:\n\n```kotlin\nval x=1\nprintln(x)\n```\n\nSelesai."
        assertEquals(expected, AssistantTextFormatter.format(input))
    }

    @Test fun collapsesExcessiveBlankLines() {
        assertEquals("A\n\nB", AssistantTextFormatter.format("A\n\n\n\nB"))
    }
}
