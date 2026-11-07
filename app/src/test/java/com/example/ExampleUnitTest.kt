package com.example

import com.example.engine.HistoryCsvExporter
import com.example.engine.SpeechMathParser
import com.example.model.CalculationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun historyCsvExport_generatesCorrectHeadersAndRows() {
        val items = listOf(
            CalculationItem(
                expression = "1,500 + Tax (20%)",
                result = "1,800",
                timestamp = 1700000000000L,
                category = "VAT Quick"
            ),
            CalculationItem(
                expression = "sin(45) * 10",
                result = "7.071",
                timestamp = 1700000010000L,
                category = "Scientific"
            )
        )

        val csv = HistoryCsvExporter.generateCsv(items)

        // Verify CSV header
        assertTrue(csv.startsWith("Timestamp,Date,Time,Category,Expression,Result\n"))

        // Verify escaping for expressions with commas
        assertTrue(csv.contains("\"1,500 + Tax (20%)\""))
        assertTrue(csv.contains("\"1,800\""))
        assertTrue(csv.contains("VAT Quick"))
        assertTrue(csv.contains("Scientific"))
    }

    @Test
    fun speechMathParser_convertsSpokenMathCorrectly() {
        // Test basic addition with number words
        val res1 = SpeechMathParser.parseSpokenText("twenty five plus forty")
        assertEquals("25+40", res1.expression)

        // Test division and phrases
        val res2 = SpeechMathParser.parseSpokenText("one hundred divided by four")
        assertEquals("100÷4", res2.expression)

        // Test functions
        val res3 = SpeechMathParser.parseSpokenText("square root of 144")
        assertEquals("sqrt(144)", res3.expression)

        // Test conversational question with evaluation flag
        val res4 = SpeechMathParser.parseSpokenText("what is 5 times 8 equals")
        assertEquals("5×8", res4.expression)
        assertTrue(res4.shouldEvaluate)

        // Test clear command
        val res5 = SpeechMathParser.parseSpokenText("clear all")
        assertTrue(res5.isClearCommand)
    }
}
