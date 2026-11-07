package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MathEvaluator
import com.example.model.DarkModePreference
import com.example.model.VatCalculationMode
import com.example.model.VatPreset
import com.example.viewmodel.CalculatorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VatCalcScreen(
    state: CalculatorUiState,
    onSetVatMode: (VatCalculationMode) -> Unit,
    onSetNetAmount: (String) -> Unit,
    onSetGrossAmount: (String) -> Unit,
    onSetVatRate: (String) -> Unit,
    onSelectPreset: (VatPreset) -> Unit,
    onAddCustomPreset: (Double, String) -> Unit,
    onSendToCalc: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = when (state.darkModePref) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
    }

    var showAddCustomDialog by remember { mutableStateOf(false) }
    var customRateText by remember { mutableStateOf("") }
    var customLabelText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector (Segmented buttons)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            VatCalculationMode.values().forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.vatMode == mode,
                    onClick = { onSetVatMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = VatCalculationMode.values().size),
                    modifier = Modifier.testTag("vat_mode_$index")
                ) {
                    Text(
                        text = when (mode) {
                            VatCalculationMode.NET_TO_GROSS -> "Add VAT"
                            VatCalculationMode.GROSS_TO_NET -> "Extract VAT"
                            VatCalculationMode.CUSTOM_ANALYSIS -> "Find Rate"
                        },
                        fontSize = 12.sp,
                        fontWeight = if (state.vatMode == mode) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // VAT Preset Rates Row
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tax / VAT Presets",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { showAddCustomDialog = true },
                    modifier = Modifier.testTag("btn_add_custom_vat")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add custom rate", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom", fontSize = 13.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.vatPresets.forEach { preset ->
                    val isSelected = state.vatRateInput.toDoubleOrNull() == preset.rate
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = state.themePalette.primary,
                            selectedLabelColor = if (isDark) Color.Black else Color.White
                        ),
                        modifier = Modifier.testTag("preset_${preset.label}")
                    )
                }
            }
        }

        // Input Fields Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) state.themePalette.darkSurface else state.themePalette.lightSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (state.vatMode) {
                    VatCalculationMode.NET_TO_GROSS -> {
                        OutlinedTextField(
                            value = state.vatAmountInput,
                            onValueChange = onSetNetAmount,
                            label = { Text("Net Price (Excl. VAT)") },
                            prefix = { Text("\$ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_net_amount")
                        )

                        OutlinedTextField(
                            value = state.vatRateInput,
                            onValueChange = onSetVatRate,
                            label = { Text("VAT Rate (%)") },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_vat_rate")
                        )
                    }

                    VatCalculationMode.GROSS_TO_NET -> {
                        OutlinedTextField(
                            value = state.vatGrossInput,
                            onValueChange = onSetGrossAmount,
                            label = { Text("Gross Price (Incl. VAT)") },
                            prefix = { Text("\$ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_gross_amount")
                        )

                        OutlinedTextField(
                            value = state.vatRateInput,
                            onValueChange = onSetVatRate,
                            label = { Text("VAT Rate (%)") },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_vat_rate_gross")
                        )
                    }

                    VatCalculationMode.CUSTOM_ANALYSIS -> {
                        OutlinedTextField(
                            value = state.vatAmountInput,
                            onValueChange = onSetNetAmount,
                            label = { Text("Net Price (Excl. VAT)") },
                            prefix = { Text("\$ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_net_amount_analysis")
                        )

                        OutlinedTextField(
                            value = state.vatGrossInput,
                            onValueChange = onSetGrossAmount,
                            label = { Text("Gross Price (Incl. VAT)") },
                            prefix = { Text("\$ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_gross_amount_analysis")
                        )
                    }
                }
            }
        }

        // Visual Proportion Bar (Net vs VAT)
        val res = state.vatResult
        val totalGross = if (res.grossAmount > 0) res.grossAmount else 1.0
        val netRatio = (res.netAmount / totalGross).coerceIn(0.0, 1.0).toFloat()
        val vatRatio = (res.vatAmount / totalGross).coerceIn(0.0, 1.0).toFloat()

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Net: ${(netRatio * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = state.themePalette.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "VAT: ${(vatRatio * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = state.themePalette.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (netRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(netRatio.coerceAtLeast(0.01f))
                            .background(state.themePalette.primary)
                    )
                }
                if (vatRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(vatRatio.coerceAtLeast(0.01f))
                            .background(state.themePalette.accent)
                    )
                }
            }
        }

        // Result Summary Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) state.themePalette.darkSurfaceVariant else state.themePalette.lightSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Calculation Breakdown",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                VatResultRow(
                    label = "Net Price (Excl. Tax)",
                    value = "\$ " + MathEvaluator.formatResult(res.netAmount, 2),
                    color = MaterialTheme.colorScheme.onSurface,
                    onCopy = { copyToClipboard(context, res.netAmount.toString(), "Net Price") }
                )

                VatResultRow(
                    label = "Effective VAT Rate",
                    value = "${MathEvaluator.formatResult(res.vatRate, 2)}%",
                    color = state.themePalette.primary,
                    onCopy = { copyToClipboard(context, res.vatRate.toString(), "VAT Rate") }
                )

                VatResultRow(
                    label = "VAT Amount",
                    value = "\$ " + MathEvaluator.formatResult(res.vatAmount, 2),
                    color = state.themePalette.accent,
                    onCopy = { copyToClipboard(context, res.vatAmount.toString(), "VAT Amount") }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gross Total (Incl. Tax)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\$ " + MathEvaluator.formatResult(res.grossAmount, 2),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { copyToClipboard(context, res.grossAmount.toString(), "Gross Total") },
                            modifier = Modifier.testTag("btn_copy_gross")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Gross", tint = state.themePalette.primary)
                        }

                        FilledTonalButton(
                            onClick = { onSendToCalc(res.grossAmount) },
                            modifier = Modifier.testTag("btn_send_vat_to_calc")
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use in Calc", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Custom Preset Dialog
    if (showAddCustomDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = { Text("Add Custom VAT Rate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customRateText,
                        onValueChange = { customRateText = it },
                        label = { Text("Rate Percentage (e.g. 7.5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customLabelText,
                        onValueChange = { customLabelText = it },
                        label = { Text("Label (e.g. 7.5% State Tax)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rate = customRateText.toDoubleOrNull()
                        if (rate != null && rate > 0) {
                            val label = if (customLabelText.isNotBlank()) customLabelText else "$rate%"
                            onAddCustomPreset(rate, label)
                            showAddCustomDialog = false
                            customRateText = ""
                            customLabelText = ""
                        }
                    }
                ) {
                    Text("Save Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VatResultRow(
    label: String,
    value: String,
    color: Color,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun copyToClipboard(context: Context, value: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}
