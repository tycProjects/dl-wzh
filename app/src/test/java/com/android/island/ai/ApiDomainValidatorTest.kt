package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiDomainValidatorTest {
    private val catalog = sequenceOf(
        "Weather|Open-Meteo|https://open-meteo.com/|Weather forecast",
        "Currency Exchange|Frankfurter|https://www.frankfurter.app/docs|Exchange rates",
        "Geocoding|Geo API|https://geo.example.org/docs|Coordinates",
        "Finance|Market API|https://market.example.org/docs|Market prices",
        "Health|Health API|https://health.example.org/docs|Health data",
        "Books|Books API|https://books.example.org/docs|Book records"
    )

    @Test fun sectorIsResolvedFromListedProviderHost() {
        assertEquals("Weather", ApiDomainValidator.sectorForUrl(catalog, "https://api.open-meteo.com/v1/forecast"))
        assertEquals("Currency Exchange", ApiDomainValidator.sectorForUrl(catalog, "https://api.frankfurter.app/latest"))
        assertEquals(null, ApiDomainValidator.sectorForUrl(catalog, "https://unknown.example.net/data"))
    }

    @Test fun weatherWarnsWhenUnitsAndObservationTimeAreMissing() {
        val result = ApiDomainValidator.assess("Weather", """{"temperature":22.5,"wind_speed":7.2,"humidity":55}""", "application/json").asText()
        assertTrue(result.contains("metadata unit tidak terdeteksi"))
        assertTrue(result.contains("Waktu pengamatan/pembaruan tidak terdeteksi"))
    }

    @Test fun weatherFlagsImplausibleValuesButDoesNotClaimTruth() {
        val result = ApiDomainValidator.assess("Weather", """{"temperature":250,"humidity":130,"temperature_unit":"C","time":"2026-11-18T10:00:00Z"}""", "application/json").asText()
        assertTrue(result.contains("'temperature'"))
        assertTrue(result.contains("'humidity'"))
        assertTrue(result.contains("bukan verifikasi fakta"))
    }

    @Test fun currencyChecksRateShapeCodesAndBaseDate() {
        val result = ApiDomainValidator.assess("Currency Exchange", """{"base":"EUR","date":"2026-11-18","rates":{"USD":1.1,"EGP":52.0}}""", "application/json").asText()
        assertTrue(result.contains("PENTING"))
        assertTrue(!result.contains("Objek rates standar tidak terdeteksi"))
        assertTrue(!result.contains("Mata uang dasar/sumber kurs tidak terdeteksi"))
        assertTrue(!result.contains("Tanggal/waktu kurs tidak terdeteksi"))
    }

    @Test fun currencyRejectsNonPositiveOrNonNumericRate() {
        val result = ApiDomainValidator.assess("Currency Exchange", """{"rates":{"USD":0,"USDX":-2}}""", "application/json").asText()
        assertTrue(result.contains("bukan angka positif yang valid"))
        assertTrue(result.contains("Kode mata uang 'USDX'"))
        assertTrue(result.contains("Mata uang dasar/sumber kurs tidak terdeteksi"))
    }

    @Test fun geocodingChecksCoordinateRangesAndPairs() {
        val result = ApiDomainValidator.assess("Geocoding", """{"latitude":120,"longitude":200}""", "application/json").asText()
        assertTrue(result.contains("Latitude berada di luar rentang"))
        assertTrue(result.contains("Longitude berada di luar rentang"))
        val pair = ApiDomainValidator.assess("Geocoding", """{"latitude":12.5}""", "application/json").asText()
        assertTrue(pair.contains("Hanya satu dari pasangan koordinat"))
    }

    @Test fun financeRequiresUnitAndTimestampHints() {
        val result = ApiDomainValidator.assess("Finance", """{"price":125.5}""", "application/json").asText()
        assertTrue(result.contains("Mata uang atau unit nilai tidak terdeteksi"))
        assertTrue(result.contains("Waktu data pasar tidak terdeteksi"))
    }

    @Test fun healthDataAlwaysGetsClinicalInterpretationWarning() {
        val result = ApiDomainValidator.assess("Health", """{"value":7.2}""", "application/json").asText()
        assertTrue(result.contains("tidak menilai diagnosis, keamanan, atau rekomendasi medis"))
        assertTrue(result.contains("Unit pengukuran kesehatan tidak terdeteksi"))
    }

    @Test fun unknownSectorDoesNotPretendToHaveSpecialValidation() {
        val result = ApiDomainValidator.assess("Books", """{"title":"Example"}""", "application/json").asText()
        assertTrue(result.contains("Belum ada aturan khusus"))
    }

    @Test fun openMeteoProviderProfileChecksItsCommonResponseEnvelope() {
        val body = """{"latitude":30.0,"longitude":31.0,"timezone":"Africa/Cairo","hourly_units":{"temperature_2m":"°C"},"hourly":{"time":["2026-11-18T12:00"],"temperature_2m":[22.0]}}"""
        val result = ApiDomainValidator.assess("Weather", body, "application/json", "Open-Meteo").asText()
        assertTrue(result.contains("PENYEDIA TERDETEKSI: Open-Meteo"))
        assertTrue(!result.contains("Profil Open-Meteo:"))
        assertTrue(!result.contains("metadata unit tidak terdeteksi"))
    }

    @Test fun frankfurterProviderProfileRequiresBaseDateAndRates() {
        val result = ApiDomainValidator.assess("Currency Exchange", """{"amount":1.0,"base":"EUR","date":"2026-11-18","rates":{"USD":1.1}}""", "application/json", "Frankfurter").asText()
        assertTrue(!result.contains("Profil Frankfurter:"))
        assertTrue(!result.contains("Objek rates standar tidak terdeteksi"))
    }

    @Test fun restCountriesProviderProfileChecksCountryIdentityFields() {
        val body = """[{"name":{"common":"Egypt","official":"Arab Republic of Egypt"},"cca2":"EG","cca3":"EGY","population":110000000}]"""
        val result = ApiDomainValidator.assess("Open Data", body, "application/json", "REST Countries").asText()
        assertTrue(!result.contains("Profil REST Countries:"))
        assertTrue(!result.contains("Belum ada aturan khusus"))
    }

    @Test fun universalChecksFlagEmptyResultContainersAndFailedStatus() {
        val result = ApiDomainValidator.assess("Books", """{"success":false,"status":"error","data":[],"items":{}}""", "application/json").asText()
        assertTrue(result.contains("success/ok bernilai false"))
        assertTrue(result.contains("status menunjukkan error"))
        assertTrue(result.contains("Kontainer 'data' kosong"))
        assertTrue(result.contains("Kontainer 'items' kosong"))
    }

    @Test fun universalChecksFlagFutureOrUnparseableTimestamps() {
        val future = ApiDomainValidator.assess("General", """{"updated_at":4102444800,"value":1}""", "application/json").asText()
        assertTrue(future.contains("lebih dari 5 menit di masa depan"))
        val malformed = ApiDomainValidator.assess("General", """{"updated_at":"not-a-date","value":1}""", "application/json").asText()
        assertTrue(malformed.contains("tidak dapat dikenali sebagai ISO-8601 atau epoch"))
    }

    @Test fun universalChecksWarnWhenFreshnessTimestampIsAbsent() {
        val result = ApiDomainValidator.assess("General", """{"value":1,"label":"sample"}""", "application/json").asText()
        assertTrue(result.contains("Tidak ditemukan field waktu pembaruan/publikasi"))
        assertTrue(result.contains("bukan bukti bahwa isi data masih terbaru"))
    }

    @Test fun jsonIsDetectedFromBodyEvenWhenContentTypeHeaderIsMissing() {
        val result = ApiDomainValidator.assess("Weather", """{"temperature":22}""", "").asText()
        assertTrue(result.contains("metadata unit tidak terdeteksi"))
    }

    @Test fun nonJsonSectorPayloadIsNotInterpretedAsStructuredFacts() {
        val result = ApiDomainValidator.assess("Weather", "temperature 20", "text/plain").asText()
        assertTrue(result.contains("dilewati karena respons bukan JSON"))
    }
}
