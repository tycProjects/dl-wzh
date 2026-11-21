package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ApiFreshnessTest {
    private val now = Instant.parse("2026-11-18T12:00:00Z")

    @Test fun recognizesProviderUpdatedAtAndReportsItsAge() {
        val report = ApiFreshness.report(now, """{"updated_at":"2026-11-18T10:00:00Z","value":4}""", ApiFreshness.Headers())
        assertTrue(report.contains("WAKTU DARI PAYLOAD (kandidat 'updated_at'"))
        assertTrue(report.contains("UMUR TIMESTAMP PAYLOAD: 2 jam"))
        assertFalse(report.contains("STATUS KEBARUAN: terbaru"))
    }

    @Test fun reportsUnknownWhenPayloadHasNoRecognizableTime() {
        val report = ApiFreshness.report(now, """{"value":4,"label":"sample"}""", ApiFreshness.Headers())
        assertTrue(report.contains("kebaruan data TIDAK DIKETAHUI"))
    }

    @Test fun readsNumericEpochSecondsAndMilliseconds() {
        assertEquals(Instant.parse("2026-11-18T11:00:00Z"), ApiFreshness.parseInstant("1794999600"))
        assertEquals(Instant.parse("2026-11-18T11:00:00Z"), ApiFreshness.parseInstant("1794999600000"))
    }

    @Test fun reportsHttpCacheMetadataSeparatelyFromProviderTime() {
        val headers = ApiFreshness.Headers(
            date = "Wed, 18 Nov 2026 12:00:00 GMT",
            age = "120",
            cacheControl = "public, max-age=300",
            expires = "Wed, 18 Nov 2026 12:05:00 GMT",
            lastModified = "Wed, 18 Nov 2026 10:00:00 GMT",
            etag = "W/\"abc123\""
        )
        val report = ApiFreshness.report(now, """{"value":4}""", headers)
        assertTrue(report.contains("HTTP DATE (waktu respons server, bukan waktu pembaruan data)"))
        assertTrue(report.contains("HTTP AGE: 2 menit"))
        assertTrue(report.contains("CACHE-CONTROL: public, max-age=300"))
        assertTrue(report.contains("LAST-MODIFIED (metadata HTTP, bukan jaminan data terbaru)"))
        assertTrue(report.contains("ETAG: W/\"abc123\""))
        assertTrue(report.contains("WAKTU DATA DARI PENYEDIA: tidak ditemukan"))
    }

    @Test fun ignoresFutureAndMalformedPayloadTimestamps() {
        val report = ApiFreshness.report(now, """{"updated_at":"2099-01-01T00:00:00Z","time":"not-a-date"}""", ApiFreshness.Headers())
        assertTrue(report.contains("kebaruan data TIDAK DIKETAHUI"))
    }

    @Test fun recognizesDateAndArrayTimeCandidatesWithoutCallingThemDatasetUpdate() {
        val report = ApiFreshness.report(now, """{"hourly":{"time":["2026-11-18T11:00:00Z"]}}""", ApiFreshness.Headers())
        assertTrue(report.contains("WAKTU DARI PAYLOAD (kandidat 'time'"))
        assertTrue(report.contains("bisa berupa waktu observasi, jadwal, atau tanggal item"))
    }
}
