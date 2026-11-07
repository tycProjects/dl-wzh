package com.example.engine

data class SpokenMathResult(
    val expression: String,
    val shouldEvaluate: Boolean,
    val isClearCommand: Boolean = false,
    val originalSpokenText: String
)

object SpeechMathParser {

    private val wordToDigit = mapOf(
        "zero" to "0",
        "one" to "1",
        "two" to "2",
        "three" to "3",
        "four" to "4",
        "five" to "5",
        "six" to "6",
        "seven" to "7",
        "eight" to "8",
        "nine" to "9",
        "ten" to "10",
        "eleven" to "11",
        "twelve" to "12",
        "thirteen" to "13",
        "fourteen" to "14",
        "fifteen" to "15",
        "sixteen" to "16",
        "seventeen" to "17",
        "eighteen" to "18",
        "nineteen" to "19",
        "twenty" to "20",
        "thirty" to "30",
        "forty" to "40",
        "fifty" to "50",
        "sixty" to "60",
        "seventy" to "70",
        "eighty" to "80",
        "ninety" to "90",
        "hundred" to "100",
        "thousand" to "1000",
        "million" to "1000000"
    )

    fun parseSpokenText(rawText: String): SpokenMathResult {
        val lower = rawText.trim().lowercase()

        // Check clear commands
        if (lower == "clear" || lower == "clear all" || lower == "reset") {
            return SpokenMathResult(expression = "", shouldEvaluate = false, isClearCommand = true, originalSpokenText = rawText)
        }

        var text = lower

        // Check if user asked a question or ended with "equals" / "calculate"
        val shouldEvaluate = text.endsWith("equals") ||
                text.endsWith("equal") ||
                text.endsWith("calculate") ||
                text.startsWith("what is") ||
                text.startsWith("calculate") ||
                text.startsWith("solve") ||
                text.contains("=")

        // Clean conversational prefixes & suffixes
        text = text.replace("what is", "")
            .replace("calculate", "")
            .replace("solve", "")
            .replace("how much is", "")
            .replace("equals", "")
            .replace("equal", "")
            .replace("=", "")
            .replace("please", "")

        // Replace math function phrases first (before basic words)
        text = text.replace("square root of", "sqrt(")
            .replace("square root", "sqrt(")
            .replace("sqrt of", "sqrt(")
            .replace("cube root of", "cbrt(")
            .replace("cube root", "cbrt(")
            .replace("cbrt of", "cbrt(")
            .replace("to the power of", "^")
            .replace("raised to the power of", "^")
            .replace("raised to", "^")
            .replace("power of", "^")
            .replace("squared", "^2")
            .replace("cubed", "^3")
            .replace("sine of", "sin(")
            .replace("sin of", "sin(")
            .replace("cosine of", "cos(")
            .replace("cos of", "cos(")
            .replace("tangent of", "tan(")
            .replace("tan of", "tan(")
            .replace("natural log of", "ln(")
            .replace("natural log", "ln(")
            .replace("ln of", "ln(")
            .replace("logarithm of", "log(")
            .replace("log of", "log(")
            .replace("absolute value of", "abs(")
            .replace("open bracket", "(")
            .replace("open parenthesis", "(")
            .replace("close bracket", ")")
            .replace("close parenthesis", ")")
            .replace("pi", "π")

        // Replace operators
        text = text.replace("multiplied by", "×")
            .replace("multiply by", "×")
            .replace("divided by", "÷")
            .replace("divide by", "÷")
            .replace("over", "÷")
            .replace("times", "×")
            .replace("into", "×")
            .replace("plus", "+")
            .replace("add", "+")
            .replace("minus", "−")
            .replace("negative", "−")
            .replace("subtract", "−")
            .replace("percent of", "% ×")
            .replace("percentage of", "% ×")
            .replace("percent", "%")
            .replace("percentage", "%")
            .replace("point", ".")
            .replace("dot", ".")

        // Tokenize and replace spoken number words (e.g., twenty five -> 25)
        val tokens = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val parsedTokens = mutableListOf<String>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            // Check compound numbers like "twenty five" -> "25"
            if (token in wordToDigit) {
                var currentNum = wordToDigit[token]!!.toLongOrNull() ?: 0L
                var j = i + 1
                while (j < tokens.size && tokens[j] in wordToDigit) {
                    val nextVal = wordToDigit[tokens[j]]!!.toLongOrNull() ?: 0L
                    when {
                        nextVal == 100L || nextVal == 1000L || nextVal == 1000000L -> {
                            currentNum *= nextVal
                        }
                        currentNum in 20..90 && nextVal in 1..9 -> {
                            currentNum += nextVal
                        }
                        else -> {
                            currentNum = (currentNum.toString() + nextVal.toString()).toLongOrNull() ?: currentNum
                        }
                    }
                    j++
                }
                parsedTokens.add(currentNum.toString())
                i = j
            } else {
                parsedTokens.add(token)
                i++
            }
        }

        var reconstructed = parsedTokens.joinToString(" ")
            .replace(" + ", "+")
            .replace(" − ", "−")
            .replace(" - ", "−")
            .replace(" × ", "×")
            .replace(" * ", "×")
            .replace(" ÷ ", "÷")
            .replace(" / ", "÷")
            .replace(" ^ ", "^")
            .replace(". ", ".")
            .replace(" .", ".")
            .replace("( ", "(")
            .replace(" )", ")")

        // Balance unclosed parentheses from functions e.g. sqrt(144 -> sqrt(144)
        val openCount = reconstructed.count { it == '(' }
        val closeCount = reconstructed.count { it == ')' }
        if (openCount > closeCount) {
            reconstructed += ")".repeat(openCount - closeCount)
        }

        return SpokenMathResult(
            expression = reconstructed.trim(),
            shouldEvaluate = shouldEvaluate,
            isClearCommand = false,
            originalSpokenText = rawText
        )
    }
}
