package com.example.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.DarkModePreference
import com.example.ui.components.CalcButton
import com.example.ui.components.CalcDisplay
import com.example.ui.components.KeyType
import com.example.viewmodel.CalculatorUiState

@Composable
fun ScientificCalcScreen(
    state: CalculatorUiState,
    onKeyClick: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onVoiceClick: () -> Unit = {},
    onClearVoiceMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = when (state.darkModePref) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
    }

    var isSecondPage by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Display Area
        CalcDisplay(
            expression = state.expression,
            liveResult = state.liveResult,
            hasMemory = state.hasMemory,
            isRadianMode = state.isRadianMode,
            isScientific = true,
            quickVatRate = state.quickVatRate,
            palette = state.themePalette,
            isDark = isDark,
            voiceMessage = state.voiceMessage,
            onVoiceClick = onVoiceClick,
            onClearVoiceMessage = onClearVoiceMessage,
            onHistoryClick = onOpenHistory,
            onBackspaceClick = { onKeyClick("⌫") }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 5-Column Grid with 2nd Mode toggle for maximum scientific power
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: 2nd, DEG/RAD, Trig/Inverse Trig, π
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton(
                    text = if (isSecondPage) "1st" else "2nd",
                    type = KeyType.TAX,
                    modifier = Modifier.weight(1f),
                    palette = state.themePalette,
                    isDark = isDark
                ) {
                    isSecondPage = !isSecondPage
                }

                CalcButton(
                    text = if (state.isRadianMode) "RAD" else "DEG",
                    type = KeyType.SCIENTIFIC,
                    modifier = Modifier.weight(1f),
                    palette = state.themePalette,
                    isDark = isDark
                ) {
                    onKeyClick(if (state.isRadianMode) "DEG" else "RAD")
                }

                CalcButton(
                    text = if (isSecondPage) "asin" else "sin",
                    type = KeyType.SCIENTIFIC,
                    modifier = Modifier.weight(1f),
                    palette = state.themePalette,
                    isDark = isDark
                ) {
                    onKeyClick(if (isSecondPage) "asin(" else "sin(")
                }

                CalcButton(
                    text = if (isSecondPage) "acos" else "cos",
                    type = KeyType.SCIENTIFIC,
                    modifier = Modifier.weight(1f),
                    palette = state.themePalette,
                    isDark = isDark
                ) {
                    onKeyClick(if (isSecondPage) "acos(" else "cos(")
                }

                CalcButton(
                    text = if (isSecondPage) "atan" else "tan",
                    type = KeyType.SCIENTIFIC,
                    modifier = Modifier.weight(1f),
                    palette = state.themePalette,
                    isDark = isDark
                ) {
                    onKeyClick(if (isSecondPage) "atan(" else "tan(")
                }
            }

            // Row 2: ln/e^x, log/10^x, sqrt/cbrt, power ^, constants
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton(
                    text = if (isSecondPage) "eˣ" else "ln",
                    type = KeyType.SCIENTIFIC,
                    modifier = Modifier.weight(1f),
                    palette = state.themePalette,
                    isDark = isDark
                ) {
                    onKeyClick(if (isSecondPage) "e^(" else "ln(")
                }

                CalcButton(
                    text = if (isSecondPage) "10ˣ" else "log",
                    type = KeyType.SCIENTIFIC,
                    modifier = Modifier.weight(1f),
                    palette = state.themePalette,
                    isDark = isDark
                ) {
                    onKeyClick(if (isSecondPage) "10^(" else "log(")
                }

                CalcButton(
                    text = if (isSecondPage) "³√" else "√",
                    type = KeyType.SCIENTIFIC,
                    modifier = Modifier.weight(1f),
                    palette = state.themePalette,
                    isDark = isDark
                ) {
                    onKeyClick(if (isSecondPage) "cbrt(" else "sqrt(")
                }

                CalcButton("^", KeyType.SCIENTIFIC, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("^") }
                CalcButton(if (isSecondPage) "e" else "π", KeyType.SCIENTIFIC, Modifier.weight(1f), state.themePalette, isDark) {
                    onKeyClick(if (isSecondPage) "e" else "π")
                }
            }

            // Row 3: AC, (, ), %, ÷
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton("AC", KeyType.ACTION, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("AC") }
                CalcButton("(", KeyType.SCIENTIFIC, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("(") }
                CalcButton(")", KeyType.SCIENTIFIC, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick(")") }
                CalcButton("%", KeyType.OPERATOR, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("%") }
                CalcButton("÷", KeyType.OPERATOR, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("÷") }
            }

            // Row 4: 7, 8, 9, ×, ⌫
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton("7", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("7") }
                CalcButton("8", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("8") }
                CalcButton("9", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("9") }
                CalcButton("×", KeyType.OPERATOR, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("×") }
                CalcButton("⌫", KeyType.ACTION, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("⌫") }
            }

            // Row 5: 4, 5, 6, −, x²
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton("4", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("4") }
                CalcButton("5", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("5") }
                CalcButton("6", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("6") }
                CalcButton("−", KeyType.OPERATOR, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("−") }
                CalcButton("x²", KeyType.SCIENTIFIC, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("^2") }
            }

            // Row 6: 1, 2, 3, +, x!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton("1", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("1") }
                CalcButton("2", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("2") }
                CalcButton("3", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("3") }
                CalcButton("+", KeyType.OPERATOR, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("+") }
                CalcButton("x!", KeyType.SCIENTIFIC, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("!") }
            }

            // Row 7: ±, 0, ., 1/x, =
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton("±", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("±") }
                CalcButton("0", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("0") }
                CalcButton(".", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick(".") }
                CalcButton("1/x", KeyType.SCIENTIFIC, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("^(-1)") }
                CalcButton("=", KeyType.EQUALS, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("=") }
            }
        }
    }
}
