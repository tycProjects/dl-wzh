package com.android.island.ai

import org.junit.Assert.*
import org.junit.Test

class SearchControlMatcherTest {
    @Test fun recognisesCommonSearchEntries() {
        assertTrue(SearchControlMatcher.isSearchControl("Cari"))
        assertTrue(SearchControlMatcher.isSearchControl("", description = "Search Button"))
        assertTrue(SearchControlMatcher.isSearchControl("Cari kontak"))
        assertTrue(SearchControlMatcher.isSearchControl("", hint = "Search or type URL"))
        assertTrue(SearchControlMatcher.isSearchControl("", viewId = "com.whatsapp:id/menuitem_search"))
        assertTrue(SearchControlMatcher.isSearchControl("id:search_bar"))
        assertTrue(SearchControlMatcher.isSearchControl("Telusuri atau ketik URL"))
    }

    @Test fun rejectsSuggestionsAndSideControls() {
        assertFalse(SearchControlMatcher.isSearchControl("Pencarian · preset am"))
        assertFalse(SearchControlMatcher.isSearchControl("Telusuri dengan suara"))
        assertFalse(SearchControlMatcher.isSearchControl("Riwayat pencarian"))
        assertFalse(SearchControlMatcher.isSearchControl("Search by image"))
        assertFalse(SearchControlMatcher.isSearchControl("", viewId = "app:id/search_results_list"))
        assertFalse(SearchControlMatcher.isSearchControl("Ummi Faris"))
        assertFalse(SearchControlMatcher.isSearchControl(""))
    }

    @Test fun recognisesHomeTabs() {
        assertTrue(SearchControlMatcher.isHomeTab("Beranda"))
        assertTrue(SearchControlMatcher.isHomeTab("Chat"))
        assertTrue(SearchControlMatcher.isHomeTab("Chat, tab 1 dari 4"))
        assertFalse(SearchControlMatcher.isHomeTab("Profil"))
    }
}
