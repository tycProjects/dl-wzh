package com.qrstudio.app

import com.qrstudio.app.qr.FaviconFetcher
import com.qrstudio.app.qr.QrContentBuilders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrContentBuildersTest {

    @Test
    fun wifiEscapesSpecialCharacters() {
        val v = QrContentBuilders.wifi("My;Net", "p:ass", "WPA")
        assertTrue(v.startsWith("WIFI:"))
        assertTrue(v.contains("S:My\\;Net"))
        assertTrue(v.contains("T:WPA"))
        assertTrue(v.contains("P:p\\:ass"))
        assertTrue(v.endsWith(";;"))
    }

    @Test
    fun openWifiUsesNopassType() {
        val v = QrContentBuilders.wifi("Cafe", "", "Open")
        assertTrue(v.contains("T:nopass"))
    }

    @Test
    fun socialBuildsProfileUrl() {
        assertEquals("https://instagram.com/ahmed", QrContentBuilders.social("instagram", "@ahmed"))
        assertEquals("https://t.me/ahmed", QrContentBuilders.social("telegram", "ahmed"))
        assertEquals("https://x.com/sara", QrContentBuilders.social("x", "sara"))
    }

    @Test
    fun whatsappStripsNonDigitsAndEncodesMessage() {
        val v = QrContentBuilders.whatsapp("+966 55 123 4567", "مرحبا بك")
        assertTrue(v.startsWith("https://wa.me/966551234567"))
        assertTrue(v.contains("text="))
    }

    @Test
    fun whatsappWithoutMessageHasNoQuery() {
        val v = QrContentBuilders.whatsapp("9665", "")
        assertEquals("https://wa.me/9665", v)
    }

    @Test
    fun vcardContainsRequiredFields() {
        val v = QrContentBuilders.contact("أحمد", "+9665")
        assertTrue(v.startsWith("BEGIN:VCARD"))
        assertTrue(v.contains("VERSION:3.0"))
        assertTrue(v.contains("FN:أحمد"))
        assertTrue(v.contains("TEL:+9665"))
        assertTrue(v.endsWith("END:VCARD"))
    }

    @Test
    fun eventFormatsDateTimeToIcs() {
        val v = QrContentBuilders.event("اجتماع", "الرياض", "2026-12-31 18:00", "")
        assertTrue(v.startsWith("BEGIN:VEVENT"))
        assertTrue(v.contains("SUMMARY:اجتماع"))
        assertTrue(v.contains("LOCATION:الرياض"))
        assertTrue(v.contains("DTSTART:20261231T180000"))
        assertTrue(v.endsWith("END:VEVENT"))
    }

    @Test
    fun geoUsesGeoScheme() {
        val v = QrContentBuilders.geo("24.7", "46.6", "برج المملكة")
        assertTrue(v.startsWith("geo:24.7,46.6"))
        assertTrue(v.contains("q=24.7,46.6"))
    }

    @Test
    fun emailAndSmsAndTel() {
        assertEquals("tel:+9665", QrContentBuilders.phone("+9665"))
        assertEquals("sms:9665?body=hello", QrContentBuilders.sms("9665", "hello"))
        assertEquals("mailto:a@b.com", QrContentBuilders.email("a@b.com", "", ""))
        val full = QrContentBuilders.email("a@b.com", "موضوع", "نص")
        assertTrue(full.startsWith("mailto:a@b.com?"))
        assertTrue(full.contains("subject="))
        assertTrue(full.contains("body="))
    }

    @Test
    fun buildDispatchesByTypeName() {
        assertEquals("tel:123", QrContentBuilders.build("PHONE", mapOf("number" to "123")))
        assertEquals("نص حر", QrContentBuilders.build("TEXT", mapOf("text" to "نص حر")))
        assertTrue(QrContentBuilders.build("WIFI", mapOf("ssid" to "S", "password" to "P", "encryption" to "WPA")).startsWith("WIFI:"))
        assertEquals("", QrContentBuilders.build("UNKNOWN", emptyMap()))
        assertEquals("", QrContentBuilders.build("URL", emptyMap()))
    }

    @Test
    fun extractDomainHandlesVariousInputs() {
        assertEquals("google.com", FaviconFetcher.extractDomain("https://www.google.com/search?q=1"))
        assertEquals("example.com", FaviconFetcher.extractDomain("example.com/page"))
        assertEquals("example.com", FaviconFetcher.extractDomain("http://example.com"))
        assertNull(FaviconFetcher.extractDomain(""))
        assertNull(FaviconFetcher.extractDomain("   "))
    }
}
