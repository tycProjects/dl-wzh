package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsFormatterTest {

    private fun article(
        title: String,
        source: String,
        url: String = "https://media.example/a",
        iso: String = "2026-11-20T08:13:00Z"
    ) = NoozraNews.Article(title, url, source, iso, java.time.Instant.parse(iso).toEpochMilli(), "Deskripsi.")

    @Test
    fun convertsGmtToWib() {
        // 08:13 UTC = 15:13 WIB
        assertEquals("20 Nov 2026, 15:13 WIB", NewsFormatter.wibLabel(article("a", "Media").publishedEpoch))
    }

    @Test
    fun rendersExactRequestedFormat() {
        val list = NewsFormatter.formatUserList(
            listOf(
                NewsFormatter.from(article("Jokowi Soal Ditinggal Pergi Giring", "AFU.id"))
            )
        )
        assertTrue(list.startsWith("1. Jokowi Soal Ditinggal Pergi Giring — AFU.id (20 Nov 2026, 15:13 WIB) | [Baca Sumber](https://media.example/a)"))
    }

    @Test
    fun stripsRawHtmlAndEntities() {
        val list = NewsFormatter.formatUserList(
            listOf(NewsFormatter.from(article("<b>Judul</b> &amp; lanjutan", "Media")))
        )
        assertTrue(list.contains("1. Judul & lanjutan"))
        assertFalse(list.contains("<b>"))
        assertFalse(list.contains("&amp;"))
    }

    @Test
    fun removesDuplicateTitles() {
        val list = NewsFormatter.formatUserList(
            listOf(
                NewsFormatter.from(article("Pemerintah resmikan proyek tol baru", "Detikcom", "https://a.example/1", "2026-11-20T02:00:00Z")),
                NewsFormatter.from(article("Pemerintah Resmikan Proyek Tol Baru", "CNN Indonesia", "https://b.example/2", "2026-11-20T03:00:00Z")),
                NewsFormatter.from(article("Berita lain benar-benar berbeda", "ANTARA", "https://c.example/3", "2026-11-20T04:00:00Z"))
            )
        )
        val lines = list.lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("Berita lain benar-benar berbeda"))
        // The duplicate pair collapses into one item; the newest variant of the headline wins.
        assertTrue(lines[1].lowercase().contains("pemerintah resmikan proyek tol baru"))
        assertTrue(lines[1].contains("CNN Indonesia"))
    }

    @Test
    fun stripsTrailingPublisherSuffixFromTitle() {
        val list = NewsFormatter.formatUserList(
            listOf(NewsFormatter.from(article("Kabinet diganti bulan depan - Detikcom", "Detikcom")))
        )
        assertFalse(list.contains("Detikcom - Detikcom"))
        assertTrue(list.contains("1. Kabinet diganti bulan depan — Detikcom"))
    }

    @Test
    fun sortsNewestFirstAndCapsCount() {
        val items = (0 until 15).map { i ->
            NewsFormatter.from(article("Judul nomor $i yang cukup panjang untuk unik", "Media$i", "https://m.example/$i",
                "2026-11-19T0${i % 10}:00:00Z"))
        }
        val list = NewsFormatter.formatUserList(items, maxItems = 5)
        assertEquals(5, list.lines().size)
        assertTrue(list.contains("Judul nomor 9"))
        assertFalse(list.contains("Judul nomor 4 yang"))
    }

    @Test
    fun hidesLongUrlsBehindShortLinkLabel() {
        val longUrl = "https://media.example/news/2026/11/20/" + "segmen-sangat-panjang".repeat(12)
        val list = NewsFormatter.formatUserList(listOf(NewsFormatter.from(article("Judul berita", "Media", longUrl))))
        // The URL only appears inside the link annotation; the visible label stays short.
        assertTrue(list.contains("| [Baca Sumber]($longUrl)"))
        assertTrue(list.trimEnd().endsWith("]($longUrl)"))
    }

    @Test
    fun groundedRenderIncludesHeaderAndNote() {
        val bundle = WebSearch.NewsBundle(
            "berita hari ini",
            listOf(article("Judul berita hari ini", "ANTARA")),
            "summary-model"
        )
        val rendered = NewsFormatter.renderGrounded("berita hari ini", bundle)
        assertTrue(rendered.contains("Berita terverifikasi dari feed penerbit"))
        assertTrue(rendered.contains("1. Judul berita hari ini"))
        assertTrue(rendered.contains("WIB (GMT+7)"))
    }

    @Test
    fun groundedRenderFallsBackToSummaryWhenEmpty() {
        val bundle = WebSearch.NewsBundle("q", emptyList(), "Tidak ada berita yang dapat diverifikasi.")
        assertEquals("Tidak ada berita yang dapat diverifikasi.", NewsFormatter.renderGrounded("q", bundle))
    }

    @Test
    fun stripLinksForSpeechRemovesUrls() {
        val text = "1. Judul — Media (20 Nov 2026, 15:13 WIB) | [Baca Sumber](https://media.example/a)"
        assertEquals("1. Judul — Media (20 Nov 2026, 15:13 WIB)", NewsFormatter.stripLinksForSpeech(text))
    }
}
