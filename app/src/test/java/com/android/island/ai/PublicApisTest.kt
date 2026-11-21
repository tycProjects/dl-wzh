package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PublicApisTest {
    private val fake = sequenceOf(
        "Weather|Open-Meteo|https://open-meteo.com/|Global weather forecast API",
        "Currency Exchange|Frankfurter|https://www.frankfurter.app/docs|Exchange rates from the ECB",
        "Open Data|REST Countries|https://restcountries.com|Get information about countries",
        "Dev|Broken line without enough fields",
        "Dev|Insecure|http://insecure.example.com/|should be skipped"
    )

    @Test fun parseKeepsOnlyWellFormedHttpsLines() {
        assertNotNull(PublicApis.parse(" A | B | https://x.com/docs | description "))
        assertNull(PublicApis.parse("A|B|http://x.com|d"))
        assertNull(PublicApis.parse("A|B|https://x.com"))
        assertNull(PublicApis.parse("A|B|https://user:pass@x.com|d"))
        assertNull(PublicApis.parse("A|B|https://x.com:8443|d"))
        assertNull(PublicApis.parse("A|B|https://localhost/docs|d"))
        assertNull(PublicApis.parse("A||https://x.com|d"))
        assertNull(PublicApis.parse("A|B|https://x.com|"))
    }

    @Test fun searchDeduplicatesSameUrlAndBoundsResultCount() {
        val duplicate = sequenceOf(
            "Weather|Open-Meteo|https://open-meteo.com/|Global weather forecast API",
            "Weather|Open Meteo duplicate|https://open-meteo.com|Another description",
            "Weather|Weather Example|https://weather.example.org/api|Weather data"
        )
        assertEquals(2, PublicApis.search(duplicate, "weather", limit = 100).size)
        assertTrue(PublicApis.search(duplicate, "weather", limit = 0).isEmpty())
    }

    @Test fun findDoesNotPromiseKeylessOrLiveEndpoints() {
        val result = PublicApis.find({ fake.joinToString("\n").byteInputStream() }, "weather")
        assertTrue(result.contains("Metadata autentikasi, harga, rate limit, CORS, dan uptime tidak tersedia"))
        assertTrue(result.contains("bukan selalu endpoint"))
        assertTrue(result.contains("hanya mendukung GET HTTPS"))
    }

    @Test fun searchRanksNameMatchesFirst() {
        assertEquals("Open-Meteo", PublicApis.search(fake, "weather forecast").first().name)
        assertEquals("Frankfurter", PublicApis.search(fake, "currency exchange rates").first().name)
        assertEquals("REST Countries", PublicApis.search(fake, "information about countries").first().name)
    }

    @Test fun searchFoldsPluralsAndIgnoresGenericWords() {
        assertEquals("REST Countries", PublicApis.search(fake, "country").first().name)
        assertEquals("Frankfurter", PublicApis.search(fake, "currencies").first().name)
        assertTrue(PublicApis.search(fake, "free api data information").isEmpty())
    }

    @Test fun searchWithNoUsefulWordFindsNothing() {
        assertTrue(PublicApis.search(fake, "").isEmpty())
        assertTrue(PublicApis.search(fake, "a in").isEmpty())
        assertTrue(PublicApis.search(fake, "zzzzzz").isEmpty())
    }

    @Test fun listedHostAndItsSubdomainsAreAllowed() {
        assertNull(PublicApis.refusal(fake, "https://api.open-meteo.com/v1/forecast?latitude=1&longitude=2"))
        assertNull(PublicApis.refusal(fake, "https://api.frankfurter.app/latest"))
        assertNull(PublicApis.refusal(fake, "https://restcountries.com/v3.1/name/japan"))
    }

    @Test fun credentialQueryValuesAreRedactedBeforeLogging() {
        val safe = PublicApis.redactUrl("https://api.example.com/v1/data?city=Cairo&api_key=secret123&access_token=abc")
        assertTrue(safe.contains("city=Cairo"))
        assertTrue(safe.contains("api_key=[REDACTED]"))
        assertTrue(safe.contains("access_token=[REDACTED]"))
        assertTrue(PublicApis.redactUrl("https://api.example.com/x?x-api-key=secret").contains("x-api-key=[REDACTED]"))
        assertTrue(!safe.contains("secret123"))
        assertTrue(!safe.contains("abc"))
    }

    @Test fun httpFailuresAreReportedWithoutPretendingSuccess() {
        assertTrue(PublicApis.httpFailure(401).contains("meminta autentikasi"))
        assertTrue(PublicApis.httpFailure(403).contains("akses ditolak"))
        assertTrue(PublicApis.httpFailure(404).contains("endpoint tidak ditemukan"))
        assertTrue(PublicApis.httpFailure(429, "30").contains("30"))
        assertTrue(PublicApis.httpFailure(503).contains("server penyedia"))
    }

    @Test fun unlistedOrUnsafeTargetsAreRefused() {
        listOf(
            "https://evil.example.com/steal?d=secret", "https://open-meteo.com.evil.com/x", "http://open-meteo.com/",
            "https://127.0.0.1/", "https://192.168.1.1/admin", "https://localhost/", "https://[::1]/",
            "https://user:pw@open-meteo.com/", "https://open-meteo.com:8443/", "ftp://open-meteo.com/", "not a url",
            "https://notopen-meteo.com/"
        ).forEach { assertNotNull(it, PublicApis.refusal(fake, it)) }
    }

    @Test fun realCatalogFindsWeatherAndAllowsItsApiHost() {
        val file = File("src/main/assets/public_apis.txt")
        if (!file.exists()) return
        file.bufferedReader().useLines { lines ->
            val all = lines.toList()
            assertTrue(all.size > 900)
            assertTrue(PublicApis.search(all.asSequence(), "weather", 40).any { it.name.contains("Meteo", true) })
            assertTrue(PublicApis.search(all.asSequence(), "country information").any { it.name.contains("Countries", true) })
            assertNull(PublicApis.refusal(all.asSequence(), "https://api.open-meteo.com/v1/forecast"))
            assertNotNull(PublicApis.refusal(all.asSequence(), "https://evil.example.com/"))
        }
    }
    @Test fun validJsonIsMarkedAsSyntaxOnlyNotFactChecked() {
        val result = PublicApis.assessBody("""{"temperature":27,"unit":"C"}""", "application/json")
        assertTrue(result.contains("JSON RFC 8259 valid"))
        assertTrue(result.contains("bukan verifikasi kebenaran atau kebaruan data"))
    }

    @Test fun invalidJsonIsRejectedInsteadOfForwardedAsValidData() {
        val result = PublicApis.assessBody("""{"temperature":27,,}""", "application/json")
        assertTrue(result.startsWith("GAGAL VALIDASI"))
    }

    @Test fun xmlMustBeWellFormed() {
        assertTrue(PublicApis.assessBody("<root><value>1</value></root>", "application/xml").contains("well-formed"))
        assertTrue(PublicApis.assessBody("<root><value></root>", "application/xml").startsWith("GAGAL VALIDASI"))
    }

    @Test fun xmlDocumentTypeDeclarationsAreRejected() {
        val payload = "<!DOCTYPE root [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><root>&x;</root>"
        assertTrue(PublicApis.assessBody(payload, "application/xml").startsWith("GAGAL VALIDASI"))
    }

    @Test fun jsonTrailingGarbageIsRejected() {
        assertTrue(PublicApis.assessBody("{\"ok\":true} trailing", "application/json").startsWith("GAGAL VALIDASI"))
    }

    @Test fun lenientJsonExtensionsAreRejected() {
        listOf(
            "{'ok':true}",
            "{\"ok\":true,}",
            "{\"ok\":true;\"count\":1}",
            "{\"n\":01}",
            "// comment\n{\"ok\":true}"
        ).forEach { payload ->
            assertTrue(payload, PublicApis.assessBody(payload, "application/json").startsWith("GAGAL VALIDASI"))
        }
    }

    @Test fun plainTextIsExplicitlyUnverified() {
        val result = PublicApis.assessBody("Temperature is 27 C", "text/plain")
        assertTrue(result.contains("teks biasa"))
        assertTrue(result.contains("belum diverifikasi"))
    }

    @Test fun htmlErrorPageIsNotTreatedAsPlainTextData() {
        assertTrue(PublicApis.assessBody("<!doctype html><html><body>Not found</body></html>", "").startsWith("GAGAL VALIDASI"))
        assertTrue(PublicApis.assessBody("<html><body>Proxy error</body></html>", "text/plain").startsWith("GAGAL VALIDASI"))
    }

    @Test fun retrievalMetadataSeparatesFetchTimeFromProviderUpdateTime() {
        val result = PublicApis.retrievalMetadata(
            java.time.Instant.parse("2026-11-18T14:00:00Z"),
            "https://api.example.com/data?api_key=secret",
            200,
            "application/json",
            "{}"
        )
        assertTrue(result.contains("2026-11-18T14:00:00Z"))
        assertTrue(result.contains("api_key=[REDACTED]"))
        assertTrue(result.contains("bukan waktu pembaruan data"))
        assertTrue(!result.contains("secret"))
    }

}
