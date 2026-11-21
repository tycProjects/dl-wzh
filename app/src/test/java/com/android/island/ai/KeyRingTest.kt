package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyRingTest {
    @Test fun primaryKeyComesFirstThenBackupsWithoutDuplicates() {
        assertEquals(listOf("a", "b", "c"), KeyRing.keys("a", "b\n\n a \nc\na"))
    }

    @Test fun keepsAtMostFiveKeys() {
        assertEquals(listOf("a", "b", "c", "d", "e"), KeyRing.keys("a", "b\nc\nd\ne\nf\ng"))
    }

    @Test fun blankPrimaryIsSkipped() {
        assertEquals(listOf("x"), KeyRing.keys("", "x"))
    }

    @Test fun allTemporarilyExhaustedKeysAreNotRetriedImmediately() {
        assertEquals(emptyList<Int>(), KeyRing.rotationOrder(2, 0, setOf(0, 1)))
    }
}
