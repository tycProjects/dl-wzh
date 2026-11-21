package com.android.island.ai

import org.junit.Assert.*
import org.junit.Test

class ApiPayloadGateTest {
    @Test fun blocksEmptyJsonObjectAndArray() {
        assertTrue(ApiPayloadGate.assess("{}", "application/json").blocked)
        assertTrue(ApiPayloadGate.assess("[]", "application/json").blocked)
    }

    @Test fun blocksStatusOnlyResponseWithoutActualData() {
        assertTrue(ApiPayloadGate.assess("""{"success":true,"message":"ok"}""", "application/json").blocked)
    }

    @Test fun blocksExplicitFailureFlagsAndStatuses() {
        assertTrue(ApiPayloadGate.assess("""{"success":false,"message":"failed"}""", "application/json").blocked)
        assertTrue(ApiPayloadGate.assess("""{"status":"error","data":{"id":1}}""", "application/json").blocked)
        assertTrue(ApiPayloadGate.assess("""{"error":"invalid key"}""", "application/json").blocked)
    }

    @Test fun blocksEmptyDataContainers() {
        listOf(
            """{"data":null}""",
            """{"results":[]}""",
            """{"items":{}}""",
            """{"payload":""}"""
        ).forEach { assertTrue(it, ApiPayloadGate.assess(it, "application/json").blocked) }
    }

    @Test fun blocksMostlyNullPayload() {
        assertTrue(
            ApiPayloadGate.assess(
                """{"a":null,"b":null,"c":null,"d":4}""",
                "application/json"
            ).blocked
        )
    }

    @Test fun blocksKnownProviderContractFailures() {
        val result = ApiPayloadGate.assess(
            """{"latitude":1,"longitude":2}""",
            "application/json",
            listOf("Profil Open-Meteo: field timezone tidak ditemukan."),
            "Open-Meteo"
        )
        assertTrue(result.blocked)
        assertTrue(result.asText().contains("DIBLOKIR"))
    }

    @Test fun blocksImpossibleDomainValues() {
        val result = ApiPayloadGate.assess(
            """{"rates":{"USD":-1}}""",
            "application/json",
            listOf("Nilai kurs untuk 'USD' bukan angka positif yang valid.")
        )
        assertTrue(result.blocked)
    }

    @Test fun allowsNonEmptyOrdinaryJsonWithoutKnownFailure() {
        val result = ApiPayloadGate.assess(
            """{"temperature":27,"unit":"C","observed_at":"2026-11-18T10:00:00Z"}""",
            "application/json"
        )
        assertFalse(result.blocked)
    }

    @Test fun blocksUnstructuredPlainText() {
        assertTrue(ApiPayloadGate.assess("The weather is sunny", "text/plain").blocked)
    }
}
