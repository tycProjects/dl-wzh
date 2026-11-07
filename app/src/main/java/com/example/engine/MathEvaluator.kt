package com.example.engine

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.*

class MathEvaluator(
    var isRadianMode: Boolean = false,
    var decimalPrecision: Int = 8
) {

    sealed class EvalResult {
        data class Success(val value: Double, val formatted: String) : EvalResult()
        data class Error(val message: String) : EvalResult()
    }

    fun evaluate(expression: String): EvalResult {
        if (expression.isBlank()) return EvalResult.Success(0.0, "0")
        try {
            val sanitized = sanitize(expression)
            val tokens = tokenize(sanitized)
            if (tokens.isEmpty()) return EvalResult.Success(0.0, "0")
            val parser = ExpressionParser(tokens, isRadianMode)
            val value = parser.parse()
            if (value.isNaN()) return EvalResult.Error("Not a number")
            if (value.isInfinite()) return EvalResult.Error("Value is infinite")
            return EvalResult.Success(value, formatResult(value, decimalPrecision))
        } catch (e: Exception) {
            return EvalResult.Error(e.message ?: "Invalid Expression")
        }
    }

    private fun sanitize(expr: String): String {
        return expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("π", "PI")
            .replace(" ", "")
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val len = expr.length

        while (i < len) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    tokens.add(sb.toString())
                }
                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < len && expr[i].isLetter()) {
                        sb.append(expr[i])
                        i++
                    }
                    val word = sb.toString()
                    tokens.add(word)
                }
                c in "+-*/^%()!" -> {
                    tokens.add(c.toString())
                    i++
                }
                else -> {
                    i++ // skip unrecognized
                }
            }
        }

        // Handle implicit multiplication: e.g. 2(3), )2, 2PI, PI2, 3sin(x), ) (
        val finalTokens = mutableListOf<String>()
        for (idx in tokens.indices) {
            val current = tokens[idx]
            if (idx > 0) {
                val prev = tokens[idx - 1]
                val prevIsNumOrConstOrClose = isNumber(prev) || prev == "PI" || prev == "e" || prev == ")" || prev == "!"
                val currIsNumOrConstOrFuncOrOpen = isNumber(current) || current == "PI" || current == "e" || current == "(" || isFunction(current)

                if (prevIsNumOrConstOrClose && currIsOpenOrFuncOrConst(current)) {
                    finalTokens.add("*")
                } else if ((prev == ")" || prev == "!") && (isNumber(current) || currIsNumberOrConst(current))) {
                    finalTokens.add("*")
                }
            }
            finalTokens.add(current)
        }

        return finalTokens
    }

    private fun isNumber(s: String): Boolean = s.toDoubleOrNull() != null
    private fun currIsNumberOrConst(s: String): Boolean = isNumber(s) || s == "PI" || s == "e"
    private fun currIsOpenOrFuncOrConst(s: String): Boolean = s == "(" || s == "PI" || s == "e" || isFunction(s)
    private fun isFunction(s: String): Boolean = s in setOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "ln", "log", "sqrt", "cbrt", "abs"
    )

    private class ExpressionParser(private val tokens: List<String>, private val isRadianMode: Boolean) {
        private var pos = 0

        fun parse(): Double {
            val result = parseExpression()
            if (pos < tokens.size) {
                throw IllegalArgumentException("Unexpected token: ${tokens[pos]}")
            }
            return result
        }

        // Expression: Term (('+' | '-') Term)*
        private fun parseExpression(): Double {
            var left = parseTerm()
            while (pos < tokens.size) {
                val op = tokens[pos]
                if (op == "+" || op == "-") {
                    pos++
                    val right = parseTerm()
                    left = if (op == "+") left + right else left - right
                } else {
                    break
                }
            }
            return left
        }

        // Term: Factor (('*' | '/' | '%') Factor)*
        private fun parseTerm(): Double {
            var left = parseFactor()
            while (pos < tokens.size) {
                val op = tokens[pos]
                if (op == "*" || op == "/" || op == "%") {
                    pos++
                    val right = parseFactor()
                    left = when (op) {
                        "*" -> left * right
                        "/" -> {
                            if (right == 0.0) throw ArithmeticException("Division by zero")
                            left / right
                        }
                        "%" -> {
                            // If used as percentage (e.g., 50 * 20%) or modulo
                            left % right
                        }
                        else -> left
                    }
                } else {
                    break
                }
            }
            return left
        }

        // Factor: Power ('^' Power)*
        private fun parseFactor(): Double {
            var base = parseUnary()
            if (pos < tokens.size && tokens[pos] == "^") {
                pos++
                val exp = parseFactor() // right-associative
                base = base.pow(exp)
            }
            return base
        }

        // Unary: ('+' | '-') Unary | Postfix
        private fun parseUnary(): Double {
            if (pos < tokens.size) {
                val token = tokens[pos]
                if (token == "+") {
                    pos++
                    return parseUnary()
                } else if (token == "-") {
                    pos++
                    return -parseUnary()
                }
            }
            return parsePostfix()
        }

        // Postfix: Primary ('!' | '%')*
        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (pos < tokens.size) {
                val token = tokens[pos]
                if (token == "!") {
                    pos++
                    value = factorial(value)
                } else if (token == "%" && (pos + 1 >= tokens.size || tokens[pos + 1] in setOf("+", "-", "*", "/", ")"))) {
                    // Trailing percentage: 50% = 0.5
                    pos++
                    value /= 100.0
                } else {
                    break
                }
            }
            return value
        }

        // Primary: Number | Constant | '(' Expression ')' | Function '(' Expression ')'
        private fun parsePrimary(): Double {
            if (pos >= tokens.size) throw IllegalArgumentException("Unexpected end of expression")
            val token = tokens[pos]

            when {
                token == "(" -> {
                    pos++
                    val inside = parseExpression()
                    if (pos < tokens.size && tokens[pos] == ")") {
                        pos++
                    } else {
                        // Tolerate unclosed paren at end of input
                    }
                    return inside
                }
                token == "PI" -> {
                    pos++
                    return Math.PI
                }
                token == "e" -> {
                    pos++
                    return Math.E
                }
                token.toDoubleOrNull() != null -> {
                    pos++
                    return token.toDouble()
                }
                isFunction(token) -> {
                    pos++
                    val arg: Double
                    if (pos < tokens.size && tokens[pos] == "(") {
                        pos++
                        arg = parseExpression()
                        if (pos < tokens.size && tokens[pos] == ")") {
                            pos++
                        }
                    } else {
                        arg = parsePrimary()
                    }
                    return evaluateFunction(token, arg, isRadianMode)
                }
                else -> throw IllegalArgumentException("Unknown symbol: $token")
            }
        }

        private fun isFunction(s: String): Boolean = s in setOf(
            "sin", "cos", "tan", "asin", "acos", "atan",
            "ln", "log", "sqrt", "cbrt", "abs"
        )

        private fun evaluateFunction(name: String, arg: Double, rad: Boolean): Double {
            return when (name) {
                "sin" -> {
                    val angle = if (rad) arg else Math.toRadians(arg)
                    // Clean precision near 0 (e.g. sin(180) -> 0)
                    val v = sin(angle)
                    if (abs(v) < 1e-12) 0.0 else v
                }
                "cos" -> {
                    val angle = if (rad) arg else Math.toRadians(arg)
                    val v = cos(angle)
                    if (abs(v) < 1e-12) 0.0 else v
                }
                "tan" -> {
                    val angle = if (rad) arg else Math.toRadians(arg)
                    val c = cos(angle)
                    if (abs(c) < 1e-12) throw ArithmeticException("Undefined (tangent singularity)")
                    val v = tan(angle)
                    if (abs(v) < 1e-12) 0.0 else v
                }
                "asin" -> {
                    if (arg < -1.0 || arg > 1.0) throw ArithmeticException("Domain error for asin")
                    val radVal = asin(arg)
                    if (rad) radVal else Math.toDegrees(radVal)
                }
                "acos" -> {
                    if (arg < -1.0 || arg > 1.0) throw ArithmeticException("Domain error for acos")
                    val radVal = acos(arg)
                    if (rad) radVal else Math.toDegrees(radVal)
                }
                "atan" -> {
                    val radVal = atan(arg)
                    if (rad) radVal else Math.toDegrees(radVal)
                }
                "ln" -> {
                    if (arg <= 0) throw ArithmeticException("Domain error: ln(x) for x <= 0")
                    ln(arg)
                }
                "log" -> {
                    if (arg <= 0) throw ArithmeticException("Domain error: log(x) for x <= 0")
                    log10(arg)
                }
                "sqrt" -> {
                    if (arg < 0) throw ArithmeticException("Square root of negative number")
                    sqrt(arg)
                }
                "cbrt" -> cbrt(arg)
                "abs" -> abs(arg)
                else -> arg
            }
        }

        private fun factorial(n: Double): Double {
            if (n < 0 || n != floor(n)) throw ArithmeticException("Factorial only defined for non-negative integers")
            if (n > 170) return Double.POSITIVE_INFINITY
            var res = 1.0
            val limit = n.toInt()
            for (i in 2..limit) {
                res *= i
            }
            return res
        }
    }

    companion object {
        fun formatResult(value: Double, precision: Int = 8): String {
            if (value.isNaN()) return "NaN"
            if (value.isInfinite()) return if (value > 0) "∞" else "-∞"

            // Handle very large or very small numbers using scientific notation
            val absVal = abs(value)
            if (absVal != 0.0 && (absVal >= 1e12 || absVal < 1e-6)) {
                val scFormat = DecimalFormat("0.######E0", DecimalFormatSymbols(Locale.US))
                return scFormat.format(value)
            }

            val bd = BigDecimal(value.toString())
                .setScale(precision, RoundingMode.HALF_UP)
                .stripTrailingZeros()
            val plain = bd.toPlainString()

            // Add grouping commas to integer part
            val parts = plain.split(".")
            val intPart = parts[0]
            val formattedInt = try {
                val isNeg = intPart.startsWith("-")
                val cleanInt = if (isNeg) intPart.substring(1) else intPart
                val grouped = cleanInt.reversed().chunked(3).joinToString(",").reversed()
                if (isNeg) "-$grouped" else grouped
            } catch (e: Exception) {
                intPart
            }

            return if (parts.size > 1) {
                "$formattedInt.${parts[1]}"
            } else {
                formattedInt
            }
        }
    }
}
