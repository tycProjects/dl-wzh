package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiFailoverPolicyTest {
    @Test fun quota429MovesToNextConfiguredRoute() {
        assertTrue(ApiFailoverPolicy.isQuotaExhausted(429, "Too Many Requests"))
        assertTrue(ApiFailoverPolicy.shouldTryNextRoute(429, "Too Many Requests"))
    }

    @Test fun recognizesProviderSpecificQuotaMessages() {
        assertTrue(ApiFailoverPolicy.isQuotaExhausted(400, "insufficient_quota"))
        assertTrue(ApiFailoverPolicy.isQuotaExhausted(403, "RESOURCE_EXHAUSTED"))
        assertTrue(ApiFailoverPolicy.isQuotaExhausted(402, "Payment required"))
    }

    @Test fun ordinaryBadRequestDoesNotTriggerFailover() {
        assertFalse(ApiFailoverPolicy.isQuotaExhausted(400, "Invalid JSON payload"))
        assertFalse(ApiFailoverPolicy.shouldTryNextRoute(400, "Invalid JSON payload"))
    }

    @Test fun credentialsAndTransientFailuresCanMoveToAnotherRoute() {
        assertTrue(ApiFailoverPolicy.shouldTryNextRoute(401, "invalid api key"))
        assertTrue(ApiFailoverPolicy.shouldTryNextRoute(503, "service unavailable"))
    }
}
