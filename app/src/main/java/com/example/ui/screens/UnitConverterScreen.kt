package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.CalcButton
import com.example.ui.components.KeyType
import com.example.viewmodel.CalculatorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(
    state: CalculatorUiState,
    onSetDimension: (UnitDimension) -> Unit,
    onSetFromUnit: (ConversionUnit) -> Unit,
    onSetToUnit: (ConversionUnit) -> Unit,
    onSetInputValue: (String) -> Unit,
    onSwapUnits: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = when (state.darkModePref) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
    }

    val availableUnits = remember(state.converterDimension) {
        UnitRegistry.getUnitsForDimension(state.converterDimension)
    }

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Category / Dimension scrollable row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UnitRegistry.dimensions.forEach { dim ->
                    FilterChip(
                        selected = state.converterDimension == dim,
                        onClick = { onSetDimension(dim) },
                        label = { Text(dim.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = state.themePalette.primary,
                            selectedLabelColor = if (isDark) Color.Black else Color.White
                        ),
                        modifier = Modifier.testTag("dim_${dim.name}")
                    )
                }
            }

            // From Unit Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) state.themePalette.darkSurface else state.themePalette.lightSurface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FROM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box {
                            OutlinedButton(
                                onClick = { fromExpanded = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_from_unit_dropdown")
                            ) {
                                Text("${state.converterFromUnit.name} (${state.converterFromUnit.symbol})")
                            }

                            DropdownMenu(
                                expanded = fromExpanded,
                                onDismissRequest = { fromExpanded = false }
                            ) {
                                availableUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text("${unit.name} (${unit.symbol})") },
                                        onClick = {
                                            onSetFromUnit(unit)
                                            fromExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (state.converterInputValue.isEmpty()) "0" else state.converterInputValue,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("converter_input_text")
                    )
                }
            }

            // Swap Button Row
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSwapUnits,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(state.themePalette.primary.copy(alpha = 0.15f))
                        .testTag("btn_swap_units")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap Units",
                        tint = state.themePalette.primary
                    )
                }
            }

            // To Unit Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) state.themePalette.darkSurfaceVariant else state.themePalette.lightSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Converted Value", state.converterOutputValue))
                                    Toast.makeText(context, "Copied result", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = state.themePalette.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Box {
                                OutlinedButton(
                                    onClick = { toExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_to_unit_dropdown")
                                ) {
                                    Text("${state.converterToUnit.name} (${state.converterToUnit.symbol})")
                                }

                                DropdownMenu(
                                    expanded = toExpanded,
                                    onDismissRequest = { toExpanded = false }
                                ) {
                                    availableUnits.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text("${unit.name} (${unit.symbol})") },
                                            onClick = {
                                                onSetToUnit(unit)
                                                toExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.converterOutputValue,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = state.themePalette.primary,
                        modifier = Modifier.testTag("converter_output_text")
                    )
                }
            }
        }

        // On-screen keypad for fast unit typing
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val onKey = { key: String ->
                when (key) {
                    "C" -> onSetInputValue("")
                    "⌫" -> {
                        if (state.converterInputValue.isNotEmpty()) {
                            onSetInputValue(state.converterInputValue.dropLast(1))
                        }
                    }
                    "." -> {
                        if (!state.converterInputValue.contains(".")) {
                            onSetInputValue(if (state.converterInputValue.isEmpty()) "0." else state.converterInputValue + ".")
                        }
                    }
                    "±" -> {
                        if (state.converterInputValue.startsWith("-")) {
                            onSetInputValue(state.converterInputValue.substring(1))
                        } else if (state.converterInputValue.isNotEmpty()) {
                            onSetInputValue("-" + state.converterInputValue)
                        }
                    }
                    else -> onSetInputValue(state.converterInputValue + key)
                }
            }

            // Keypad rows
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalcButton("7", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey("7") }
                CalcButton("8", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey("8") }
                CalcButton("9", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey("9") }
                CalcButton("C", KeyType.ACTION, Modifier.weight(1f), state.themePalette, isDark) { onKey("C") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalcButton("4", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey("4") }
                CalcButton("5", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey("5") }
                CalcButton("6", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey("6") }
                CalcButton("⌫", KeyType.ACTION, Modifier.weight(1f), state.themePalette, isDark) { onKey("⌫") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalcButton("1", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey("1") }
                CalcButton("2", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey("2") }
                CalcButton("3", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey("3") }
                CalcButton("±", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey("±") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalcButton("0", KeyType.NUMBER, Modifier.weight(2f), state.themePalette, isDark) { onKey("0") }
                CalcButton(".", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey(".") }
                CalcButton("00", KeyType.NUMBER, Modifier.weight(1f), state.themePalette, isDark) { onKey("00") }
            }
        }
    }
}
