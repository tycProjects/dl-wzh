package com.example.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.DarkModePreference
import com.example.ui.components.CalcButton
import com.example.ui.components.CalcDisplay
import com.example.ui.components.KeyType
import com.example.viewmodel.CalculatorUiState

@Composable
fun BasicCalcScreen(
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Display Area
        CalcDisplay(
            expression = state.expression,
            liveResult = state.liveResult,
            hasMemory = state.hasMemory,
            isRadianMode = state.isRadianMode,
            isScientific = false,
            quickVatRate = state.quickVatRate,
            palette = state.themePalette,
            isDark = isDark,
            voiceMessage = state.voiceMessage,
            onVoiceClick = onVoiceClick,
            onClearVoiceMessage = onClearVoiceMessage,
            onHistoryClick = onOpenHistory,
            onBackspaceClick = { onKeyClick("⌫") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Keypad Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Memory & Quick Financial VAT Tax buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.85f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton("MC", KeyType.MEMORY, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("MC") }
                CalcButton("MR", KeyType.MEMORY, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("MR") }
                CalcButton("M+", KeyType.MEMORY, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("M+") }
                CalcButton("TAX+", KeyType.TAX, Modifier.weight(1.1f), state.themePalette, isDark) { onKeyClick("TAX+") }
                CalcButton("TAX-", KeyType.TAX, Modifier.weight(1.1f), state.themePalette, isDark) { onKeyClick("TAX-") }
            }

            // Row 2: AC, C, %, ÷
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton("AC", KeyType.ACTION, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("AC") }
                CalcButton("C", KeyType.ACTION, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("C") }
                CalcButton("%", KeyType.OPERATOR, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("%") }
                CalcButton("÷", KeyType.OPERATOR, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("÷") }
            }

            // Row 3: 7, 8, 9, ×
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton("7", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("7") }
                CalcButton("8", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("8") }
                CalcButton("9", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("9") }
                CalcButton("×", KeyType.OPERATOR, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("×") }
            }

            // Row 4: 4, 5, 6, −
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton("4", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("4") }
                CalcButton("5", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("5") }
                CalcButton("6", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("6") }
                CalcButton("−", KeyType.OPERATOR, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("−") }
            }

            // Row 5: 1, 2, 3, +
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton("1", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("1") }
                CalcButton("2", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("2") }
                CalcButton("3", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("3") }
                CalcButton("+", KeyType.OPERATOR, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("+") }
            }

            // Row 6: ±, 0, ., =
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton("±", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("±") }
                CalcButton("0", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("0") }
                CalcButton(".", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick(".") }
                CalcButton("=", KeyType.EQUALS, Modifier.weight(1f), state.themePalette, isDark) { onKeyClick("=") }
            }
        }
    }
}
