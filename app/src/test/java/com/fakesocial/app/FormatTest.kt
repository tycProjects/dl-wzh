package com.fakesocial.app

import com.fakesocial.app.data.Format
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun countBelowOneThousandIsPlain() {
        assertEquals("0", Format.count(0))
        assertEquals("999", Format.count(999))
    }

    @Test
    fun countUsesKAndM() {
        assertEquals("1K", Format.count(1000))
        assertEquals("1.2K", Format.count(1240))
        assertEquals("15.3K", Format.count(15300))
        assertEquals("1M", Format.count(1_000_000))
        assertEquals("1.5M", Format.count(1_500_000))
    }

    @Test
    fun countNearMillionRollsOverToM() {
        assertEquals("999.9K", Format.count(999_949))
        assertEquals("1M", Format.count(999_950))
    }

    @Test
    fun parseCountKeepsDigitsOnly() {
        assertEquals(0, Format.parseCount(""))
        assertEquals(0, Format.parseCount("abc"))
        assertEquals(12, Format.parseCount("1a2"))
        assertEquals(5, Format.parseCount("05"))
    }

    @Test
    fun parseCountIsCappedToNineDigits() {
        assertEquals(999_999_999, Format.parseCount("99999999999999"))
    }

    @Test
    fun sampleSizeIsPowerOfTwoAndKeepsImageAtLeastMaxSide() {
        assertEquals(1, Format.sampleSize(400, 300, 512))
        assertEquals(1, Format.sampleSize(1000, 600, 512))
        assertEquals(2, Format.sampleSize(1024, 768, 512))
        assertEquals(4, Format.sampleSize(4000, 3000, 512))
        assertEquals(1, Format.sampleSize(0, 0, 512))
    }

    @Test
    fun initialHandlesBlankAndLowercase() {
        assertEquals("A", Format.initial("alex"))
        assertEquals("?", Format.initial("   "))
    }
}
